package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.12.2014 15:35
 *
 * @see com.pragmatix.app.services.SpecialDealService#onGetSpecialDeal(GetSpecialDeal, com.pragmatix.app.model.UserProfile)
 */
@Command(108)
public class GetSpecialDeal {

    @Override
    public String toString() {
        return "GetSpecialDeal{}";
    }

}
