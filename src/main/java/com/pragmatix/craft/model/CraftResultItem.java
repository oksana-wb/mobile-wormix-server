package com.pragmatix.craft.model;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 12.12.13 14:58
 */
public class CraftResultItem {

    public boolean seasonStuff;

    public int weaponCount;

    public int moneyCount;

    public int rubyCount;

    public int medalCount;

    public int mutagenCount;

    public int experience;

    public int battles = 0;

    public int bossToken = 0;

    public int wagerToken = 0;

    public int getWeaponCount() {
        return weaponCount;
    }

    public void setWeaponCount(int weaponCount) {
        this.weaponCount = weaponCount;
    }

    public int getRubyCount() {
        return rubyCount;
    }

    public void setRubyCount(int rubyCount) {
        this.rubyCount = rubyCount;
    }

    public boolean isSeasonStuff() {
        return seasonStuff;
    }

    public void setSeasonStuff(boolean seasonStuff) {
        this.seasonStuff = seasonStuff;
    }

    public int getMedalCount() {
        return medalCount;
    }

    public void setMedalCount(int medalCount) {
        this.medalCount = medalCount;
    }

    public int getMutagenCount() {
        return mutagenCount;
    }

    public void setMutagenCount(int mutagenCount) {
        this.mutagenCount = mutagenCount;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public int getBattles() {
        return battles;
    }

    public void setBattles(int battles) {
        this.battles = battles;
    }

    public int getBossToken() {
        return bossToken;
    }

    public void setBossToken(int bossToken) {
        this.bossToken = bossToken;
    }

    public int getWagerToken() {
        return wagerToken;
    }

    public void setWagerToken(int wagerToken) {
        this.wagerToken = wagerToken;
    }

    public int getMoneyCount() {
        return moneyCount;
    }

    public void setMoneyCount(int moneyCount) {
        this.moneyCount = moneyCount;
    }
}
