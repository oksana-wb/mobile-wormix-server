package com.pragmatix.app.model;

import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.serialization.annotations.Serialize;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Arrays;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_SHORT_ARRAY;

/**
 * Арендованное оружие
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 14.04.2016 9:07
 */
@Structure(nullable = true)
public class RentedItems {

    // время когда истекает аренда (в секундах)
    public int activeUntil;

    // оружие в аренде, если куплен VIP
    @Serialize(ifExpr = "self.activeUntil * 1000L > System.currentTimeMillis()")
    public short[] weapons;

    // если VIP-а нет
    @Serialize(ifExpr = "self.activeUntil * 1000L  <= System.currentTimeMillis()")
    public short[] emptyWeapons = EMPTY_SHORT_ARRAY;

    // предметы в аренде, если куплен VIP
    @Serialize(ifExpr = "self.activeUntil * 1000L > System.currentTimeMillis()")
    public short[] stuff;

    // если VIP-а нет
    @Serialize(ifExpr = "self.activeUntil * 1000L  <= System.currentTimeMillis()")
    public short[] emptyStuff = EMPTY_SHORT_ARRAY;

    public RentedItems() {
    }

    public RentedItems(int activeUntil, short[] weapons, short[] stuff) {
        this.activeUntil = activeUntil;
        this.weapons = weapons;
        this.stuff = stuff;
    }

    public short[] getWeapons() {
        return activeUntil * 1000L > System.currentTimeMillis() ? weapons : EMPTY_SHORT_ARRAY;
    }

    public short[] getStuff() {
        return activeUntil * 1000L > System.currentTimeMillis() ? stuff : EMPTY_SHORT_ARRAY;
    }

    public boolean isEmpty(){
        return activeUntil == 0;
    }

    @Override
    public String toString() {
        return "RentedItems{" +
                "activeUntil=" + AppUtils.formatDateInSeconds(activeUntil) +
                ", weapons=" + Arrays.toString(getWeapons()) +
                ", stuff=" + Arrays.toString(getStuff()) +
                '}';
    }

}
