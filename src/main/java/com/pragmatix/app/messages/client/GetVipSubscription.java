package com.pragmatix.app.messages.client;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.06.2016 11:16
 * @see com.pragmatix.app.controllers.PaymentController#onGetVipSubscription(GetVipSubscription, UserProfile)
 */
@Command(140)
public class GetVipSubscription {

    public int vipSubscriptionId;

    @Override
    public String toString() {
        return "GetVipSubscription{" +
                "vipSubscriptionId=" + vipSubscriptionId +
                '}';
    }

}
