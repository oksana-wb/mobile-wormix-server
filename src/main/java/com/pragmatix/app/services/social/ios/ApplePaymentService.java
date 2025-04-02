package com.pragmatix.app.services.social.ios;

import com.google.gson.Gson;
import com.pragmatix.app.common.OrderBean;
import com.pragmatix.app.common.PaymentType;
import com.pragmatix.app.domain.PaymentStatisticEntity;
import com.pragmatix.app.messages.client.AppleVerifyReceipt;
import com.pragmatix.app.messages.server.VerifyPaymentResult;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.CheatersCheckerService;
import com.pragmatix.app.services.DailyRegistry;
import com.pragmatix.app.services.DaoService;
import com.pragmatix.app.services.PaymentService;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.social.SocialService;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import javax.xml.bind.DatatypeConverter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.pragmatix.app.messages.server.NeedMoneyResult.ResultEnum.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.01.14 12:10
 */
public class ApplePaymentService {

    private final Logger log = SocialService.PAYMENT_LOGGER;

    @Resource
    private DaoService daoService;

    @Resource
    private CheatersCheckerService cheatersCheckerService;

    @Resource
    private PaymentService paymentService;

    @Resource
    private DailyRegistry dailyRegistry;

    @Value("${debug.paymentResultSuccess:false}")
    private boolean debugPaymentResultSuccess = false;

    private int connectionTimeout = 1000;
    private int readTimeout = 5000;

    @Value("${paymentController.verifyReceiptUrl:}")
    private String url;

    @Value("${paymentController.password:}")
    private String password;

    //indexes: 0 - MoneyType 1 - moneyCount 2 - votes
    private Map<String, List<Integer>> priceByItem;

    public VerifyPaymentResult verifyReceipt(final AppleVerifyReceipt msg, final UserProfile profile) {
        //https://developer.apple.com/library/ios/#documentation/NetworkingInternet/Conceptual/StoreKitGuide/VerifyingStoreReceipts/VerifyingStoreReceipts.html
        VerifyPaymentResult errorResult = new VerifyPaymentResult(ERROR, PaymentType.UNDEFINED, msg.productId, 0, msg.sessionKey, "");
        if(cheatersCheckerService.checkPaymentDelay(profile)) {
            //отправляем на клиент инфу о том что произошла ошибка
            return errorResult;
        }
        String transactionId = "";
        String productId = "";
        try {
            VerifyReceiptResponse verifyReceiptResponse;
            if(!debugPaymentResultSuccess) {
                HttpClient httpClient = new HttpClient();
                httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(connectionTimeout);
                httpClient.getHttpConnectionManager().getParams().setSoTimeout(readTimeout);

                String receiptData = DatatypeConverter.printBase64Binary(msg.receiptData);
                String content = "{ \"receipt-data\" : \"" + receiptData + "\" }";
                if(msg.isSubscription) {
                    content = "{ \"receipt-data\" : \"" + receiptData + "\", \"password\" : \"" + password + "\" }";
                }
                PostMethod method = new PostMethod(url);
                method.setRequestEntity(new StringRequestEntity(content, "application/json", "UTF-8"));
                int statusCode = httpClient.executeMethod(method);

                if(statusCode != HttpStatus.SC_OK) {
                    log.error("POST request for " + url + " " + content + " resulted in error: " + method.getStatusLine());
                    return errorResult;
                }

                // Read the response body.
                String response = new String(method.getResponseBody(), "UTF-8");
                log.info("response: {}", response);
                Gson gson = new Gson();
                verifyReceiptResponse = gson.fromJson(response, VerifyReceiptResponse.class);
                log.info("verify response: {}", verifyReceiptResponse);

                if(verifyReceiptResponse.receipt != null){
                    transactionId = verifyReceiptResponse.receipt.get("transaction_id");
                    errorResult.transactionId = transactionId;

                    productId = verifyReceiptResponse.receipt.get("product_id");
                    errorResult.item = productId;
                }
            } else {
                verifyReceiptResponse = new VerifyReceiptResponse();
                verifyReceiptResponse.status = 0;
                verifyReceiptResponse.receipt = new HashMap<>();
                productId = new String(msg.receiptData, "UTF-8");
                verifyReceiptResponse.receipt.put("product_id", productId);
                transactionId = RandomStringUtils.randomAlphabetic(32);
                verifyReceiptResponse.receipt.put("transaction_id", transactionId);
                verifyReceiptResponse.receipt.put("purchase_date_ms", "" + System.currentTimeMillis());
                long vipTime = productId.contains("vip_7") ? TimeUnit.MINUTES.toMillis(5) : TimeUnit.MINUTES.toMillis(90);
                verifyReceiptResponse.receipt.put("expires_date", "" + (System.currentTimeMillis() + vipTime));

                log.info("Тестовый режим! verify response: {}", verifyReceiptResponse);
            }

            final OrderBean orderBean = paymentService.getOrderBean(priceByItem, productId);
            if(orderBean == null) {
                log.error("не зарегистрированный код {} [{}]", msg.isSubscription ? "подписки" : "платежа", productId);
                return errorResult;
            }
            errorResult.paymentType = orderBean.paymentType;

            if(verifyReceiptResponse.status != 0) {
                log.error("verify receipt failed with error [{}]", verifyReceiptResponse.status);
                if(verifyReceiptResponse.receipt != null && verifyReceiptResponse.receipt.containsKey("expires_date")) {
                    long expiryTimeMillis = Long.parseLong(verifyReceiptResponse.receipt.get("expires_date"));
                    errorResult.subscriptionExpiryTimeSecconds = (int) (expiryTimeMillis / 1000L);
                }
                return errorResult;
            }

            Date transactionDate;
            try {
                transactionDate = new Date(Long.parseLong(verifyReceiptResponse.receipt.get("purchase_date_ms")));
            } catch (Exception e) {
                log.error(e.toString());
                transactionDate = new Date();
            }

            if(!msg.isSubscription) {
                PaymentStatisticEntity paymentStatisticEntity = daoService.getPaymentStatisticDao().selectByTransactionId(transactionId);
                if(paymentStatisticEntity != null) {
                    log.error("платеж уже зарегистрирован {}", paymentStatisticEntity);
                    errorResult.result = ALREADY_PURCHASED;
                    return errorResult;
                }

                List<GenericAwardStructure> awards = Collections.emptyList();
                int expiryTimeInSecconds = 0;
                if(orderBean.paymentType == PaymentType.MONEY || orderBean.paymentType == PaymentType.REAL_MONEY) {
                    paymentService.applyPayment(profile, orderBean.paymentType, orderBean.paymentAmount, orderBean.getPaymentCostInt(), transactionId, transactionDate, productId, false);
                } else if(orderBean.paymentType == PaymentType.RENAME) {
                    paymentService.rename(profile, orderBean.paymentAmount, orderBean.getPaymentCostInt(), transactionId, transactionDate, productId);
                } else if(orderBean.paymentType == PaymentType.DEPOSIT) {
                    paymentService.openDeposit(profile, orderBean.paymentAmount, orderBean.getPaymentCostInt(), transactionId, transactionDate, productId);
                } else if(orderBean.paymentType == PaymentType.VIP) {
                    int vipFrom = Math.max(profile.getVipExpiryTime(), AppUtils.currentTimeSeconds());
                    expiryTimeInSecconds = vipFrom + (int) TimeUnit.DAYS.toSeconds(orderBean.paymentAmount);

                    paymentService.confirmMobileVip(profile, transactionId, productId, transactionDate.getTime(), expiryTimeInSecconds);
                } else if(orderBean.paymentType == PaymentType.CLAN_DONATE) {
                    paymentService.donateToClan(profile, orderBean.paymentAmount, orderBean.paymentAmountComeback, orderBean.getPaymentCostInt(), transactionId, transactionDate, productId);
                } else if(orderBean.paymentType == PaymentType.BUNDLE) {
                    awards = paymentService.purchaseBundle(profile, transactionId, transactionDate, productId)._2;
                }

                paymentService.onSuccessPayment(profile);

                String sessionKey = getSessionKey(profile);
                VerifyPaymentResult verifyPaymentResult = new VerifyPaymentResult(SUCCESS, orderBean.paymentType, productId, orderBean.paymentAmount, sessionKey, transactionId);
                verifyPaymentResult.subscriptionExpiryTimeSecconds = expiryTimeInSecconds;
                verifyPaymentResult.awards = awards;
                return verifyPaymentResult;
            } else {
                VerifyPaymentResult verifyPaymentResult;
                long expiryTimeMillis = Long.parseLong(verifyReceiptResponse.receipt.get("expires_date"));
                int expiryTimeInSecconds = (int) (expiryTimeMillis / 1000L);
                boolean verifyResult = expiryTimeMillis > System.currentTimeMillis();
                if(verifyResult) {
                    if(orderBean.paymentType == PaymentType.VIP) {
                        paymentService.confirmMobileVip(profile, transactionId, productId, transactionDate.getTime(), expiryTimeInSecconds);
                    }
                    String sessionKey = getSessionKey(profile);
                    verifyPaymentResult = new VerifyPaymentResult(SUCCESS, orderBean.paymentType, productId, orderBean.paymentAmount, sessionKey, transactionId);
                } else {
                    log.error("подписка истекла {}", AppUtils.formatDate(new Date(expiryTimeMillis)));
                    verifyPaymentResult = errorResult;
                }
                verifyPaymentResult.subscriptionExpiryTimeSecconds = expiryTimeInSecconds;
                return verifyPaymentResult;
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
            return errorResult;
        }
    }

    private String getSessionKey(UserProfile profile) {
        Session session = Sessions.get(profile);
        if(session != null) {
            return session.getKey();
        } else {
            log.error("Session is null!");
            return "";
        }
    }

    static class VerifyReceiptResponse {
        public int status;
        public Map<String, String> receipt;

        @Override
        public String toString() {
            return "VerifyReceiptResponse{" +
                    "status=" + status +
                    ", receipt=" + receipt +
                    '}';
        }
    }

    public static void main(String[] args) throws Exception {
        Logger log = LoggerFactory.getLogger(ApplePaymentService.class);
        int connectionTimeout = 1000;
        int readTimeout = 5000;
//        String url = "https://buy.itunes.apple.com/verifyReceipt";
        String url = "https://sandbox.itunes.apple.com/verifyReceipt";
        String password = "d944fe63ce3048c6bee5250ef0244d64";

        String receiptData = "ewoJInNpZ25hdHVyZSIgPSAiQWxrZnhLZFJ6R3l4K1h1VDlCTFZpTGVnSG5NWll3WDZV\n" +
                "b2xBRlNJY2xPOUN4dUJGYm9DbmxkVkRzYlJRWEN3RzZleW82Y2V3TXNOaVMrOVNreWtSUWxxZytveWpyU2k3UThVRVFVSVh1YzdLUGxySlRXelViWUFPZXc3U0plNFVQSVk5U3RJQm5Ga25lM2duT0ZaeTIwaDZzZU1LVHZjekFZeFZkRzNieWxFa0FBQURWekNDQTFNd2dnSTdvQU1DQVFJQ0NCdXA0K1BBaG0vTE1BM\n" +
                "EdDU3FHU0liM0RRRUJCUVVBTUg4eEN6QUpCZ05WQkFZVEFsVlRNUk13RVFZRFZRUUtEQXBCY0hCc1pTQkpibU11TVNZd0pBWURWUVFMREIxQmNIQnNaU0JEWlhKMGFXWnBZMkYwYVc5dUlFRjFkR2h2Y21sMGVURXpNREVHQTFVRUF3d3FRWEJ3YkdVZ2FWUjFibVZ6SUZOMGIzSmxJRU5sY25ScFptbGpZWFJwYjI0Z1\n" +
                "FYVjBhRzl5YVhSNU1CNFhEVEUwTURZd056QXdNREl5TVZvWERURTJNRFV4T0RFNE16RXpNRm93WkRFak1DRUdBMVVFQXd3YVVIVnlZMmhoYzJWU1pXTmxhWEIwUTJWeWRHbG1hV05oZEdVeEd6QVpCZ05WQkFzTUVrRndjR3hsSUdsVWRXNWxjeUJUZEc5eVpURVRNQkVHQTFVRUNnd0tRWEJ3YkdVZ1NXNWpMakVMTUF\n" +
                "rR0ExVUVCaE1DVlZNd2daOHdEUVlKS29aSWh2Y05BUUVCQlFBRGdZMEFNSUdKQW9HQkFNbVRFdUxnamltTHdSSnh5MW9FZjBlc1VORFZFSWU2d0Rzbm5hbDE0aE5CdDF2MTk1WDZuOTNZTzdnaTNvclBTdXg5RDU1NFNrTXArU2F5Zzg0bFRjMzYyVXRtWUxwV25iMzRucXlHeDlLQlZUeTVPR1Y0bGpFMU93QytvVG5S\n" +
                "TStRTFJDbWVOeE1iUFpoUzQ3VCtlWnRERWhWQjl1c2szK0pNMkNvZ2Z3bzdBZ01CQUFHamNqQndNQjBHQTFVZERnUVdCQlNKYUVlTnVxOURmNlpmTjY4RmUrSTJ1MjJzc0RBTUJnTlZIUk1CQWY4RUFqQUFNQjhHQTFVZEl3UVlNQmFBRkRZZDZPS2RndElCR0xVeWF3N1hRd3VSV0VNNk1BNEdBMVVkRHdFQi93UUVBd\n" +
                "0lIZ0RBUUJnb3Foa2lHOTJOa0JnVUJCQUlGQURBTkJna3Foa2lHOXcwQkFRVUZBQU9DQVFFQWVhSlYyVTUxcnhmY3FBQWU1QzIvZkVXOEtVbDRpTzRsTXV0YTdONlh6UDFwWkl6MU5ra0N0SUl3ZXlOajVVUllISytIalJLU1U5UkxndU5sMG5rZnhxT2JpTWNrd1J1ZEtTcTY5Tkluclp5Q0Q2NlI0Szc3bmI5bE1UQU\n" +
                "JTU1lsc0t0OG9OdGxoZ1IvMWtqU1NSUWNIa3RzRGNTaVFHS01ka1NscDRBeVhmN3ZuSFBCZTR5Q3dZVjJQcFNOMDRrYm9pSjNwQmx4c0d3Vi9abEwyNk0ydWVZSEtZQ3VYaGRxRnd4VmdtNTJoM29lSk9PdC92WTRFY1FxN2VxSG02bTAzWjliN1BSellNMktHWEhEbU9Nazd2RHBlTVZsTERQU0dZejErVTNzRHhKemV\n" +
                "iU3BiYUptVDdpbXpVS2ZnZ0VZN3h4ZjRjemZIMHlqNXdOelNHVE92UT09IjsKCSJwdXJjaGFzZS1pbmZvIiA9ICJld29KSW05eWFXZHBibUZzTFhCMWNtTm9ZWE5sTFdSaGRHVXRjSE4wSWlBOUlDSXlNREUyTFRBMUxURXhJREEyT2pJMU9qVXhJRUZ0WlhKcFkyRXZURzl6WDBGdVoyVnNaWE1pT3dvSkluQjFjbU5v\n" +
                "WVhObExXUmhkR1V0YlhNaUlEMGdJakUwTmpJNU56TXpNekF3TURBaU93b0pJblZ1YVhGMVpTMXBaR1Z1ZEdsbWFXVnlJaUE5SUNJMVl6ZGhNMkU0WXpReE1EWXlOMlJsWVRFM1kyRXpNR0k0WXpjME1HRmxPVGN5TVdWaE16SmhJanNLQ1NKdmNtbG5hVzVoYkMxMGNtRnVjMkZqZEdsdmJpMXBaQ0lnUFNBaU1UQXdNR\n" +
                "EF3TURJeE1EY3pNalkzTVNJN0Nna2laWGh3YVhKbGN5MWtZWFJsSWlBOUlDSXhORFl5T1Rjek5URXdNREF3SWpzS0NTSjBjbUZ1YzJGamRHbHZiaTFwWkNJZ1BTQWlNVEF3TURBd01ESXhNRGN6TXpJeU9TSTdDZ2tpYjNKcFoybHVZV3d0Y0hWeVkyaGhjMlV0WkdGMFpTMXRjeUlnUFNBaU1UUTJNamszTXpFMU1UQX\n" +
                "dNQ0k3Q2draWQyVmlMVzl5WkdWeUxXeHBibVV0YVhSbGJTMXBaQ0lnUFNBaU1UQXdNREF3TURBek1qVXdNek00TVNJN0Nna2lZblp5Y3lJZ1BTQWlNUzQ0TUM0Mk9TSTdDZ2tpZFc1cGNYVmxMWFpsYm1SdmNpMXBaR1Z1ZEdsbWFXVnlJaUE5SUNJMlFqYzVOREF6TlMweU56aEZMVFE0TXpJdFFUWkNNeTFETmpJd01\n" +
                "UY3pRVGcwTlRnaU93b0pJbVY0Y0dseVpYTXRaR0YwWlMxbWIzSnRZWFIwWldRdGNITjBJaUE5SUNJeU1ERTJMVEExTFRFeElEQTJPak14T2pVd0lFRnRaWEpwWTJFdlRHOXpYMEZ1WjJWc1pYTWlPd29KSW1sMFpXMHRhV1FpSUQwZ0lqRXhNVEU1TXpRM05qVWlPd29KSW1WNGNHbHlaWE10WkdGMFpTMW1iM0p0WVhS\n" +
                "MFpXUWlJRDBnSWpJd01UWXRNRFV0TVRFZ01UTTZNekU2TlRBZ1JYUmpMMGROVkNJN0Nna2ljSEp2WkhWamRDMXBaQ0lnUFNBaWRtbHdOeUk3Q2draWNIVnlZMmhoYzJVdFpHRjBaU0lnUFNBaU1qQXhOaTB3TlMweE1TQXhNem95T0RvMU1DQkZkR012UjAxVUlqc0tDU0p2Y21sbmFXNWhiQzF3ZFhKamFHRnpaUzFrW\n" +
                "VhSbElpQTlJQ0l5TURFMkxUQTFMVEV4SURFek9qSTFPalV4SUVWMFl5OUhUVlFpT3dvSkltSnBaQ0lnUFNBaWNuVXVjSEpoWjIxaGRHbDRMbmR2Y20xcGVDNXRiMkpwYkdVaU93b0pJbkIxY21Ob1lYTmxMV1JoZEdVdGNITjBJaUE5SUNJeU1ERTJMVEExTFRFeElEQTJPakk0T2pVd0lFRnRaWEpwWTJFdlRHOXpYME\n" +
                "Z1WjJWc1pYTWlPd29KSW5GMVlXNTBhWFI1SWlBOUlDSXhJanNLZlE9PSI7CgkiZW52aXJvbm1lbnQiID0gIlNhbmRib3giOwoJInBvZCIgPSAiMTAwIjsKCSJzaWduaW5nLXN0YXR1cyIgPSAiMCI7Cn0=";

        HttpClient httpClient = new HttpClient();
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(connectionTimeout);
        httpClient.getHttpConnectionManager().getParams().setSoTimeout(readTimeout);
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);

        PostMethod method = new PostMethod(url);
        String content = "{ \"receipt-data\" : \"" + receiptData + "\", \"password\" : \"" + password + "\" }";
        method.setRequestEntity(new StringRequestEntity(content, "application/json", "UTF-8"));
        int statusCode = httpClient.executeMethod(method);

        if(statusCode != HttpStatus.SC_OK) {
            log.error("POST request for " + url + " " + content + " resulted in error: " + method.getStatusLine());
        }

        // Read the response body.
        String result = new String(method.getResponseBody(), "UTF-8");
        Gson gson = new Gson();
        final VerifyReceiptResponse verifyReceiptResponse = gson.fromJson(result, VerifyReceiptResponse.class);
        long expiryTimeMillis = Long.parseLong(verifyReceiptResponse.receipt.get("expires_date"));

        log.info("verify result: {}", verifyReceiptResponse);
        log.info("expiryTime: {}", new Date(expiryTimeMillis));
    }


    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, List<Integer>> getPriceByItem() {
        return priceByItem;
    }

    public void setPriceByItem(Map<String, List<Integer>> priceByItem) {
        this.priceByItem = priceByItem;
    }

}
