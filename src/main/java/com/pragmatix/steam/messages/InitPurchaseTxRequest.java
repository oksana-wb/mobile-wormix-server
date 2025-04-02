package com.pragmatix.steam.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.07.2017 10:53
 *
 * @see com.pragmatix.steam.SteamController#onInitPurchaseTxRequest(InitPurchaseTxRequest, UserProfile)
 */
@Command(6001)
public class InitPurchaseTxRequest {

    public String productCode;

    @Override
    public String toString() {
        return "InitPurchaseTxRequest{" +
                "productCode='" + productCode + '\'' +
                '}';
    }

}
