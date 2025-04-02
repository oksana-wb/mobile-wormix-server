package com.pragmatix.app.services.social.android;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.ProductPurchase;
import com.google.api.services.androidpublisher.model.SubscriptionPurchase;
import com.pragmatix.app.common.OrderBean;
import com.pragmatix.app.common.PaymentType;
import com.pragmatix.app.dao.PaymentStatisticDao;
import com.pragmatix.app.domain.PaymentStatisticEntity;
import com.pragmatix.app.messages.server.NeedMoneyResult;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.DailyRegistry;
import com.pragmatix.app.services.PaymentService;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.common.utils.VarInt;
import com.pragmatix.common.utils.VarObject;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.gameapp.social.SocialService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 09.12.13 17:42
 */

public class GooglePurchaseService {

    private final Logger log = SocialService.PAYMENT_LOGGER;

    @Resource
    private PaymentService paymentService;

    @Resource
    private SoftCache softCache;

    @Resource
    private PaymentStatisticDao paymentStatisticDao;

    @Value("${debug.paymentResultSuccess:false}")
    private boolean debugPaymentResultSuccess = false;

    // https://console.developers.google.com/project/821670409723/apiui/credential?authuser=0

    @Value("${google.applicationName}")
    protected String applicationName = "Wormix: Notify";

    //Client ID for Android application: Package name
    @Value("${google.packageName}")
    protected String packageName = "air.ru.pragmatix.wormix.mobile";

    // используется Service Account проекта "Crash Racing: Notify" https://console.developers.google.com/project/614018623323/apiui/credential?authuser=0

    //Service Account: Email address
    @Value("${google.serviceAccountId}")
    protected String serviceAccountId = "614018623323-pldmm0mcvbtjm5qitll2ln7iamb9an4m@developer.gserviceaccount.com";

    //Service Account: generated P12 private key
    @Value("${google.serviceAccountP12Key}")
    protected String serviceAccountP12Key = "ssl/Crash_Racing_Notify-76e15dcd6bd9.p12";

    //indexes: 0 - MoneyType 1 - moneyCount 2 - votes
    private Map<String, List<Integer>> priceByItem;

    // https://developers.google.com/android-publisher/api-ref/purchases/products/get
    public NeedMoneyResult.ResultEnum validateInappPurchase(UserProfile profile, String productId, String purchaseToken, VarObject<OrderBean> varOrderBean, List<GenericAwardStructure> awards) {
        try {
            ProductPurchase productPurchase;
            if(!debugPaymentResultSuccess) {
                AndroidPublisher publisher = getAndroidPublisher();

                AndroidPublisher.Purchases.Products products = publisher.purchases().products();
                AndroidPublisher.Purchases.Products.Get get = products.get(packageName, productId, purchaseToken);

                productPurchase = get.execute();

                log.info("productId={}, purchaseToken={} -> \n{}", productId, purchaseToken, productPurchase.toString());
            } else {
                productPurchase = new ProductPurchase();
                productPurchase.setPurchaseState(0);
                productPurchase.setConsumptionState(0);
                productPurchase.setPurchaseTimeMillis(System.currentTimeMillis());

                log.info("Тестовый режим! productId={}, purchaseToken={} -> \n{}", productId, purchaseToken, productPurchase.toString());
            }

            boolean verifyResult = productPurchase.getPurchaseState() == 0 && productPurchase.getConsumptionState() == 0;
            if(verifyResult) {
                OrderBean orderBean = paymentService.getOrderBean(priceByItem, productId);
                if(orderBean == null) {
                    log.error("не зарегистрированный код платежа [{}]", productId);
                    return NeedMoneyResult.ResultEnum.ERROR;
                }

                varOrderBean.value = orderBean;
                String transactionId = purchaseToken;
                Date transactionDate = new Date(productPurchase.getPurchaseTimeMillis());

                PaymentStatisticEntity paymentStatisticEntity = paymentStatisticDao.selectByTransactionId(transactionId);
                if(paymentStatisticEntity != null) {
                    log.error("платеж уже зарегистрирован {}", paymentStatisticEntity);
                    return NeedMoneyResult.ResultEnum.ALREADY_PURCHASED;
                }

                if(orderBean.paymentType == PaymentType.MONEY || orderBean.paymentType == PaymentType.REAL_MONEY) {
                    paymentService.applyPayment(profile, orderBean.paymentType, orderBean.paymentAmount, orderBean.getPaymentCostInt(), transactionId, transactionDate, productId, false);
                } else if(orderBean.paymentType == PaymentType.RENAME) {
                    paymentService.rename(profile, orderBean.paymentAmount, orderBean.getPaymentCostInt(), transactionId, transactionDate, productId);
                } else if(orderBean.paymentType == PaymentType.DEPOSIT) {
                    paymentService.openDeposit(profile, orderBean.paymentAmount, orderBean.getPaymentCostInt(), transactionId, transactionDate, productId);
                } else if(orderBean.paymentType == PaymentType.CLAN_DONATE) {
                    paymentService.donateToClan(profile, orderBean.paymentAmount, orderBean.paymentAmountComeback, orderBean.getPaymentCostInt(), transactionId, transactionDate, productId);
                } else if(orderBean.paymentType == PaymentType.BUNDLE) {
                    awards.addAll(paymentService.purchaseBundle(profile, transactionId, transactionDate, productId)._2);
                }

                paymentService.onSuccessPayment(profile);
            } else {
                log.error("платеж {}!", productPurchase.getPurchaseState() != 0 ? "не оплачен" : "уже погашен");
            }
            return verifyResult ? NeedMoneyResult.ResultEnum.SUCCESS : NeedMoneyResult.ResultEnum.ERROR;

        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        return NeedMoneyResult.ResultEnum.ERROR;
    }

    // https://developers.google.com/android-publisher/api-ref/purchases/subscriptions/get
    public NeedMoneyResult.ResultEnum validateInappSubscription(UserProfile profile, String productId, String purchaseToken, VarObject<PaymentType> varSubscriptionType, VarInt varExpiryTimeInSecconds) {
        try {
            SubscriptionPurchase subscriptionPurchase;
            if(!debugPaymentResultSuccess) {
                AndroidPublisher publisher = getAndroidPublisher();

                AndroidPublisher.Purchases.Subscriptions subscriptions = publisher.purchases().subscriptions();
                AndroidPublisher.Purchases.Subscriptions.Get get = subscriptions.get(packageName, productId, purchaseToken);

                subscriptionPurchase = get.execute();

                log.info("productId={}, purchaseToken={} -> \n{}", productId, purchaseToken, subscriptionPurchase.toString());
            } else {
                subscriptionPurchase = new SubscriptionPurchase();
                subscriptionPurchase.setPaymentState(1);
                subscriptionPurchase.setStartTimeMillis(System.currentTimeMillis());
                long vipTime = productId.equals("vip7") ? TimeUnit.MINUTES.toMillis(5) : TimeUnit.MINUTES.toMillis(90);
                subscriptionPurchase.setExpiryTimeMillis(System.currentTimeMillis() + vipTime);

                log.info("Тестовый режим!  productId={}, purchaseToken={} -> \n{}", productId, purchaseToken, subscriptionPurchase.toString());
            }

            varExpiryTimeInSecconds.value = (int) (subscriptionPurchase.getExpiryTimeMillis() / 1000L);
            boolean verifyResult = subscriptionPurchase.getExpiryTimeMillis() > System.currentTimeMillis()
                    && subscriptionPurchase.getPaymentState() != null && subscriptionPurchase.getPaymentState() == 1;
            if(verifyResult) {
                PaymentStatisticEntity paymentStatisticEntity = softCache.get(PaymentStatisticEntity.class, purchaseToken);
                if(paymentStatisticEntity != null) {
                    if(!Objects.equals(paymentStatisticEntity.getProfileId(), profile.getId())) {
                        log.error("подписка [{}] оформлена на другого игрока [{}]!", productId, paymentStatisticEntity.getProfileId());
                        return NeedMoneyResult.ResultEnum.ERROR;
                    }
                }

                OrderBean orderBean = paymentService.getOrderBean(priceByItem, productId);
                if(orderBean == null) {
                    log.error("не зарегистрированный код подписки [{}]", productId);
                    return NeedMoneyResult.ResultEnum.ERROR;
                }

                varSubscriptionType.value = orderBean.paymentType;
                if(orderBean.paymentType == PaymentType.VIP) {
                    paymentService.confirmMobileVip(profile, purchaseToken, productId, subscriptionPurchase.getStartTimeMillis(), varExpiryTimeInSecconds.value);
                }
                return NeedMoneyResult.ResultEnum.SUCCESS;
            } else {
                if(subscriptionPurchase.getCancelReason() != null) {
                    String cancelReason = subscriptionPurchase.getCancelReason() == 0 ?
                            "User cancelled the subscription" :
                            "Subscription was cancelled by the system, for example because of a billing problem";
                    log.error("подписка отменена [{}]", cancelReason);
                } else if(subscriptionPurchase.getPaymentState() == null || subscriptionPurchase.getPaymentState() != 1) {
                    log.error("не валидный paymentState {}", subscriptionPurchase.getPaymentState());
                } else {
                    log.error("подписка истекла {}", AppUtils.formatDate(new Date(subscriptionPurchase.getExpiryTimeMillis())));
                }
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        return NeedMoneyResult.ResultEnum.ERROR;
    }

    public AndroidPublisher getAndroidPublisher() throws GeneralSecurityException, IOException {
        return getAndroidPublisher(null);
    }

    public AndroidPublisher getAndroidPublisher(GoogleClientRequestInitializer googleClientRequestInitializer) throws GeneralSecurityException, IOException {
        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        HttpRequestInitializer credential = new GoogleCredential.Builder().setTransport(transport)
                .setJsonFactory(jsonFactory)
                .setServiceAccountId(serviceAccountId)
                .setServiceAccountScopes(Collections.singleton("https://www.googleapis.com/auth/androidpublisher"))
                .setServiceAccountPrivateKeyFromP12File(new File(serviceAccountP12Key))
                .build();

        return new AndroidPublisher.Builder(transport, jsonFactory, credential)
                .setApplicationName(applicationName)
                .setGoogleClientRequestInitializer(googleClientRequestInitializer)
                .build();
    }

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        String productId = "vip7";
        //String purchaseToken = "ocemidlifkmmdlijbbcaefhg.AO-J1OzZTySsfY4uqHUuhqdsOCjutXtgVW4oUdhmsjF14p1Y-oivYZhOun3QOpXg3pgE9TJSSte-6FpJQ7RUrP849rXbBD2b--sjjlBl-fnGbs_Oo5Ag3uPYKETR2pcoSvk5lJUzOXlx ";

        Stream.of("obfleemehoijaeandfpgidhi.AO-J1OzQ4P-ExOBVwaNszRWunRs3GKOZRCMkaIjFvGoTmJUjJK0WMeKE6rnHidvpHGULAKqYxPomXfKp1Ld6zhDlKNC6HWqYUXUYJ80_Gbhx5LdPLfD8YE5BWUIxh575DuOYQSoNVPT-",
                "nijjongahcmennmcikpmlbmp.AO-J1OwLaxCEYnT0hhHXffzsPN4lMUDHQhFVJmSUvYMXsievo9JMA860aLIQusdBCKNRMyFOc5sFX19tipdoysiCS2l-ots1dHTaIv8Peg1J5tLI-U07BuBArY5u7TW0Mz8KB9ZMbfD0",
                "iidhbijfdhbdneppdfmldmed.AO-J1OzTDNAgAYLmB3zPmGrBOyHuU9f298L9WFgZ3hVfxjH77EuVFzmDtM98wKJX_2fic1OfVuV54GO1apjaKGjcez_hiCB8FYF1RTSHbzwg5V49NCVoH3moz1YtjF2_yV_MO7ROZBJB",
                "hinocnanajncflheomfpbabj.AO-J1OwHsKOWeN6oZjKN_lsb68AeYqgDluP0QtOgBYxSL0vo7HDAbnbhdQEmP9d9RRXQLkbvNvNUU6QNBpmlaJhG109_4ROM3kwVAZ-75eVqgH7dNu_xS7CKX0EPd8CeNNLXfvZy33U8",
                "poaoiicegblomikibdbjdodm.AO-J1Oym-sjufzwLjSLetnSgcBFTF2QB6H6ChLjKjD2Gbg4IyrE5BvhdiA5J7lfxGVbXRSWOwd8z8xPO7Ld2m7lMc1gRW_opI9XeJ_C9ssiTnf8hEMNoWgldS-fIesvECkK83OeSSwsc",
                "gliffhhloljbldmhlkalijbo.AO-J1OxbNmrZf8TKXd1NB-9vsmuue74gLh9-ouCaxNzfnZMJf9PpavcBTQ_U-Wn8bhGnzEH0V0iXchwK6G03CHoHL-Uqd6SnvdJP4IfRllnt3EoBvHE8PjjmPU8kqHqdO78WuchhcyNE",
                "kbnnninifcdmpaoahpclmadh.AO-J1Ox1RyA2LahZi6Ae-O4UVpe_6-s6sMXaLcwZjcy3hblyHm68xLSJrnDcPCM7AEbulpidXbyRUE8iIF1fKYYFGzLU1AAgdl-ONVygM1reylRBJYLxFXf-qT7AH6iT-W0CNX9c7EHd"
        ).forEach(purchaseToken -> {
            try {
                GooglePurchaseService service = new GooglePurchaseService();
                AndroidPublisher publisher = service.getAndroidPublisher();
                AndroidPublisher.Purchases.Subscriptions subscriptions = publisher.purchases().subscriptions();
                AndroidPublisher.Purchases.Subscriptions.Get get = subscriptions.get(service.packageName, productId, purchaseToken);
                SubscriptionPurchase subscriptionPurchase = get.execute();

                System.out.println(purchaseToken + " : " + subscriptionPurchase.get("linkedPurchaseToken"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


    }

    //====================== Getters and Setters =================================================================================================================================================

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getServiceAccountId() {
        return serviceAccountId;
    }

    public void setServiceAccountId(String serviceAccountId) {
        this.serviceAccountId = serviceAccountId;
    }

    public String getServiceAccountP12Key() {
        return serviceAccountP12Key;
    }

    public void setServiceAccountP12Key(String serviceAccountP12Key) {
        this.serviceAccountP12Key = serviceAccountP12Key;
    }

    public Map<String, List<Integer>> getPriceByItem() {
        return priceByItem;
    }

    public void setPriceByItem(Map<String, List<Integer>> priceByItem) {
        this.priceByItem = priceByItem;
    }
}
