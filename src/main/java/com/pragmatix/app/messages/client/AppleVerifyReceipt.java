package com.pragmatix.app.messages.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;

import javax.xml.bind.DatatypeConverter;

/**
 * Для валидации платежа на apple.com
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.02.13 14:29
 * @see com.pragmatix.app.controllers.PaymentController#onAppleVerifyReceipt(AppleVerifyReceipt, com.pragmatix.app.model.UserProfile)
 */
@Command(7)
public class AppleVerifyReceipt extends SecuredCommand {

    public String productId;

    public byte[] receiptData;

    public boolean isSubscription;

    public String sessionKey;

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "AplVerifyReceipt{" +
                "productId=" + productId +
                ", isSubscription=" + isSubscription +
                ", receiptData='" + DatatypeConverter.printBase64Binary(receiptData) + '\'' +
                ", secureResult=" + secureResult +
                '}';
    }
}
