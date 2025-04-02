package com.pragmatix.app.model.group;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.RaceService;
import io.vavr.Function2;

import java.util.function.Function;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.04.2014 11:27
 */
public class FriendTeamMember extends MercenaryTeamMember {

    public static final int SIZE = 9;

    public final byte raceId;

    public final byte armor;

    public final byte attack;

    public final byte skinId;

    public FriendTeamMember(byte[] buff) {
        int[] i = new int[]{0};
        raceId = readByte(buff, i);
        armor = readByte(buff, i);
        attack = readByte(buff, i);
        hat = readShort(buff, i);
        kit = readShort(buff, i);
        if(buff.length >= 8) {
            active = readBoolean(buff, i);
        }
        skinId = buff.length == 9 ? readByte(buff, i) : 0;
    }

    public FriendTeamMember(FriendTeamMember member, byte attack, byte armor) {
        // TeamMember
        this.active = member.active;
        this.name = member.name;
        // MercenaryTeamMember
        this.hat = member.hat;
        this.kit = member.kit;
        // FriendTeamMember
        this.raceId = member.raceId;
        this.skinId = member.skinId;

        this.armor = armor;
        this.attack = attack;
    }

    public FriendTeamMember(UserProfile profile, Function2<UserProfile, Byte, Byte> skinProducer) {
        raceId = RaceService.getRaceExceptExclusive(profile);
        skinId = skinProducer.apply(profile, raceId);
        armor = (byte) profile.getArmor();
        attack = (byte) profile.getAttack();
        name = profile.getName();
    }

    @Override
    public boolean canBeRenamed() {
        return false;
    }

    public byte[] toBytea() {
        byte[] buff = new byte[SIZE];
        int i = 0;
        i = writeByte(raceId, buff, i);
        i = writeByte(armor, buff, i);
        i = writeByte(attack, buff, i);
        i = writeShort(hat, buff, i);
        i = writeShort(kit, buff, i);
        i = writeBoolean(active, buff, i);
        i = writeByte(skinId, buff, i);
        return buff;
    }

    @Override
    public String toString() {
        return "FriendTeamMember{" +
                Race.valueOf(raceId) +
                ", skin=" + skinId +
                ", armor=" + armor +
                ", attack=" + attack +
                ", hat=" + hat +
                ", kit=" + kit +
                '}';
    }
}
