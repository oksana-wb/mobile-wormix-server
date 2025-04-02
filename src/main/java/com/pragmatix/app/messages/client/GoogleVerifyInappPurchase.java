package com.pragmatix.app.messages.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 10.12.13 11:07
 * @see com.pragmatix.app.controllers.PaymentController#onGoogleValidateInappPurchase(GoogleVerifyInappPurchase, com.pragmatix.app.model.UserProfile)
 */
@Command(103)
public class GoogleVerifyInappPurchase extends SecuredCommand {

    public String productId;

    public boolean isSubscription;

    public String purchaseToken;

    public String sessionKey;

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "GoogleValidateInappPurchase{" +
                "productId='" + productId + '\'' +
                ", isSubscription=" + isSubscription +
                ", purchaseToken='" + purchaseToken + '\'' +
                ", sessionKey='" + sessionKey + '\'' +
                ", secureResult=" + secureResult +
                '}';
    }

}
