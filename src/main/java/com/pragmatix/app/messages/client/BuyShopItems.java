package com.pragmatix.app.messages.client;

import com.pragmatix.app.messages.structures.ShopItemStructure;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * команда пресылается с клиента при покупке вещей из магазина
 * <p>
 * User: denis
 * Date: 29.11.2009
 * Time: 21:52:25
 *
 * @see com.pragmatix.app.controllers.ShopController#onBuyShopItems(BuyShopItems, com.pragmatix.app.model.UserProfile)
 */
@Command(3)
public class BuyShopItems {

    /**
     * массив оружия покупка которого производится
     */
    public ShopItemStructure[] items;

    public BuyShopItems() {
    }

    public BuyShopItems(ShopItemStructure... items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "BuyShopItems{" +
                "items=" + (items == null ? null : Arrays.asList(items)) +
                '}';
    }
}
