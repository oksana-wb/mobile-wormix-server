package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.client.BuySkin;
import com.pragmatix.app.model.PurchaseResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * Ответ на команду покупки скина для расы
 *
 * @see com.pragmatix.app.controllers.ShopController#onBuySkin(BuySkin, UserProfile)
 */
@Command(10029)
public class BuySkinResponse extends PurchaseResponse {

    public byte skinId;

    public byte[] skins;

    public BuySkinResponse() {
    }

    public BuySkinResponse(PurchaseResult purchaseResult, byte skinId, byte[] skins, String sessionKey) {
        super(purchaseResult, sessionKey);
        this.skinId = skinId;
        this.skins = skins;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "BuySkinResult{" +
                "result=" + result +
                ", skinId=" + skinId +
                ", skins=" + Arrays.toString(skins) +
                ", cost=" + cost +
                '}';
    }

}
