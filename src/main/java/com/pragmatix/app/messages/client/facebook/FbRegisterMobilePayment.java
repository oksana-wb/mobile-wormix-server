package com.pragmatix.app.messages.client.facebook;

import com.pragmatix.app.common.MoneyType;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.annotations.Command;

/**
 * Facebook: регистрация платежа
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.09.13 16:09
 *
 *@see com.pragmatix.app.controllers.PaymentController#onFbRegisterMobilePayment(FbRegisterMobilePayment, com.pragmatix.app.model.UserProfile)
 *
 * https://developers.facebook.com/docs/howto/payments/mobilepricing
 */
@Command(105)
public class FbRegisterMobilePayment extends SecuredCommand {

    /**
     * идентификатор мобильного платежа
     */
    public String pricepointId;

    /**
     * валюта
     */
    public MoneyType moneyType;

    /**
     * количество
     */
    public int amount;

    /**
     * стоимость платежа в $
     */
    public float votes;

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
        return "FbRegisterMobilePayment{" +
                "pricepointId='" + pricepointId + '\'' +
                ", moneyType=" + moneyType +
                ", amount=" + amount +
                ", votes=" + votes +
                ", sessionKey='" + sessionKey + '\'' +
                ", secureResult=" + secureResult +
                '}';
    }
}
