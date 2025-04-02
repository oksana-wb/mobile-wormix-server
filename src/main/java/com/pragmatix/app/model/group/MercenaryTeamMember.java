package com.pragmatix.app.model.group;

import com.pragmatix.app.model.UserProfile;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.04.2014 11:27
 */
public class MercenaryTeamMember extends TeamMember {

    public static final int SIZE = 5;

    public short hat;

    public short kit;

    public MercenaryTeamMember() {
    }

    public MercenaryTeamMember(byte[] buff) {
        int[] i = new int[]{0};
        hat = readShort(buff, i);
        kit = readShort(buff, i);
        if(buff.length == SIZE){
            active = readBoolean(buff, i);
        }
    }

    public byte[] toBytea() {
        byte[] buff = new byte[SIZE];
        int i = 0;
        i = writeShort(hat, buff, i);
        i = writeShort(kit, buff, i);
        i = writeBoolean(active, buff, i);
        return buff;
    }

    public short getHat() {
        return hat;
    }

    public boolean setHat(short hat) {
        this.hat = hat;
        return true;
    }

    public short getKit() {
        return kit;
    }

    public boolean setKit(short kit) {
        this.kit = kit;
        return true;
    }

    @Override
    public boolean canBeRenamed() {
        return true;
    }

    @Override
    public String toString() {
        return "MercenaryTeamMember{" +
                "hat=" + hat +
                ", kit=" + kit +
                '}';
    }

}
