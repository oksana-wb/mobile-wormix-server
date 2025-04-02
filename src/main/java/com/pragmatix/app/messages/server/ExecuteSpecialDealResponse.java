
package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.12.2014 15:35
 * @see com.pragmatix.app.services.SpecialDealService#onExecuteSpecialDeal(com.pragmatix.app.messages.client.ExecuteSpecialDeal, com.pragmatix.app.model.UserProfile)
 */
@Command(10109)
public class ExecuteSpecialDealResponse implements SecuredResponse {

    public ShopResultEnum result;

    public short weaponId;

    public byte rubyPrice;

    public String sessionKey;

    public ExecuteSpecialDealResponse() {
    }

    public ExecuteSpecialDealResponse(ShopResultEnum result, short weaponId, byte rubyPrice, String sessionKey) {
        this.result = result;
        this.weaponId = weaponId;
        this.rubyPrice = rubyPrice;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "ExecuteSpecialDealResponse{" +
                "result=" + result +
                ", weaponId=" + weaponId +
                ", rubyPrice=" + rubyPrice +
                '}';
    }

}
