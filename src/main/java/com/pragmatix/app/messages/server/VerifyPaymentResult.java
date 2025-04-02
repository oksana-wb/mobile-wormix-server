package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.PaymentType;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.serialization.annotations.Command;

/**
 * Результат валидации платежа на apple.com
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.09.13 14:42
 *
 * @see com.pragmatix.app.controllers.PaymentController#onGoogleValidateInappPurchase(com.pragmatix.app.messages.client.GoogleVerifyInappPurchase, com.pragmatix.app.model.UserProfile)
 *
 */
@Command(10020)
public class VerifyPaymentResult extends IncomingPayment {

    public NeedMoneyResult.ResultEnum result;

    public int subscriptionExpiryTimeSecconds;

    public String transactionId;

    public VerifyPaymentResult() {
    }

    public VerifyPaymentResult(NeedMoneyResult.ResultEnum result, PaymentType paymentType, String item, int count, String sessionKey, String transactionId) {
        this.result = result;
        this.paymentType = paymentType;
        this.item = item;
        this.count = count;
        this.sessionKey = sessionKey;
        this.transactionId = transactionId;
    }

    @Override
    public String toString() {
        return "VerifyPaymentResult{" +
                "result=" + result +
                ", paymentType=" + paymentType +
                ", item=" + item +
                ", count=" + count +
                ", subscriptionExpiryTime=" + AppUtils.formatDateInSeconds(subscriptionExpiryTimeSecconds) +
                ", transactionId='" + transactionId + '\'' +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }

}
