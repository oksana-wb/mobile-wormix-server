package com.pragmatix.app.messages.client;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.08.2016 11:13
 *
 *  @see com.pragmatix.app.services.PromoService.PromoController#onActivatePromoKey(ActivatePromoKey, UserProfile)
 */
@Command(10)
public class ActivatePromoKey {

    public String key;


    @Override
    public String toString() {
        return "ActivatePromoKey{" +
                "key='" + key + '\'' +
                '}';
    }

}
