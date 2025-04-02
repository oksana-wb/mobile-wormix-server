package com.pragmatix.app.model;

import com.pragmatix.app.common.Race;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Параметры нового героя
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.03.2016 10:55
 */
public class ProfileInitParams {

    private short level = 1;
    private int money;
    private int realMoney;
    private int battlesCount = 5;
    private Race race = Race.BOXER;
    private short attack = 1;
    private short armor = 1;
    private byte renameActions;

    // конечное оружие выдаваемое при старте
    private Map<Integer, Integer> weapons = Collections.emptyMap();

    // бесконечное оружие выдаваемое при старте (всегда находится в рюкзаке, в базу не записывается)
    private Set<Integer> defaultEndlessWeapons;

    public short getLevel() {
        return level;
    }

    public void setLevel(short level) {
        this.level = level;
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

    public int getBattlesCount() {
        return battlesCount;
    }

    public void setBattlesCount(int battlesCount) {
        this.battlesCount = battlesCount;
    }

    public Race getRace() {
        return race;
    }

    public void setRace(Race race) {
        this.race = race;
    }

    public short getAttack() {
        return attack;
    }

    public void setAttack(short attack) {
        this.attack = attack;
    }

    public short getArmor() {
        return armor;
    }

    public void setArmor(short armor) {
        this.armor = armor;
    }

    public byte getRenameActions() {
        return renameActions;
    }

    public void setRenameActions(byte renameActions) {
        this.renameActions = renameActions;
    }

    public void setWeapons(Map<Integer, Integer> weapons) {
        this.weapons = weapons;
    }

    public Map<Integer, Integer> getWeapons() {
        return weapons;
    }

    public Set<Integer> getDefaultEndlessWeapons() {
        return defaultEndlessWeapons;
    }

    public void setDefaultEndlessWeapons(Set<Integer> defaultEndlessWeapons) {
        this.defaultEndlessWeapons = defaultEndlessWeapons;
    }

    public int defaultArmorForLevel(int level){
       return level;
    }

    public int defaultAttackForLevel(int level){
        return level;
    }
}
