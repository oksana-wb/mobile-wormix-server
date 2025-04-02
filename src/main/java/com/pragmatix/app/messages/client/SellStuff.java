package com.pragmatix.app.messages.client;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * Команда продажи шапок и артефактов
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.11.2016 10:59
 * @see com.pragmatix.app.controllers.ProfileController#onSellStuff(SellStuff, UserProfile)
 */
@Command(137)
public class SellStuff {

    public int[] itemsToSell;

    public SellStuff() {
    }

    public SellStuff(int... itemsToSell) {
        this.itemsToSell = itemsToSell;
    }

    @Override
    public String toString() {
        return "SellStuff{" +
                "itemsToSell=" + Arrays.toString(itemsToSell) +
                '}';
    }

}
