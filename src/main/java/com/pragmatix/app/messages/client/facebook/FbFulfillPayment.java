package com.pragmatix.app.messages.client.facebook;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.09.13 17:36
 *
 * @see com.pragmatix.app.controllers.PaymentController#onFbFulfillPayment(FbFulfillPayment, com.pragmatix.app.model.UserProfile)
 */
@Command(100)
public class FbFulfillPayment extends SecuredCommand {

    /**
     * id платежной транзакции
     */
    public int paymentId;

    public String signedRequest;

    /**
     * ключ текущей сессии
     */
    public String sessionKey;

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "FbFulfillPayment{" +
                "paymentId=" + paymentId +
                ", signedRequest='" + signedRequest + '\'' +
                ", sessionKey='" + sessionKey + '\'' +
                ", secureResult=" + secureResult +
                '}';
    }
}
