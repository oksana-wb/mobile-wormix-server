package com.pragmatix.app.messages.server;

import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.12.2014 15:35
 *
 * @see com.pragmatix.app.services.SpecialDealService#onGetSpecialDeal(com.pragmatix.app.messages.client.GetSpecialDeal, com.pragmatix.app.model.UserProfile)
 */
@Command(10108)
public class GetSpecialDealResponse {

    public short itemId;

    public byte rubyPrice;

    @Override
    public String toString() {
        return "GetSpecialDealResponse{" +
                "itemId=" + itemId +
                ", rubyPrice=" + rubyPrice +
                '}';
    }
}
