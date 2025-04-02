package com.pragmatix.app.messages.client.facebook;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.annotations.Command;

/**
 * Facebook: регистрация платежа
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.09.13 16:09
 * @see com.pragmatix.app.controllers.PaymentController#onFbRegisterPayment(FbRegisterPayment, com.pragmatix.app.model.UserProfile)
 */
@Command(101)
public class FbRegisterPayment extends SecuredCommand {

    /**
     * идентификатор платежа
     */
    public String itemId;

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
        return "FbRegisterPayment{" +
                "itemId='" + itemId + '\'' +
                ", sessionKey='" + sessionKey + '\'' +
                ", secureResult=" + secureResult +
                '}';
    }

}
