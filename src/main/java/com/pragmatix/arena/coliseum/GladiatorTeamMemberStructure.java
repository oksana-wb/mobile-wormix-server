package com.pragmatix.arena.coliseum;

import com.pragmatix.serialization.annotations.Structure;

@Structure
public class GladiatorTeamMemberStructure {

    public byte race;
    public byte armor;
    public byte attack;
    public short hat;
    public short kit;

    public GladiatorTeamMemberStructure() {
    }

    public GladiatorTeamMemberStructure(byte race, byte armor, byte attack, short hat, short kit) {
        this.race = race;
        this.armor = armor;
        this.attack = attack;
        this.hat = hat;
        this.kit = kit;
    }

    @Override
    public String toString() {
        return "{" +
                "race=" + race +
                ", armor=" + armor +
                ", attack=" + attack +
                ", hat=" + hat +
                ", kit=" + kit +
                '}';
    }
}
