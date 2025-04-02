package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.messages.client.BuyRace;
import com.pragmatix.app.model.PurchaseResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.serialization.annotations.Command;

/**
 * Ответ на команду покупки расы
 *
 * @see com.pragmatix.app.controllers.ShopController#onBuyRace(BuyRace, UserProfile)
 */
@Command(10028)
public class BuyRaceResponse extends PurchaseResponse {

    public Race race;

    public short races;

    public BuyRaceResponse() {
    }

    public BuyRaceResponse(PurchaseResult purchaseResult, Race race, short races, String sessionKey) {
        super(purchaseResult, sessionKey);
        this.race = race;
        this.races = races;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "BuyRaceResult{" +
                "result=" + result +
                ", race=" + race +
                (AppParams.IS_NOT_MOBILE() ? ", races=" + Race.toList(races) : ", race=" + Race.valueOf(races)) +
                ", cost=" + cost +
                '}';
    }

}
