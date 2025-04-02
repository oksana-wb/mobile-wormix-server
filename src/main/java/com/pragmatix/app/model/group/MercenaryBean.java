package com.pragmatix.app.model.group;

import com.pragmatix.app.model.StuffHaving;
import com.pragmatix.app.settings.ItemRequirements;

import java.util.*;

/**
 * Характеристики наёмника для найма в команду
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.04.2014 14:27
 */
public class MercenaryBean extends ItemRequirements implements StuffHaving {

    public int id;

    public byte level;

    public byte raceId;

    public byte skinId;

    public byte armor;

    public byte attack;

    public short hatId;

    public short kitId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public byte getRaceId() {
        return raceId;
    }

    public void setRaceId(byte raceId) {
        this.raceId = raceId;
    }

    public byte getSkinId() {
        return skinId;
    }

    public void setSkinId(byte skinId) {
        this.skinId = skinId;
    }

    public byte getArmor() {
        return armor;
    }

    public void setArmor(byte armor) {
        this.armor = armor;
    }

    public byte getAttack() {
        return attack;
    }

    public void setAttack(byte attack) {
        this.attack = attack;
    }

    public short getHatId() {
        return hatId;
    }

    public void setHatId(short hatId) {
        this.hatId = hatId;
    }

    public short getKitId() {
        return kitId;
    }

    public void setKitId(short kitId) {
        this.kitId = kitId;
    }
}
