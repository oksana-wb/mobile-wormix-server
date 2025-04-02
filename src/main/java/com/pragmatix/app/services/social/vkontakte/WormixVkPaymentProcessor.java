package com.pragmatix.app.services.social.vkontakte;

import com.pragmatix.app.common.OrderBean;
import com.pragmatix.app.common.PaymentType;
import com.pragmatix.app.dao.PaymentStatisticDao;
import com.pragmatix.app.domain.PaymentStatisticEntity;
import com.pragmatix.app.messages.server.VipSubscriptionResponse;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.PaymentService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.gameapp.social.service.VkontakteService;
import com.pragmatix.gameapp.social.service.vkontakte.*;
import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 11.10.12 12:57
 */
public class WormixVkPaymentProcessor implements VkPaymentProcessor {

    private final Logger log = SocialService.PAYMENT_LOGGER;

    //indexes: 0 - MoneyType 1 - moneyCount 2 - votes
    private Map<String, List<Integer>> priceByItem;

    private String photoUrlTemplate;

    // кеширование по умолчанию - 5 минут
    private int expiration = 5 * 60;// в секундах

    private Set<String> ignoredOffers = new ConcurrentHashSet<>();

    @Resource
    private ProfileService profileService;

    @Resource
    private PaymentStatisticDao paymentStatisticDao;

    @Resource
    private PaymentService paymentService;

    @Resource
    private VkontakteService vkontakteService;

    private Map<String, String[]> customItemIconsAndTitle = Collections.emptyMap();

    /**
     * Получение информации о товаре
     */
    @Override
    public Map<String, Object> handleGetItem(VkPaymentRecord paymentRecord) throws VkPaymentHandleException {
        String item = paymentRecord.getItem();
        Map<String, Object> result = new HashMap<>();
        result.put("title", getTitle(item, paymentRecord.getLang()));
        result.put("item_id", item);
        result.put("price", getPrice(item));
        result.put("photo_url", getPhotoUrl(item));
        result.put("expiration", expiration);
        return result;
    }

    /**
     * Получение информации о подписке
     */
    @Override
    public Map<String, Object> handleGetSubscription(VkPaymentRecord paymentRecord) throws VkPaymentHandleException {
        String item = paymentRecord.getItem();
        Map<String, Object> result = new HashMap<>();
        result.put("item_id", item);
        result.put("title", getTitle(item, paymentRecord.getLang()));
        result.put("photo_url", getPhotoUrl(item));
        result.put("price", getPrice(item));
        result.put("period", getPeriod(item));
        result.put("trial_duration ", getTrialDuration(item));
        result.put("expiration", expiration);
        return result;
    }

    /**
     * Изменение статуса заказа
     */
    @Override
    public Map<String, Object> handleOrderStatusChange(final VkPaymentRecord paymentRecord) throws VkPaymentHandleException {
        if(paymentRecord.getStatus() != VkPaymentRecord.Status.chargeable) {
            throw new VkPaymentHandleException("Оплата заказа не подтверждена; status=" + paymentRecord.getStatus(), VkPaymentHandleError.WRONG_CALLBACK_REQUEST);
        }
        final long transactionId = paymentRecord.getOrderId();
        String item = paymentRecord.getItem();
        OrderBean orderBean = getOrderBean(item, true);
        if(orderBean == null) {
            // значит пришел offer
            if(ignoredOffers.contains(item)) {
                // offer необходимо игнорировать
                log.info("игнорируем offer [{}]", item);
                return success(transactionId, (int) transactionId);
            } else {
                // один голос переводим в 2 рубина
                orderBean = new OrderBean(PaymentType.REAL_MONEY, paymentRecord.getItemPrice() * 2, paymentRecord.getItemPrice());
            }
        }
        int paymentPrice = paymentRecord.getItemPrice();
        if(paymentPrice != orderBean.getPaymentCostInt()) {
            throw new VkPaymentHandleException(
                    String.format("Цена в платеже не соответствует покупке; item=%s, paymentPrice=%s; paymentCost=%s", item, paymentPrice, orderBean.paymentCost),
                    VkPaymentHandleError.WRONG_CALLBACK_REQUEST);
        }

        final UserProfile profile = profileService.getUserProfile((long) paymentRecord.getReceiverId());
        if(profile == null) {
            throw new VkPaymentHandleException(VkPaymentHandleError.PROFILE_NOT_FOUND);
        }

        PaymentStatisticEntity paymentStatisticEntity = paymentStatisticDao.selectByTransactionId(transactionId);
        if(paymentStatisticEntity != null) {
            if(paymentStatisticEntity.getPaymentStatus() == 0) {
                log.info("Повторный запрос orderStatusChange для order_id [{}]", transactionId);
                return success(transactionId, paymentStatisticEntity.getId());
            } else {
                throw new VkPaymentHandleException(
                        String.format("Платеж [%s] найден, но имеет ошибочный статус [%s]", transactionId, paymentStatisticEntity.getPaymentStatus()),
                        VkPaymentHandleError.ERROR);
            }
        }

        int newPaymentId = -1;
        Date paymentDate = paymentRecord.getDate();
        if(orderBean.paymentType == PaymentType.MONEY || orderBean.paymentType == PaymentType.REAL_MONEY) {
            newPaymentId = paymentService.applyPayment(profile, orderBean.paymentType, orderBean.paymentAmount, orderBean.getPaymentCostInt(), "" + transactionId, paymentDate, item, true);
        } else if(orderBean.paymentType == PaymentType.CLAN_DONATE) {
            newPaymentId = paymentService.donateToClan(profile, orderBean.paymentAmount, orderBean.paymentAmountComeback, orderBean.getPaymentCostInt(), "" + transactionId, paymentDate, item);
        } else if(orderBean.paymentType == PaymentType.BUNDLE) {
            newPaymentId = paymentService.purchaseBundle(profile, "" + transactionId, paymentDate, item)._1;
        }

        if(newPaymentId == -1) {
            // возвращаяем ошибку, плетеж будет передоставлен позднее
            throw new VkPaymentHandleException(VkPaymentHandleError.DATABASE_ERROR);
        } else if(newPaymentId == -2) {
            // возвращаяем ошибку
            throw new VkPaymentHandleException(VkPaymentHandleError.PROFILE_NOT_FOUND);
        }

        return success(transactionId, newPaymentId);
    }

    /**
     * Изменение статуса подписки
     */
    @Override
    public Map<String, Object> handleSubscriptionStatusChange(VkPaymentRecord paymentRecord) throws VkPaymentHandleException {
        int subscriptionId = paymentRecord.getSubscriptionId();
        final String transactionId = "sub_" + subscriptionId;
        final UserProfile profile = profileService.getUserProfile((long) paymentRecord.getUserId());
        if(profile == null) {
            throw new VkPaymentHandleException(VkPaymentHandleError.PROFILE_NOT_FOUND);
        }
        final String item = paymentRecord.getItemId();

        if(paymentRecord.getStatus() == VkPaymentRecord.Status.chargeable) {
            OrderBean orderBean = getOrderBean(item, true);
            if(orderBean == null || orderBean.paymentType != PaymentType.BUNDLE) {
                throw new VkPaymentHandleException("Подписка не зарегистрирована! item=" + item, VkPaymentHandleError.WRONG_CALLBACK_REQUEST);
            }
            int paymentPrice = paymentRecord.getItemPrice();
            if(paymentPrice != orderBean.getPaymentCostInt()) {
                throw new VkPaymentHandleException(
                        String.format("Цена в платеже не соответствует покупке; item=%s, paymentPrice=%s; paymentCost=%s", item, paymentPrice, orderBean.paymentCost),
                        VkPaymentHandleError.WRONG_CALLBACK_REQUEST);
            }

            if(profile.getVipSubscriptionId() > 0 && profile.getVipSubscriptionId() != subscriptionId) {
                throw new VkPaymentHandleException("Подписка VIP уже присутствует!", VkPaymentHandleError.ERROR);
            }

            PaymentStatisticEntity paymentStatisticEntity = paymentStatisticDao.selectByTransactionId(transactionId);
            if(paymentStatisticEntity != null) {
                log.info("Повторный запрос handleSubscriptionStatusChange status => chargeable для transactionId [{}]", transactionId);
                return successSubscription(profile, subscriptionId, paymentStatisticEntity.getId(), null);
            }

            int newPaymentId = paymentService.purchaseBundle(profile, transactionId, new Date(), item)._1;

            if(newPaymentId == -1) {
                // возвращаяем ошибку, платеж будет передоставлен позднее
                throw new VkPaymentHandleException(VkPaymentHandleError.DATABASE_ERROR);
            } else if(newPaymentId == -2) {
                // возвращаяем ошибку
                throw new VkPaymentHandleException(VkPaymentHandleError.WRONG_CALLBACK_REQUEST);
            }
            return successSubscription(profile, subscriptionId, newPaymentId, null);
        } else if(paymentRecord.getStatus() == VkPaymentRecord.Status.active) {
            PaymentStatisticEntity paymentStatisticEntity = paymentStatisticDao.selectByTransactionId(transactionId);
            if(paymentStatisticEntity != null) {
                if(paymentRecord.getPendingCancel() != 1) {
                    Optional<UserSubscription> userSubscriptionOpt = vkontakteService.getUserSubscription(profile.id, subscriptionId);
                    if(userSubscriptionOpt.isPresent()) {
                        UserSubscription userSubscription = userSubscriptionOpt.get();
                        int subscriptionExpireTime = userSubscription.period_start_time + (int) TimeUnit.DAYS.toSeconds(userSubscription.period);
                        Runnable task = paymentService.setVipSubscriptionExpireTime(profile, item, subscriptionExpireTime, "statusChangeToActive", () -> paymentStatisticEntity);
                        paymentService.sendIncomingPayment(profile, PaymentType.BUNDLE, item, subscriptionExpireTime);

                        return successSubscription(profile, subscriptionId, paymentStatisticEntity.getId(), task);
                    } else {
                        throw new VkPaymentHandleException("подписка не найдена (subscriptionId=" + subscriptionId + ") " + paymentRecord.getStatus(), VkPaymentHandleError.ITEM_NOT_FOUND);
                    }
                } else {
                    Runnable task = paymentService.cancelVipSubscriptionSoft(profile, item, "statusChangeToPendingCancel", paymentStatisticEntity, paymentRecord);
                    VipSubscriptionResponse vipSubscriptionResponse = vkontakteService.getUserSubscription(profile.id, subscriptionId)
                            .map(VipSubscriptionResponse::new)
                            .orElse(new VipSubscriptionResponse(subscriptionId));
                    paymentService.sendMessage(profile, vipSubscriptionResponse);

                    return successSubscription(profile, subscriptionId, paymentStatisticEntity.getId(), task);
                }
            } else {
                throw new VkPaymentHandleException("подписка не найдена (transactionId=" + transactionId + ") " + paymentRecord.getStatus(), VkPaymentHandleError.ITEM_NOT_FOUND);
            }
        } else if(paymentRecord.getStatus() == VkPaymentRecord.Status.cancelled) {
            PaymentStatisticEntity paymentStatisticEntity = paymentStatisticDao.selectByTransactionId(transactionId);
            if(paymentStatisticEntity != null) {
                if(paymentRecord.getPendingCancel() != 1) {
                    Runnable task = paymentService.cancelVipSubscriptionSoft(profile, item, "statusChangeToCancel", paymentStatisticEntity, paymentRecord);
                    return successSubscription(profile, 0, paymentStatisticEntity.getId(), task);
                } else {
                    Runnable task = paymentService.cancelVipSubscriptionSoft(profile, item, "statusChangeToPendingCancel", paymentStatisticEntity, paymentRecord);
                    return successSubscription(profile, subscriptionId, paymentStatisticEntity.getId(), task);
                }
            } else {
                throw new VkPaymentHandleException("подписка не найдена (transactionId=" + transactionId + ") " + paymentRecord.getStatus(), VkPaymentHandleError.ITEM_NOT_FOUND);
            }
        }
        throw new VkPaymentHandleException("заглушка для " + paymentRecord.getStatus(), VkPaymentHandleError.WRONG_CALLBACK_REQUEST);
    }

    private Map<String, Object> success(long transactionId, Integer appOrderId) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("order_id", transactionId);
        result.put("app_order_id", appOrderId);
        return result;
    }

    private Map<String, Object> successSubscription(UserProfile profile, int subscriptionId, int appOrderId, Runnable task) {
        profile.setVipSubscriptionId(subscriptionId);
        profileService.updateSync(task, profile);

        HashMap<String, Object> result = new HashMap<>();
        result.put("subscription_id", subscriptionId);
        result.put("app_order_id", appOrderId);
        return result;
    }

    private String getPhotoUrl(String item) throws VkPaymentHandleException {
        String img = "ruby";
        if(customItemIconsAndTitle.containsKey(item)) {
            img = customItemIconsAndTitle.get(item)[0];
        } else {
            PaymentType type = getMoneyType(item);
            if(type == PaymentType.MONEY) {
                img = "fuzy";
            } else if(type == PaymentType.BUNDLE) {
                img = "bundle";
            }
        }
        return String.format(photoUrlTemplate, img);
    }

    public String getTitle(String item, VkPaymentRecord.Lang lang) throws VkPaymentHandleException {
        if(customItemIconsAndTitle.containsKey(item)) {
            return customItemIconsAndTitle.get(item)[1];
        } else {
            int itemCount = getItemCount(item);
            PaymentType paymentType = getMoneyType(item);
            if(paymentType == PaymentType.REAL_MONEY || paymentType == PaymentType.CLAN_DONATE) {
                if(lang == VkPaymentRecord.Lang.ru_RU || lang == VkPaymentRecord.Lang.be_BY) {
                    return getRussianPhrase(itemCount, "рубин");
                } else if(lang == VkPaymentRecord.Lang.uk_UA) {
                    String russianPhrase = getRussianPhrase(itemCount, "рубин");
                    return translateTuUkr(russianPhrase);
                } else if(lang == VkPaymentRecord.Lang.en_US) {
                    return String.format("%s ruby", itemCount);
                } else {
                    return getRussianPhrase(itemCount, "рубин");
                }
            } else if(paymentType == PaymentType.MONEY) {
                if(lang == VkPaymentRecord.Lang.ru_RU || lang == VkPaymentRecord.Lang.be_BY) {
                    return getRussianPhrase(itemCount, "фуз");
                } else if(lang == VkPaymentRecord.Lang.uk_UA) {
                    String russianPhrase = getRussianPhrase(itemCount, "фуз");
                    return translateTuUkr(russianPhrase);
                } else if(lang == VkPaymentRecord.Lang.en_US) {
                    return String.format("%s fuze", itemCount);
                } else {
                    return getRussianPhrase(itemCount, "фуз");
                }
            } else if(paymentType == PaymentType.BUNDLE) {
//            if(lang == VkPaymentRecord.Lang.ru_RU || lang == VkPaymentRecord.Lang.be_BY) {
//                return "Пакет предметов";
//            } else if(lang == VkPaymentRecord.Lang.uk_UA) {
//                return "Пакет предметов";
//            } else if(lang == VkPaymentRecord.Lang.en_US) {
//                return "Bundle";
//            } else {
                return "Пакет предметов";
//            }
            } else {
                return item;
            }
        }
    }

    private PaymentType getMoneyType(String item) throws VkPaymentHandleException {
        return getOrderBean(item, false).paymentType;
    }

    private Integer getItemCount(String item) throws VkPaymentHandleException {
        return getOrderBean(item, false).paymentAmount;
    }

    private Integer getPrice(String item) throws VkPaymentHandleException {
        return getOrderBean(item, false).getPaymentCostInt();
    }

    private Integer getPeriod(String item) throws VkPaymentHandleException {
        return getOrderBean(item, false).period;
    }

    private Integer getTrialDuration(String item) throws VkPaymentHandleException {
        return getOrderBean(item, false).trialDuration;
    }

    private OrderBean getOrderBean(String item, boolean allowNull) throws VkPaymentHandleException {
        OrderBean orderBean = paymentService.getOrderBean(priceByItem, item);
        if(orderBean == null && !allowNull) {
            throw new VkPaymentHandleException(VkPaymentHandleError.ITEM_NOT_FOUND);
        }
        return orderBean;
    }

    public String translateTuUkr(String russianPhrase) {
        if(russianPhrase.endsWith("фузов")) {
            return russianPhrase.replaceFirst("фузов", "фузiв");
        }
        if(russianPhrase.endsWith("рубина")) {
            return russianPhrase.replaceFirst("рубина", "рубіна");
        }
        if(russianPhrase.endsWith("рубинов")) {
            return russianPhrase.replaceFirst("рубинов", "рубінів");
        }
        if(russianPhrase.endsWith("фуз")) {
            return russianPhrase.replaceFirst("фуз", "фуз");
        }
        if(russianPhrase.endsWith("фуза")) {
            return russianPhrase.replaceFirst("фуза", "фуза");
        }
        if(russianPhrase.endsWith("рубин")) {
            return russianPhrase.replaceFirst("рубин", "рубін");
        }

        return russianPhrase;
    }

    public String getRussianPhrase(int itemCount, String itemSinglarWord) {
        String count = String.valueOf(itemCount);
        char last = count.charAt(count.length() - 1);
        boolean isSecondDecade = count.length() == 2 && count.startsWith("1");
        String ending = "ов";
        switch (last) {
//            case '0': ending="ов";break;
            case '1':
                ending = (isSecondDecade ? "ов" : "");
                break;
            case '2':
                ending = (isSecondDecade ? "ов" : "а");
                break;
            case '3':
                ending = (isSecondDecade ? "ов" : "а");
                break;
            case '4':
                ending = (isSecondDecade ? "ов" : "а");
                break;
//            case '5': ending = itemSinglarWord + "ов";break;
//            case '6': ending = itemSinglarWord + "ов";break;
//            case '7': ending = itemSinglarWord + "ов";break;
//            case '8': ending = itemSinglarWord + "ов";break;
//            case '9': ending = itemSinglarWord + "ов";break;
        }
        return String.format("%s %s%s", count, itemSinglarWord, ending);
    }

//====================== Getters and Setters =================================================================================================================================================

    public void setPriceByItem(Map<String, List<Integer>> priceByItem) {
        this.priceByItem = priceByItem;
    }

    public void setPhotoUrlTemplate(String photoUrlTemplate) {
        this.photoUrlTemplate = photoUrlTemplate;
    }

    public void setExpiration(int expiration) {
        this.expiration = expiration;
    }

    public Set<String> getIgnoredOffers() {
        return ignoredOffers;
    }

    public void setIgnoredOffers(Set<String> ignoredOffers) {
        this.ignoredOffers = new ConcurrentHashSet<>(ignoredOffers);
        log.info("ignored offers: {}", ignoredOffers);
    }

    public void addIgnoredOffer(String ignoredOffer) {
        log.info("add ignored offer [{}]", ignoredOffer);
        this.ignoredOffers.add(ignoredOffer);
        log.info("ignored offers: {}", ignoredOffers);
    }

    public Map<String, String[]> getCustomItemIconsAndTitle() {
        return customItemIconsAndTitle;
    }

    public void setCustomItemIconsAndTitle(Map<String, String[]> customItemIconsAndTitle) {
        this.customItemIconsAndTitle = customItemIconsAndTitle;
    }
}
