package com.pragmatix.arena.mercenaries.messages;

import com.pragmatix.serialization.annotations.Structure;

import java.util.Arrays;
import java.util.List;

@Structure
public class MercenariesTeamMember {

    public byte id;
    public byte level;
    public byte race;
    public byte armor;
    public byte attack;
    public short hat;
    public short kit;
    public short skin;
    public boolean active = true;

    public BackpackItemShortStruct[] backpack;

    public MercenariesTeamMember() {
    }

    public MercenariesTeamMember(byte id, byte level, byte race, byte armor, byte attack, short hat, short kit) {
        this.id = id;
        this.level = level;
        this.race = race;
        this.armor = armor;
        this.attack = attack;
        this.hat = hat;
        this.kit = kit;
    }

    public void setBackpack(List<BackpackItemShortStruct> backpack) {
        this.backpack = backpack.toArray(new BackpackItemShortStruct[backpack.size()]);
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", active=" + active +
                ", level=" + level +
                ", race=" + race +
                ", skin=" + skin +
                ", armor=" + armor +
                ", attack=" + attack +
                ", hat=" + hat +
                ", kit=" + kit +
                ", backpack=" + Arrays.toString(backpack) +
                '}';
    }

    public byte getId() {
        return id;
    }
}
