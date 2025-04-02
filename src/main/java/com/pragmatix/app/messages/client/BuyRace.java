package com.pragmatix.app.messages.client;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.app.messages.server.BuyRaceResponse;
import com.pragmatix.serialization.annotations.Command;

/**
 * Команда покупки расы
 *
 * @see com.pragmatix.app.controllers.ShopController#onBuyRace(BuyRace, com.pragmatix.app.model.UserProfile)
 * @see BuyRaceResponse
 */
@Command(36)
public class BuyRace {

    public Race race;

    public MoneyType moneyType;

    public BuyRace() {
    }

    public BuyRace(Race race, MoneyType moneyType) {
        this.race = race;
        this.moneyType = moneyType;
    }

    @Override
    public String toString() {
        return "BuyRace{" +
                "raceId=" + race +
                ", moneyType=" + moneyType +
                '}';
    }
}
