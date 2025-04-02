package com.pragmatix.app.messages.client;

import com.pragmatix.app.messages.server.BuySkinResponse;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.serialization.annotations.Command;

/**
 * Команда покупки расы
 *
 * @see com.pragmatix.app.controllers.ShopController#onBuySkin(BuySkin, UserProfile)
 * @see BuySkinResponse
 */
@Command(29)
public class BuySkin {

    public byte skinId;

    public MoneyType moneyType;

    public BuySkin() {
    }

    public BuySkin(int skinId, MoneyType moneyType) {
        this.skinId = (byte) skinId;
        this.moneyType = moneyType;
    }

    @Override
    public String toString() {
        return "BuySkin{" +
                "skinId=" + skinId +
                ", moneyType=" + moneyType +
                '}';
    }
}
