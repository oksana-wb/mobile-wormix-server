package com.pragmatix.app.services;

import com.google.gson.Gson;
import com.pragmatix.app.common.*;
import com.pragmatix.app.dao.PaymentStatisticDao;
import com.pragmatix.app.dao.UserProfileDao;
import com.pragmatix.app.domain.DepositEntity;
import com.pragmatix.app.domain.PaymentStatisticEntity;
import com.pragmatix.app.messages.server.BuyVipResult;
import com.pragmatix.app.messages.server.IncomingPayment;
import com.pragmatix.app.messages.server.OpenDepositResult;
import com.pragmatix.app.messages.structures.BundleStructure;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.DepositBean;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.clanserver.messages.ServiceResult;
import com.pragmatix.clanserver.services.ClanServiceImpl;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.common.utils.VarObject;
import com.pragmatix.gameapp.GameApp;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.gameapp.social.service.vkontakte.UserSubscription;
import com.pragmatix.gameapp.social.service.vkontakte.VkPaymentRecord;
import com.pragmatix.gameapp.threads.Execution;
import com.pragmatix.gameapp.threads.ExecutionContext;
import com.pragmatix.server.Server;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 26.10.12 16:05
 */
@Component
public class PaymentService {

    private final Logger log = SocialService.PAYMENT_LOGGER;

    @Resource
    private PaymentStatisticDao paymentStatisticDao;

    @Resource
    private UserProfileDao userProfileDao;

    @Resource
    private ProfileService profileService;

    @Resource
    private GameApp gameApp;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private WipeProfileService wipeProfileService;

    // те кто совершил хоть один платеж за последний год
    private Set<Integer> donaters = new HashSet<>();

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private ClanServiceImpl clanService;

    @Resource
    private BundleService bundleService;

    @Resource
    private Optional<DepositService> depositService;

    @Resource
    private SoftCache softCache;

    @Resource
    private StuffService stuffService;

    @Value("${debug.paymentResultSuccess:false}")
    private boolean debugMode = false;

    private boolean initialized = false;

    // частота показа спецпредложений для игроков _низких_ уровней (< 30)
    private String lowSpecialDealPeriod = "2 MONTHS";
    // частота показа спецпредложений для игроков _высокого_ уровня (= 30)
    private String highSpecialDealPeriod = "1 MONTH";

    public void init() {
        fillDonaters();
        initialized = true;
    }

    public void fillDonaters() {
        final Set<Integer> donaters = new HashSet<>();
        Server.sysLog.info("fill donaters ...");
        String sql = "WITH p AS (" +
                "  SELECT profile_id, max(date) AS date" +
                "  FROM payment_statistic_parent" +
                "  WHERE payment_status = 0 AND date > now() - ?::INTERVAL GROUP BY 1" +
                " ) " +
                "SELECT p.profile_id FROM wormswar.user_profile AS u " +
                "INNER JOIN p ON u.id = p.profile_id " +
                "WHERE (u.level < 30 OR p.date > now() - ?::INTERVAL)";
        jdbcTemplate.query(sql, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet res) throws SQLException {
                donaters.add(res.getInt("profile_id"));
            }
        }, lowSpecialDealPeriod, highSpecialDealPeriod);
        this.donaters = donaters;
        Server.sysLog.info("done. donaters: {}", donaters.size());
    }

    public void onSuccessPayment(UserProfile profile) {
        dailyRegistry.makePayment(profile.getId());
        profile.setLastPaymentTime(System.currentTimeMillis());
    }

    @Null
    public OrderBean getOrderBean(Map<String, List<Integer>> priceByItem, String item) {
        BundleStructure validBundle = bundleService.getValidBundle(item);
        if(validBundle != null) {
            return new OrderBean(PaymentType.BUNDLE, 1, validBundle.votes, validBundle.period, validBundle.trialDuration);
        }
        List<Integer> itemBean = priceByItem.get(item);
        if(itemBean != null) {
            PaymentType paymentMoneyType = PaymentType.valueOf(itemBean.get(0));
            Integer paymentAmount = itemBean.get(1);
            Integer paymentCost = itemBean.get(2);
            if(paymentMoneyType == PaymentType.CLAN_DONATE) {
                Integer paymentAmountComeback = itemBean.get(3);
                return new OrderBean(paymentMoneyType, paymentAmount, paymentAmountComeback, paymentCost);
            } else if(paymentMoneyType != null) {
                return new OrderBean(paymentMoneyType, paymentAmount, paymentCost);
            }
        }
        return null;
    }

    public List<OrderBean> getClanDonateOrderBeans(Map<String, List<Integer>> priceByItem) {
        List<OrderBean> result = new ArrayList<>();
        for(List<Integer> itemBean : priceByItem.values()) {
            PaymentType paymentMoneyType = PaymentType.valueOf(itemBean.get(0));
            if(paymentMoneyType == PaymentType.CLAN_DONATE) {
                Integer paymentAmount = itemBean.get(1);
                Integer paymentCost = itemBean.get(2);
                Integer paymentAmountComeback = itemBean.get(3);
                result.add(new OrderBean(paymentMoneyType, paymentAmount, paymentAmountComeback, paymentCost));
            }
        }
        return result;
    }

    /**
     * Провести платеж
     *
     * @param profile         профиль
     * @param paymentType     реалы или фузы
     * @param paymentAmount   количество валюты
     * @param paymentCost     стоимость в ед. соц сети
     * @param transactionId   transactionId
     * @param transactionDate transactionDate
     * @param item            строковый id платежа
     * @return id платежа в таблице payment_statistic или -1 в случае ошибоки базы данных
     */
    public int applyPayment(final UserProfile profile, final PaymentType paymentType, final int paymentAmount, final int paymentCost, final String transactionId, final Date transactionDate,
                            final String item, boolean sendIncomingPayment) {
        final UserProfileDao profileDao = userProfileDao;
        int newPaymentId;
        try {
            final VarObject<Integer> newRealMoney = new VarObject<>(profile.getRealMoney());
            final VarObject<Integer> newMoney = new VarObject<>(profile.getMoney());

            newPaymentId = transactionTemplate.execute(transactionStatus -> {
                int balance = 0;
                if(paymentType == PaymentType.REAL_MONEY) {
                    //сохроняем в базе новое значение
                    newRealMoney.value = profile.getRealMoney() + paymentAmount;
                    balance = newRealMoney.value;
                } else if(paymentType == PaymentType.MONEY) {
                    //сохроняем в профайле новое значение
                    newMoney.value = profile.getMoney() + paymentAmount;
                    balance = newMoney.value;
                }
                final PaymentStatisticEntity entity = new PaymentStatisticEntity();
                entity.setCompleted(true);
                entity.setDate(transactionDate);
                entity.setMoneyType(paymentType.type);
                entity.setPaymentStatus((short) 0);
                entity.setProfileId(profile.getId());
                entity.setTransactionId(transactionId);
                entity.setUpdateDate(new Date());
                entity.setVotes(paymentCost);
                entity.setAmount(paymentAmount);
                entity.setBalanse(balance);
                entity.setItem(item);

                PaymentStatisticEntity statisticEntity = paymentStatisticDao.insert(entity);

                return statisticEntity.getId();
            });

            onSuccessPayment(profile);

            int realMoney = 0;
            int money = 0;
            if(paymentType == PaymentType.REAL_MONEY) {
                //сохроняем в профайле новое значение
                profile.setRealMoney(newRealMoney.value);
                realMoney = paymentAmount;
            } else if(paymentType == PaymentType.MONEY) {
                //сохроняем в профайле новое значение
                profile.setMoney(newMoney.value);
                money = paymentAmount;
            }

            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PAYMENT, profile,
                    Param.eventType, paymentType,
                    "paymentCost", paymentCost,
                    "paymentItem", item,
                    Param.realMoney, realMoney,
                    Param.money, money,
                    "remoteAddress", getConnectionRemoteAddress(profile)
            );

            profileService.updateSync(profile);
        } catch (TransactionException e) {
            log.error(e.toString(), e);
            return -1;
        }

        if(sendIncomingPayment)
            sendIncomingPayment(profile, paymentType, item, paymentAmount);

        return newPaymentId;
    }

    public void rename(final UserProfile profile, final int paymentAmount, final int paymentCost, final String transactionId, final Date transactionDate, final String item) {
        final PaymentType paymentType = PaymentType.RENAME;
        try {
            final VarObject<Byte> newRenameAct = new VarObject<>(profile.getRenameAct());

            transactionTemplate.execute(transactionStatus -> {
                newRenameAct.value = (byte) (profile.getRenameAct() + paymentAmount);

                final PaymentStatisticEntity entity = new PaymentStatisticEntity();
                entity.setCompleted(true);
                entity.setDate(transactionDate);
                entity.setMoneyType(paymentType.type);
                entity.setPaymentStatus((short) 0);
                entity.setProfileId(profile.getId());
                entity.setTransactionId(transactionId);
                entity.setUpdateDate(new Date());
                entity.setVotes(paymentCost);
                entity.setAmount(paymentAmount);
                entity.setBalanse(newRenameAct.value);
                entity.setItem(item);

                PaymentStatisticEntity statisticEntity = paymentStatisticDao.insert(entity);

                return statisticEntity.getId();
            });

            profile.setRenameAct(newRenameAct.value);

            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PAYMENT, profile,
                    Param.eventType, paymentType,
                    "paymentCost", paymentCost,
                    "paymentItem", item,
                    "renameAct", profile.getRenameAct(),
                    "remoteAddress", getConnectionRemoteAddress(profile)
            );

            profileService.updateSync(profile);
        } catch (TransactionException e) {
            log.error(e.toString(), e);
        }
    }

    public void sendIncomingPayment(UserProfile profile, PaymentType paymentMoneyType, String item, int paymentAmount) {
        sendIncomingPayment(profile, paymentMoneyType, item, paymentAmount, Collections.emptyList(), "");
    }

    public void sendIncomingPayment(UserProfile profile, PaymentType paymentMoneyType, String item, int paymentAmount, List<GenericAwardStructure> awards, String note) {
        if(Execution.EXECUTION.get() == null) {
            Execution.EXECUTION.set(new ExecutionContext(gameApp));
        }
        Session session = Sessions.get(profile);
        if(session != null) {
            //Сообщаем игроку о поступившем платеже
            IncomingPayment message = new IncomingPayment(paymentMoneyType, item, paymentAmount, awards, note, session.getKey());
            boolean sendMessageResult = Messages.toUser(message, profile, Connection.MAIN);
            log.info(String.format("%s >> [%s], результат %s", message, profile, sendMessageResult));
        } else {
            log.info("получатель платежа [{}] вышел из игры", profile);
        }
    }

    public void sendMessage(UserProfile profile, Object message) {
        if(Execution.EXECUTION.get() == null) {
            Execution.EXECUTION.set(new ExecutionContext(gameApp));
        }
        Session session = Sessions.get(profile);
        if(session != null) {
            boolean sendMessageResult = Messages.toUser(message, profile, Connection.MAIN);
            log.info(String.format("%s >> [%s], результат %s", message, profile, sendMessageResult));
        }
    }

    public int donateToClan(final UserProfile profile, final int paymentAmount, final int paymentAmountComeback, final int paymentCost, final String transactionId, final Date transactionDate, final String item) {
        int newPaymentId = -1;

        if(Execution.EXECUTION.get() == null) {
            Execution.EXECUTION.set(new ExecutionContext(gameApp));
        }

        try {
            ServiceResult donateResult = clanService.donate(profile.getId().intValue(), profile.getSocialId(), paymentAmount, paymentAmountComeback);
            if(!donateResult.isOk())
                return -2;

            newPaymentId = transactionTemplate.execute(transactionStatus -> {

                final PaymentStatisticEntity entity = new PaymentStatisticEntity();
                entity.setCompleted(true);
                entity.setDate(transactionDate);
                entity.setMoneyType(PaymentType.CLAN_DONATE.type);
                entity.setPaymentStatus((short) 0);
                entity.setProfileId(profile.getId());
                entity.setTransactionId(transactionId);
                entity.setUpdateDate(new Date());
                entity.setVotes(paymentCost);
                entity.setAmount(paymentAmount);
                entity.setBalanse(profile.getRealMoney());
                entity.setItem(item + "; clanId=" + profile.getClanId());

                PaymentStatisticEntity statisticEntity = paymentStatisticDao.insert(entity);

                return statisticEntity.getId();
            });

            onSuccessPayment(profile);
        } catch (TransactionException e) {
            log.error(e.toString(), e);
            return -1;
        }

        profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PAYMENT, profile,
                Param.eventType, PaymentType.CLAN_DONATE,
                "paymentCost", paymentCost,
                "paymentItem", item,
                "clanId", profile.getClanId(),
                "clanRank", profile.getRankInClan(),
                "donateAmount", paymentAmount,
                "donateAmountComeback", paymentAmountComeback,
                "remoteAddress", getConnectionRemoteAddress(profile)
        );

        log.info("[{}] пополнение казны клана: в казну [{}], вернется в случае исключения из клана [{}]", profile, paymentAmount, paymentAmountComeback);

        profileService.updateSync(profile);

        return newPaymentId;
    }

    public Runnable cancelVipSubscription(final UserProfile profile, final String item, String eventSource, PaymentStatisticEntity entity, VkPaymentRecord paymentRecord) {
        final BundleStructure bundle = bundleService.getBundle(item);
        if(bundle == null) {
            log.error("[{}] информация о пакете предметов не найдена! code={}", profile, item);
            return null;
        }
        List<Short> expiredStuff = new ArrayList<>();
        for(GenericAwardStructure awardItem : bundle.items) {
            if(awardItem.awardKind == AwardKindEnum.TEMPORARY_STUFF) {
                boolean result = stuffService.removeStuff(profile, (short) awardItem.itemId);
                if(result) {
                    expiredStuff.add((short) awardItem.itemId);
                }
            }
        }
        Runnable resultTask = null;
        if(!expiredStuff.isEmpty()) {
            log.info("[{}] sentStuffExpired {}", profile, expiredStuff);
            stuffService.sentStuffExpiredToMainConnection(profile, expiredStuff);
            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.SUBSCRIPTION, profile,
                    Param.eventType, "cancelVipSubscription",
                    Param.eventSource, eventSource,
                    "item", item,
                    "vipSubscriptionId", profile.getVipSubscriptionId(),
                    "removedStuff", expiredStuff
            );
            resultTask = () -> {
                try {
                    appendEvent(entity, Arrays.asList(eventSource, paymentRecord.getStatus().name(), paymentRecord.getCancelReason().name()));
                    paymentStatisticDao.update(entity);
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }
            };
        }
        return resultTask;
    }

    public Runnable cancelVipSubscriptionSoft(final UserProfile profile, final String item, String eventSource, PaymentStatisticEntity entity, VkPaymentRecord paymentRecord) {
        profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.SUBSCRIPTION, profile,
                Param.eventType, "cancelVipSubscription",
                Param.eventSource, eventSource,
                "item", item,
                "vipSubscriptionId", profile.getVipSubscriptionId(),
                "status", paymentRecord.getStatus(),
                "cancelReason", paymentRecord.getCancelReason(),
                "pendingCancel", paymentRecord.getPendingCancel()
        );
        Runnable resultTask = () -> {
            try {
                appendEvent(entity, Arrays.asList(eventSource, paymentRecord.getStatus().name(), paymentRecord.getCancelReason().name(), "" + (paymentRecord.getPendingCancel() == 1)));
                paymentStatisticDao.update(entity);
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        };
        return resultTask;
    }

    public Runnable cancelVipSubscription(final UserProfile profile, String eventSource, UserSubscription userSubscription, int vipSubscriptionId) {
        List<Integer> removedStuff = new ArrayList<>();
        bundleService.bundles.values().stream().filter(BundleStructure::isSubscriptionBundle).forEach(bundle -> {
            for(GenericAwardStructure awardItem : bundle.items) {
                if(awardItem.awardKind == AwardKindEnum.TEMPORARY_STUFF) {
                    boolean result = stuffService.removeStuff(profile, (short) awardItem.itemId);
                    if(result)
                        removedStuff.add(awardItem.itemId);
                }
            }
        });
        Runnable resultTask = null;
        if(!removedStuff.isEmpty()) {
            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.SUBSCRIPTION, profile,
                    Param.eventType, "cancelVipSubscription",
                    Param.eventSource, eventSource,
                    "vipSubscriptionId", profile.getVipSubscriptionId(),
                    "removedStuff", removedStuff
            );
            resultTask = () -> {
                try {
                    PaymentStatisticEntity entity = paymentStatisticDao.selectByTransactionId("sub_" + vipSubscriptionId);
                    ;
                    if(entity != null) {
                        List<String> event = userSubscription != null ? Arrays.asList(eventSource, userSubscription.status.name(), userSubscription.cancel_reason.name()) : Collections.singletonList(eventSource);
                        appendEvent(entity, event);
                        paymentStatisticDao.update(entity);
                    } else {
                        log.error("[{}] PaymentStatisticEntity not found by vipSubscriptionId [{}]", profile, vipSubscriptionId);
                    }
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }
            };
        }
        return resultTask;
    }

    public Runnable setVipSubscriptionExpireTime(final UserProfile profile, final String item, int vipExpireTime, String eventSource, Callable<PaymentStatisticEntity> entityProducer) {
        final BundleStructure bundle = bundleService.getBundle(item);
        if(bundle == null) {
            log.error("[{}] информация о пакете предметов не найдена! code={}", profile, item);
            return null;
        }
        Runnable resultTask = null;
        int currentVipExpireTime = Optional.ofNullable(stuffService.getBoostExpireDate(profile, BoostFamily.Vip)).map(d -> d.getTime() / 1000L).orElse(0L).intValue();
        for(GenericAwardStructure awardItem : bundle.items) {
            if(awardItem.awardKind == AwardKindEnum.TEMPORARY_STUFF) {
                stuffService.addStuffUntilTime(profile, (short) awardItem.itemId, vipExpireTime, false);
            }
        }
        if(vipExpireTime != currentVipExpireTime) {
            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.SUBSCRIPTION, profile,
                    Param.eventType, "setVipSubscriptionExpireTime",
                    Param.eventSource, eventSource,
                    "item", item,
                    "vipSubscriptionId", profile.getVipSubscriptionId(),
                    "currentVipExpireTime", AppUtils.formatDateInSeconds(currentVipExpireTime),
                    "vipExpireTime", AppUtils.formatDateInSeconds(vipExpireTime)
            );
            resultTask = () -> {
                try {
                    PaymentStatisticEntity entity = entityProducer.call();
                    if(entity != null) {
                        appendEvent(entity, Arrays.asList(eventSource, AppUtils.formatDateInSeconds(vipExpireTime)));
                        paymentStatisticDao.update(entity);
                    }
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }
            };
        }
        return resultTask;
    }

    private void appendEvent(PaymentStatisticEntity entity, List<String> event) {
        Gson gson = new Gson();
        String json = entity.getItem();
        Map<String, List<String>> events = new TreeMap<>(gson.fromJson(json, Map.class));
        events.put(AppUtils.formatDate(new Date()), event);
        entity.setItem(new Gson().toJson(events));
    }

    public PaymentStatisticEntity getPaymentStatisticEntityByTransactionId(String transactionId) {
        return paymentStatisticDao.selectByTransactionId(transactionId);
    }

    public Tuple2<Integer, List<GenericAwardStructure>> purchaseBundle(final UserProfile profile, final String transactionId, final Date transactionDate, final String item) {
        int newPaymentId = -1;
        final BundleStructure validBundle = bundleService.getValidBundle(item);
        if(validBundle == null) {
            log.error("[{}] информация о пакете предметов не найдена! code={}", profile, item);
            return Tuple.of(-1, Collections.emptyList());
        }
        if(validBundle.isVip() && !validBundle.isSubscriptionBundle() && profile.getVipSubscriptionId() > 0) {
            log.error("[{}] у игрока действует подписка на VIP. Платеж не проводим!", profile);
            return Tuple.of(-2, Collections.emptyList());
        }
        int subscriptionExpireTime;
        String entityItem;
        String note = "";
        if(validBundle.isSubscriptionBundle()) {
            subscriptionExpireTime = (int) (AppUtils.currentTimeSeconds() + TimeUnit.DAYS.toSeconds(validBundle.period));
            Map<String, List<String>> events = new TreeMap<>();
            events.put(AppUtils.formatDate(new Date()), Arrays.asList(item, "" + validBundle.period, AppUtils.formatDateInSeconds(subscriptionExpireTime)));
            entityItem = new Gson().toJson(events);
            note = transactionId.replaceFirst("sub_", "");
        } else {
            subscriptionExpireTime = 0;
            entityItem = item;
        }
        Tuple2<List<GenericAwardStructure>, Integer> items_realMoney = null;
        try {
            items_realMoney = bundleService.issueBundle(profile, validBundle);
            if(items_realMoney._1.size() == 0) {
                profile.setRealMoney(profile.getRealMoney() - items_realMoney._2);
                log.error("[{}] присутствуют все предметы из Bundle или предмет из Bundle не доступен по уровню. Платеж не проводим!", profile);
                return Tuple.of(-2, Collections.emptyList());
            }
            newPaymentId = transactionTemplate.execute(transactionStatus -> {
                final PaymentStatisticEntity entity = new PaymentStatisticEntity();
                entity.setCompleted(true);
                entity.setDate(transactionDate);
                entity.setMoneyType(validBundle.isSubscriptionBundle() ? PaymentType.VIP.type : PaymentType.BUNDLE.type);
                entity.setPaymentStatus((short) 0);
                entity.setProfileId(profile.getId());
                entity.setTransactionId(transactionId);
                entity.setUpdateDate(new Date());
                entity.setVotes(validBundle.votes);
                entity.setAmount(subscriptionExpireTime > 0 ? subscriptionExpireTime : 1);
                entity.setBalanse(profile.getRealMoney());
                entity.setItem(entityItem);

                PaymentStatisticEntity statisticEntity = paymentStatisticDao.insert(entity);
                return statisticEntity.getId();
            });
            onSuccessPayment(profile);
        } catch (TransactionException e) {
            log.error(e.toString(), e);
            return Tuple.of(-1, Collections.emptyList());
        }
        List<GenericAwardStructure> awards = items_realMoney._1;
        int reparationRealMoney = items_realMoney._2;
        if(validBundle.isSubscriptionBundle()) {
            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.SUBSCRIPTION, profile,
                    Param.eventType, "activateSubscription",
                    "subscriptionCost", validBundle.votes,
                    "subscriptionItem", item,
                    "subscriptionExpireTime", AppUtils.formatDateInSeconds(subscriptionExpireTime),
                    "bundleCode", validBundle.code,
                    "bundleItems", awards,
                    Param.realMoney, reparationRealMoney,
                    "remoteAddress", getConnectionRemoteAddress(profile)
            );
        } else {
            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PAYMENT, profile,
                    Param.eventType, PaymentType.BUNDLE,
                    "paymentCost", validBundle.votes,
                    "paymentItem", item,
                    "bundleCode", validBundle.code,
                    "bundleItems", awards,
                    Param.realMoney, reparationRealMoney,
                    "remoteAddress", getConnectionRemoteAddress(profile)
            );
        }

        log.info("[{}] покупка пакета предметов {}", profile, validBundle);

        profileService.updateSync(profile);

        int paymentAmount = 1;
        if(validBundle.isSubscriptionBundle()) {
            paymentAmount = AppUtils.currentTimeSeconds() + (int) TimeUnit.DAYS.toSeconds(validBundle.period);
        }
        sendIncomingPayment(profile, PaymentType.BUNDLE, item, paymentAmount, awards, note);
        if(reparationRealMoney > 0) {
            sendIncomingPayment(profile, PaymentType.REAL_MONEY, item, reparationRealMoney);
        }

        return Tuple.of(newPaymentId, awards);
    }

    public int wipePayment(final UserProfile profile, final int paymentAmount, final int paymentCost, final String transactionId, final Date transactionDate, final String item) {
        int newPaymentId = -1;
        try {
            if(Execution.EXECUTION.get() == null) {
                Execution.EXECUTION.set(new ExecutionContext(gameApp));
            }
            wipeProfileService.wipeAndSendResponse(profile);

            newPaymentId = transactionTemplate.execute(transactionStatus -> {
                final PaymentStatisticEntity entity = new PaymentStatisticEntity();
                entity.setCompleted(true);
                entity.setDate(transactionDate);
                entity.setMoneyType(PaymentType.WIPE.type);
                entity.setPaymentStatus((short) 0);
                entity.setProfileId(profile.getId());
                entity.setTransactionId(transactionId);
                entity.setUpdateDate(new Date());
                entity.setVotes(paymentCost);
                entity.setAmount(paymentAmount);
                entity.setBalanse(profile.getRealMoney());
                entity.setItem(item);

                PaymentStatisticEntity statisticEntity = paymentStatisticDao.insert(entity);
                return statisticEntity.getId();
            });


            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PAYMENT, profile,
                    Param.eventType, PaymentType.WIPE,
                    "paymentCost", paymentCost,
                    "paymentItem", item,
                    "remoteAddress", getConnectionRemoteAddress(profile)
            );

        } catch (TransactionException e) {
            log.error(e.toString(), e);
            return -1;
        }

        log.info("[{}] обнуление профиля", profile);

        return newPaymentId;
    }

    public void confirmMobileVip(final UserProfile profile, final String transactionId, final String item, final long startTimeMillis, final int expiryTimeInSecconds) {
        final PaymentType paymentType = PaymentType.VIP;

        profileService.setVipExpiryTime(profile, expiryTimeInSecconds);

        profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PAYMENT, profile,
                Param.eventType, paymentType,
                "paymentItem", item,
                "vipStartTime", new Date(startTimeMillis),
                "profile#vipExpiryTime", new Date(profile.getVipExpiryTime() * 1000L)
        );

        final PaymentStatisticEntity paymentStatisticEntity = softCache.get(PaymentStatisticEntity.class, transactionId);
        if(paymentStatisticEntity == null) {
            Runnable runnable = () -> {
                PaymentStatisticEntity entity = new PaymentStatisticEntity();
                entity.setCompleted(true);
                entity.setDate(new Date(startTimeMillis));
                entity.setMoneyType(paymentType.type);
                entity.setPaymentStatus((short) 0);
                entity.setProfileId(profile.getId());
                entity.setTransactionId(transactionId);
                entity.setUpdateDate(new Date());
                entity.setVotes(1);
                entity.setAmount(expiryTimeInSecconds);
                entity.setItem(item + "," + AppUtils.formatDateInSeconds(expiryTimeInSecconds));

                paymentStatisticDao.insert(entity);
                onSuccessPayment(profile);

                if(TimeUnit.SECONDS.toDays(expiryTimeInSecconds - System.currentTimeMillis() / 1000L) > 20 || debugMode) {
                    profile.setRenameVipAct(2);
                    profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.EXTRA, profile, "profile#renameVipAct", 2);
                }

                softCache.put(PaymentStatisticEntity.class, transactionId, entity);
            };
            profileService.updateSync(runnable, profile);
        } else if(expiryTimeInSecconds != paymentStatisticEntity.getAmount()) {
            Runnable runnable = () -> {
                paymentStatisticEntity.setUpdateDate(new Date());
                paymentStatisticEntity.setAmount(expiryTimeInSecconds);
                paymentStatisticEntity.setItem(paymentStatisticEntity.getItem() + "," + AppUtils.formatDateInSeconds(expiryTimeInSecconds));

                paymentStatisticDao.update(paymentStatisticEntity);
                onSuccessPayment(profile);

                if(TimeUnit.SECONDS.toDays(expiryTimeInSecconds - paymentStatisticEntity.getAmount()) > 20 || debugMode) {
                    profile.setRenameVipAct(2);
                    profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.EXTRA, profile, "profile#renameVipAct", 2);
                }
            };
            profileService.updateSync(runnable, profile);
        }

        try {
            String sessionKey = Sessions.getOpt(profile).map(Session::getKey).orElse("");
            Messages.toUser(new BuyVipResult(profile.getUserProfileStructure().rentedItems, (byte) (profile.getRenameAct() + profile.getRenameVipAct()), sessionKey), profile);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    public int openDeposit(final UserProfile profile, final int paymentAmount, final int paymentCost, final String transactionId, final Date transactionDate, final String depositTypeId) {
        final PaymentType paymentType = PaymentType.DEPOSIT;
        final DepositBean depositBean = depositService.map(service -> service.getDepositBean(depositTypeId)).orElse(null);
        if(depositBean == null) {
            log.error("[{}] информация о типе дипозита не найдена! code={}", profile, depositTypeId);
            return -1;
        }

        DepositEntity entity;
        try {
            entity = transactionTemplate.execute(transactionStatus -> {
                final PaymentStatisticEntity statEntity = new PaymentStatisticEntity();
                statEntity.setCompleted(true);
                statEntity.setDate(transactionDate);
                statEntity.setMoneyType(paymentType.type);
                statEntity.setPaymentStatus((short) 0);
                statEntity.setProfileId(profile.getId());
                statEntity.setTransactionId(transactionId);
                statEntity.setUpdateDate(new Date());
                statEntity.setVotes(paymentCost);
                statEntity.setAmount(paymentAmount);
                statEntity.setBalanse(getBalanceOf(depositBean.getMoneyType(), profile) + depositBean.getImmediateDividend());
                statEntity.setItem(depositTypeId);

                PaymentStatisticEntity statisticEntity = paymentStatisticDao.insert(statEntity);
                return depositService.get().openDeposit(depositBean, statisticEntity.getId(), transactionDate, profile);
            });

            onSuccessPayment(profile);

        } catch (TransactionException e) {
            log.error(e.toString(), e);
            return -1;
        }

        profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PAYMENT, profile,
                Param.eventType, paymentType,
                "paymentCost", paymentCost,
                "paymentItem", depositTypeId,
                Param.of(depositBean.getMoneyType()), depositBean.getImmediateDividend(),
                "depositTotalValue", depositBean.getTotalValue()
        );

        try {
            Messages.toUser(
                    new OpenDepositResult(
                            depositBean.getMoneyType(),
                            depositBean.getDividendsByDays(),
                            entity.getStartDate(),
                            entity.getProgress(),
                            depositBean.getImmediateDividend(),
                            Sessions.getKey()),
                    profile);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        return entity.getId();
    }

    public void runDailyTask() {
        if(initialized) {
            fillDonaters();
        }
    }

    public boolean isDonat(Long profileId) {
        return donaters.contains(profileId.intValue());
    }

    public Set<Integer> getDonaters() {
        return donaters;
    }

    public String getConnectionRemoteAddress(UserProfile profile) {
        return ofNullable(gameApp.getSessions().get(profile))
                .flatMap(session -> ofNullable(session.getConnection()))
                .flatMap(connection -> ofNullable(connection.getIP()))
                .orElse("");
    }

    private int getBalanceOf(MoneyType moneyType, UserProfile profile) {
        switch (moneyType) {
            case REAL_MONEY:
                return profile.getRealMoney();
            case MONEY:
                return profile.getMoney();
            default:
                return 0; // impossible
        }
    }

}
