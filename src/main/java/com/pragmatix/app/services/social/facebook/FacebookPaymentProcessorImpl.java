package com.pragmatix.app.services.social.facebook;

import com.pragmatix.app.common.OrderBean;
import com.pragmatix.app.common.PaymentType;
import com.pragmatix.app.dao.PaymentStatisticDao;
import com.pragmatix.app.domain.PaymentStatisticEntity;
import com.pragmatix.app.messages.structures.BundleStructure;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.clanserver.services.ClanServiceImpl;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.gameapp.social.service.facebook.FacebookPaymentProcessor;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @see com.pragmatix.gameapp.social.service.facebook.FacebookRealtimeUpdatesHandler#handleRealtimeUpdates(String, org.eclipse.jetty.server.Request, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
 */

public class FacebookPaymentProcessorImpl implements FacebookPaymentProcessor {

    private final Logger log = SocialService.PAYMENT_LOGGER;

    //indexes: 0 - MoneyType 1 - moneyCount 2 - votes
    private Map<String, List<Integer>> priceByItem;

    @Resource
    private PaymentStatisticDao paymentStatisticDao;

    @Resource
    private ProfileService profileService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private ClanServiceImpl clanService;

    @Resource
    private BanService banService;

    @Resource
    private BundleService bundleService;

    @Resource
    private PaymentService paymentService;

    public void setPriceByItem(Map<String, List<Integer>> priceByItem) {
        this.priceByItem = priceByItem;
    }

    public Map<String, List<Integer>> getPriceByItem() {
        return priceByItem;
    }

    @Override
    public void fulfillPayment(final int paymentId, final long transactionId, boolean completed) {
        try {
            int resultedStatus = completed ? 0 : 1;

            final PaymentStatisticEntity paymentEntity = paymentStatisticDao.get(paymentId);
            if(paymentEntity == null) {
                log.error("платеж не найден по id [{}]", paymentId);
                return;
            } else if(paymentEntity.getPaymentStatus() == resultedStatus) {
                log.info("платеж был обработан ранее");
                return;
            }

            final UserProfile profile = profileService.getUserProfile(paymentEntity.getProfileId());
            if(profile == null) {
                log.info("профиль не найден по id {}", paymentEntity.getProfileId());
                return;
            }

            // ошибка платежа
            if(!completed) {
                final int balanse = paymentEntity.getMoneyType() == MoneyType.REAL_MONEY.getType() ? profile.getRealMoney() : profile.getMoney();
                transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                        paymentStatisticDao.updateFailurePaymentRequest(paymentId, transactionId, balanse);
                    }
                });
                log.error("[{}] платеж не подтвержден", profile);
                return;
            }

            boolean successUpdateResult = transactionTemplate.execute(new TransactionCallback<Boolean>() {
                @Override
                public Boolean doInTransaction(TransactionStatus status) {
                    int balance;
                    if(paymentEntity.getMoneyType() == MoneyType.MONEY.getType()) {
                        balance = profile.getMoney() + paymentEntity.getAmount();
                    } else {
                        balance = profile.getRealMoney() + paymentEntity.getAmount();
                    }

                    return paymentStatisticDao.updateSuccessPaymentRequest(paymentId, transactionId, balance);
                }
            });

            if(successUpdateResult) {
                paymentService.onSuccessPayment(profile);

                PaymentType paymentType = PaymentType.valueOf(paymentEntity.getMoneyType());

                if(paymentType == PaymentType.MONEY || paymentType == PaymentType.REAL_MONEY) {
                    int realMoney = 0;
                    int money = 0;
                    if(paymentEntity.getMoneyType() == MoneyType.REAL_MONEY.getType()) {
                        profile.setRealMoney(profile.getRealMoney() + paymentEntity.getAmount());
                        realMoney = paymentEntity.getAmount();
                    } else {
                        profile.setMoney(profile.getMoney() + paymentEntity.getAmount());
                        money = paymentEntity.getAmount();
                    }

                    profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PAYMENT, profile,
                            Param.eventType, paymentType,
                            "paymentItem", paymentEntity.getItem(),
                            "paymentCost", paymentEntity.getVotes(),
                            Param.realMoney, realMoney,
                            Param.money, money,
                            "remoteAddress", paymentService.getConnectionRemoteAddress(profile)
                    );

                    profileService.updateSync(profile);

                    paymentService.sendIncomingPayment(profile, paymentType,paymentEntity.getItem(), paymentEntity.getAmount());
                } else if(paymentType == PaymentType.CLAN_DONATE) {
                    int paymentAmount = paymentEntity.getAmount();
                    int paymentAmountComeback = paymentAmount;
                    for(OrderBean orderBean :  paymentService.getClanDonateOrderBeans(priceByItem)) {
                        if(orderBean.paymentAmount == paymentAmount) {
                            paymentAmountComeback = orderBean.paymentAmountComeback;
                            break;
                        }
                    }
                    clanService.donate(profile.getId().intValue(), profile.getSocialId(), paymentAmount, paymentAmountComeback);
                    profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PAYMENT, profile,
                            Param.eventType, paymentType,
                            "paymentCost", paymentEntity.getVotes(),
                            "paymentItem", paymentEntity.getItem(),
                            "clanId", profile.getClanId(),
                            "clanRank", profile.getRankInClan(),
                            "donateAmount", paymentAmount,
                            "donateAmountComeback", paymentAmountComeback,
                            "remoteAddress", paymentService.getConnectionRemoteAddress(profile)
                    );
                    log.info("[{}] пополнение казны клана: в казну [{}], вернется в случае исключения из клана [{}]", profile, paymentAmount, paymentAmountComeback);
                } else if(paymentType == PaymentType.BUNDLE) {
                    String item = paymentEntity.getItem();
                    final BundleStructure validBundle = bundleService.getValidBundle(item);
                    if(validBundle == null) {
                        log.error("[{}] информация о пакете предметов не найдена! code={}", profile, item);
                    } else {
                        Tuple2<List<GenericAwardStructure>, Integer> items_realMoney = bundleService.issueBundle(profile, validBundle);
                        profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PAYMENT, profile,
                                Param.eventType, paymentType,
                                "paymentCost", paymentEntity.getVotes(),
                                "paymentItem", paymentEntity.getItem(),
                                "bundleCode", validBundle.code,
                                "bundleItems", items_realMoney._1,
                                Param.realMoney, items_realMoney._2,
                                "remoteAddress", paymentService.getConnectionRemoteAddress(profile)
                        );
                        log.info("[{}] покупка пакета предметов {}", profile, validBundle);
                        paymentService.sendIncomingPayment(profile, paymentType, item, paymentEntity.getAmount());
                    }
                }
            } else {
                log.info("платеж был обработан ранее");
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    @Override
    public void chargeback(final int paymentId) {
        try {
            final PaymentStatisticEntity paymentEntity = paymentStatisticDao.get(paymentId);
            if(paymentEntity == null) {
                log.error("платеж не найден по id [{}]", paymentId);
                return;
            } else if(paymentEntity.getPaymentStatus() != 0) {
                log.info("платеж не был обработан ранее");
                return;
            }

            final UserProfile profile = profileService.getUserProfile(paymentEntity.getProfileId());
            if(profile == null) {
                log.info("профиль не найден по id {}", paymentEntity.getProfileId());
                return;
            }

            log.info("платеж был отозван - баним профиль [{}]", profile);

            banService.banForever(profile.getId());

            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    paymentStatisticDao.updatePaymentRequest(paymentId, paymentEntity.getPaymentStatus(), false);
                }
            });
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    public OrderBean getOrderBean(String itemId) {
        return paymentService.getOrderBean(priceByItem, itemId);
    }
}
