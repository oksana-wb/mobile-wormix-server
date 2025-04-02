package com.pragmatix.app.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pragmatix.app.model.AnyMoneyAddition;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.02.12 12:08
 */
public class BossBattleWinAward implements AnyMoneyAddition {

    private int money;

    private int realMoney;

    private int experience;

    private List<AwardBackpackItem> awardItems = new ArrayList<AwardBackpackItem>();

    /**
     * вероятности получения "редкой" награды
     */
    private int[][] rareAwardMassMap;

    @JsonIgnore
    private String rareAwardMass;

    @JsonIgnore
    private String awardItemsStr="";

    public void setAwardItemsStr(String awardItemsAsString) {
        this.awardItemsStr = awardItemsAsString;
    }

    public String getAwardItemsStr() {
        return awardItemsStr;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getRealMoney() {
        return realMoney;
    }

    public void setRealMoney(int realMoney) {
        this.realMoney = realMoney;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public List<AwardBackpackItem> getAwardItems() {
        return awardItems;
    }

    public void setAwardItems(List<AwardBackpackItem> awardItems) {
        this.awardItems = awardItems;
    }

    public int[][] getRareAwardMassMap() {
        return rareAwardMassMap;
    }

    public void setRareAwardMassMap(int[][] rareAwardMassMap) {
        this.rareAwardMassMap = rareAwardMassMap;
    }

    public String getRareAwardMass() {
        return rareAwardMass;
    }

    public void setRareAwardMass(String rareAwardMass) {
        this.rareAwardMass = rareAwardMass;
    }
}
