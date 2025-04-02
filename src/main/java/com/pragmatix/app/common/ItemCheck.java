package com.pragmatix.app.common;

import com.pragmatix.app.settings.IItemRequirements;
import com.pragmatix.app.settings.ItemRequirements;

/**
 * Класс для проверки типа предмета
 * User: denis, eugene
 * Date: 29.09.2010
 * Time: 18:29:14
 */
public class ItemCheck {

    public static final int WEAPON_START_INDEX = 0;

    public static final int STUFF_START_INDEX = 1000;

    public static final int CRAFT_HAT_START_INDEX = 1500;
    public static final int CRAFT_HAT_END_INDEX = 2000;

    public static final int CRAFT_KIT_START_INDEX = 2500;
    public static final int CRAFT_KIT_END_INDEX = 3000;

    public static final int EMPTY_PRICE = 99999;

    public static boolean isWeapon(int id) {
        return id >= WEAPON_START_INDEX && id < STUFF_START_INDEX;
    }

    public static boolean isStuff(int id) {
        return id >= STUFF_START_INDEX;
    }

    public static boolean isCraftStuff(int id) {
        return (id > CRAFT_HAT_START_INDEX && id < CRAFT_HAT_END_INDEX)
                || (id > CRAFT_KIT_START_INDEX && id < CRAFT_KIT_END_INDEX);
    }

    public static boolean hasPrice(int price) {
        return price > 0 && price < EMPTY_PRICE;
    }

    public static boolean hasPrice(IItemRequirements item) {
        return hasPrice(item.needMoney());
    }

    public static boolean hasRealPrice(IItemRequirements item) {
        return hasPrice(item.needRealMoney());
    }

}
