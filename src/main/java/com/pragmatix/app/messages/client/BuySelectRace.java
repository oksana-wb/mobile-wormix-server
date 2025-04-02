package com.pragmatix.app.messages.client;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.messages.server.BuySelectRaceResponse;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

/**
 * Команда покупки расы
 *
 * @see com.pragmatix.app.controllers.ShopController#onBuySelectRace(BuySelectRace, UserProfile)
 * @see BuySelectRaceResponse
 */
@Command(50)
public class BuySelectRace {

    public Race race;

    public byte skinId;

    public BuySelectRace() {
    }

    public BuySelectRace(Race race, byte skinId) {
        this.race = race;
        this.skinId = skinId;
    }

    @Override
    public String toString() {
        return "BuySelectRace{" +
                "race=" + race +
                ", skinId=" + skinId +
                '}';
    }
}
