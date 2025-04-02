package com.pragmatix.app.services.social;

import com.pragmatix.app.common.PaymentType;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.social.odnoklassniki.OdnoklassnikiPaymentProcessor;
import com.pragmatix.common.xml.XmlWrapper;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.gameapp.social.payment.PaymentRecord;
import com.pragmatix.gameapp.social.service.odnoklassniki.OdnoklassnikiService;
import com.pragmatix.testcase.AbstractSpringTest;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Author: Vladimir
 * Date: 03.08.12 11:21
 */
public class OdnoklassnikiServiceTest extends AbstractSpringTest {

    @Resource
    OdnoklassnikiService odnoklassnikiService;

    @Resource
    private OdnoklassnikiPaymentProcessor paymentProcessor;

    @Resource
    ProfileService profileService;


    private String socialUserId = "442078919308";
    private String paymentCallbackUrl = "http://127.0.0.1:6082/odnoklassniki-api/payment";

    private final int[][] priceList = new int[][]{
            {7, 3, 0, 3, 300, 0, 300},
            {12, 5, 0, 5, 500, 0, 500},
            {24, 10, 0, 10, 1000, 0, 1000},
            {48, 20, 1, 21, 2000, 100, 2100},
            {72, 30, 3, 33, 3000, 300, 3300},
            {120, 50, 6, 56, 5000, 600, 5600},
            {240, 100, 15, 115, 10000, 1500, 11500},
            {480, 200, 35, 235, 20000, 3500, 23500}
    };
    /*
200 35 235 480
    */
    private final Random rnd = new Random();

    @Test
    public void massNotificationTest() {
        Map<String, String> recipientFilter = new HashMap<String, String>();
        recipientFilter.put("birthday_range", "01.01-12.31");

        String sendId = odnoklassnikiService.sendMassNotification(recipientFilter, "Проверяем рассылку", new Date(new Date().getTime() + 48 * 3600 * 1000));

        assert sendId != null;
    }

    public void notificationTest() {
        boolean result = odnoklassnikiService.sendNotification(socialUserId, "От Васи Пупкина привет!!!");

        System.out.println(result);
    }

    public void remoteHttpPaymentTest() {
        paymentCallbackUrl = "http://my.rmart.ru:6082/odnoklassniki-api/payment/";

        Map<String, String> params = createCallbackParams();

        XmlWrapper xmlWrapper = doCallback(params);
        assert xmlWrapper != null;
        System.out.println(xmlWrapper.toXml());
        assert xmlWrapper.getString("invocation_error", null) == null;

        params.put("sig", "wrong_sig");
        xmlWrapper = doCallback(params);
        assert xmlWrapper != null;
        System.out.println(xmlWrapper.toXml());
        assert xmlWrapper.getString("invocation_error", null) != null;

    }

    private Map<String, String> createCallbackParams() {
        Random rnd = new Random();

        int ix = rnd.nextInt(priceList.length);

        Map<String, String> params = new HashMap<String, String>();

        params.put("uid", socialUserId);
        params.put("transaction_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        params.put("transaction_id", Long.toHexString(rnd.nextLong()));
        params.put("product_code", Integer.toString(rnd.nextInt(2)));
        params.put("product_option", Integer.toString(ix));
        params.put("amount", Integer.toString(priceList[ix][0]));
        params.put("currency", "ok");
        params.put("payment_system", "odnoklassniki");
        params.put("extra_attributes", "a:1");

        String signature = odnoklassnikiService.calcSignature(params);

        params.put("sig", signature);

        return params;
    }

    private XmlWrapper doCallback(Map<String, String> params) {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpClient httpClient = new HttpClient(connectionManager);
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(300);
        httpClient.getHttpConnectionManager().getParams().setSoTimeout(300);

        NameValuePair[] queryString = new NameValuePair[params.size()];
        int i = 0;
        for(String paramName : params.keySet()) {
            queryString[i++] = new NameValuePair(paramName, params.get(paramName));
        }

        GetMethod method = new GetMethod(paymentCallbackUrl);
        method.addRequestHeader("Accept", "application/xml");
        method.setQueryString(queryString);

        String requestUrl = paymentCallbackUrl + "?" + method.getQueryString();

        sleep(1000);

        System.out.println("Выполняем запрос " + requestUrl);

        try {
            int statusCode = httpClient.executeMethod(method);

            if(statusCode != HttpStatus.SC_OK) {
                System.out.println("Запрос  " + requestUrl + " вернул ошибочный статус " + method.getStatusLine());

                return new XmlWrapper("<http_status_error>" + statusCode + "</http_status_error>");
            } else {
                Header invocatiionError = method.getResponseHeader("invocation-error");

                byte[] responseBody = method.getResponseBody();

                String xml = new String(responseBody, "UTF-8");

                XmlWrapper xmlWrapper = new XmlWrapper(xml);

                if(invocatiionError != null) {
                    xmlWrapper.getContent().setAttribute("invocation_error", invocatiionError.getValue());
                }

                return xmlWrapper;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            method.releaseConnection();
        }

        return null;
    }

    private PaymentRecord paymentRecord(Long userId, PaymentType productCode, int productOption) {
        PaymentRecord payment = new PaymentRecord();
        payment.setUserId(userId.toString());
        payment.setProductCode("" + productCode.type);
        payment.setProductOption("" + productOption);
        payment.setSocialId(SocialServiceEnum.odnoklassniki);
        payment.setTransactionId(Long.toHexString(rnd.nextLong()));
        payment.setTransactionTime(new Date());
        payment.setCurrency("ok");
        payment.setPaymentSystem("odnoklassniki");
        payment.setExtraAttributes("a:1");

        return payment;
    }

    protected void sleep(int mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException ignore) {
        }
    }
}
