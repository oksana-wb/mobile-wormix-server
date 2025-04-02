package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * команда на покупку боя
 *
 * @author denis
 *
 * @see com.pragmatix.app.controllers.ShopController#onBuyBattle(BuyBattle, com.pragmatix.app.model.UserProfile)
 */
@Command(11)
public class BuyBattle {

    public static final int BULK_BATTLES_COUNT = 5;

    /**
     * тип денег
     */
    public int moneyType;

    /**
     * купить 5-ть боёв оптом
     */
    public boolean bulk;

    public BuyBattle() {
    }

    @Override
    public String toString() {
        return "BuyBattle{" +
                "moneyType=" + moneyType +
                ", bulk=" + bulk +
                '}';
    }

}
