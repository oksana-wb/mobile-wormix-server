package com.pragmatix.app.model;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.05.2016 15:28
 */
public class SeasonWeaponItem {

    public final int weaponId;

    public double clanAwardParam1;

    public double clanAwardParam2;

    public SeasonWeaponItem(int weaponId) {
        this.weaponId = weaponId;
    }

    public void setClanAwardParam1(double clanAwardParam1) {
        this.clanAwardParam1 = clanAwardParam1;
    }

    public void setClanAwardParam2(double clanAwardParam2) {
        this.clanAwardParam2 = clanAwardParam2;
    }

    @Override
    public String toString() {
        return "{" +
                "weaponId=" + weaponId +
                ", clanAwardParam1=" + clanAwardParam1 +
                ", clanAwardParam2=" + clanAwardParam2 +
                '}';
    }
}
