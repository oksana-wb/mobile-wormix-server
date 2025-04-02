package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.messages.client.BuySelectRace;
import com.pragmatix.app.model.PurchaseResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

/**
 * Ответ на команду покупки расы
 *
 * @see com.pragmatix.app.controllers.ShopController#onBuySelectRace(BuySelectRace, UserProfile)
 */
@Command(10050)
public class BuySelectRaceResponse extends PurchaseResponse {

    public Race race;

    public byte skinId;

    public BuySelectRaceResponse() {
    }

    public BuySelectRaceResponse(PurchaseResult purchaseResult, Race race, byte skinId, String sessionKey) {
        super(purchaseResult, sessionKey);
        this.race = race;
        this.skinId = skinId;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "BuySelectRaceResult{" +
                "result=" + result +
                ", race=" + race +
                ", skinId=" + skinId +
                ", cost=" + cost +
                '}';
    }
}
