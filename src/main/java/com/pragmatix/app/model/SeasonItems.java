package com.pragmatix.app.model;

import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 01.08.2017 9:50
 */
public class SeasonItems {

    public List<SeasonWeaponItem> seasonWeapons;

    public List<Integer> seasonStuff;

    public List<SeasonWeaponItem> getSeasonWeapons() {
        return seasonWeapons;
    }

    public void setSeasonWeapons(List<SeasonWeaponItem> seasonWeapons) {
        this.seasonWeapons = seasonWeapons;
    }

    public List<Integer> getSeasonStuff() {
        return seasonStuff;
    }

    public void setSeasonStuff(List<Integer> seasonStuff) {
        this.seasonStuff = seasonStuff;
    }
}
