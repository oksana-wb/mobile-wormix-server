package com.pragmatix.app.model.group;

import com.pragmatix.app.common.TeamMemberType;
import com.pragmatix.app.model.UserProfile;
import io.vavr.Function2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Null;
import java.util.function.Function;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.04.2014 10:55
 */
public abstract class TeamMember {

    private static final Logger log = LoggerFactory.getLogger(TeamMember.class);

    public abstract byte[] toBytea();

    protected boolean active = true;

    protected String name;

    @Null
    public static TeamMember newTeamMember(TeamMemberType teamMemberType, UserProfile profile, Function2<UserProfile, Byte, Byte> skinProducer) {
        switch (teamMemberType) {
            case Friend:
                return new FriendTeamMember(profile, skinProducer);
            case SoclanMember:
                return new SoclanTeamMember();
            case OtherClanMember:
                return new SoclanTeamMember();
            case Merchenary:
                return new MercenaryTeamMember();
            default:
                return null;
        }
    }

    @Null
    public static TeamMember newTeamMember(byte[] buff) {
        if(buff == null) {
            return null;
        } else if(buff.length == SoclanTeamMember.SIZE || buff.length == SoclanTeamMember.SIZE - 1) {
            return new SoclanTeamMember(buff);
        } else if(buff.length == FriendTeamMember.SIZE || buff.length == FriendTeamMember.SIZE - 1 || buff.length == FriendTeamMember.SIZE - 2) {
            return new FriendTeamMember(buff);
        } else if(buff.length == MercenaryTeamMember.SIZE || buff.length == MercenaryTeamMember.SIZE - 1) {
            return new MercenaryTeamMember(buff);
        }
        log.error(String.format("ошибка создания подкласса TeamMember! Не верный размер массива [%s]", buff.length));
        return null;
    }

    public int writeInt(int value, byte[] buff, int i) {
        buff[i++] = (byte) ((value >>> 24) & 0xFF);
        buff[i++] = (byte) ((value >>> 16) & 0xFF);
        buff[i++] = (byte) ((value >>> 8) & 0xFF);
        buff[i++] = (byte) ((value) & 0xFF);
        return i;
    }

    public int readInt(byte[] buff, int[] i) {
        int ch1 = buff[i[0]++] & 0xFF;
        int ch2 = buff[i[0]++] & 0xFF;
        int ch3 = buff[i[0]++] & 0xFF;
        int ch4 = buff[i[0]++] & 0xFF;
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
    }

    public int writeShort(short value, byte[] buff, int i) {
        buff[i++] = (byte) ((value >>> 8) & 0xFF);
        buff[i++] = (byte) ((value) & 0xFF);
        return i;
    }

    public short readShort(byte[] buff, int[] i) {
        int ch1 = buff[i[0]++] & 0xFF;
        int ch2 = buff[i[0]++] & 0xFF;
        return (short) ((ch1 << 8) + (ch2));
    }

    public int writeByte(byte value, byte[] buff, int i) {
        buff[i++] = (byte) ((value) & 0xFF);
        return i;
    }

    public byte readByte(byte[] buff, int[] i) {
        return (byte) (buff[i[0]++] & 0xFF);
    }

    public int writeBoolean(boolean value, byte[] buff, int i) {
        return writeByte((byte) (value ? 1 : 0), buff, i);
    }

    public boolean readBoolean(byte[] buff, int[] i) {
        return readByte(buff, i) != 0;
    }

    public abstract short getHat();

    /**
     * Можно ли менять шапку
     */
    public abstract boolean setHat(short hat);

    public abstract short getKit();

    /**
     * Можно ли менять артефакт
     */
    public abstract boolean setKit(short kit);

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean canBeRenamed() {
        return false;
    }
}
