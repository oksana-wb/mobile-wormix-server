package com.pragmatix.app.model.group;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.04.2014 11:27
 */
public class SoclanTeamMember extends TeamMember {

    public static final int SIZE = 1;

    public SoclanTeamMember() {
    }

    public SoclanTeamMember(byte[] buff) {
        int[] i = new int[]{0};
        if(buff.length == SIZE){
            active = readBoolean(buff, i);
        }
    }

    @Override
    public byte[] toBytea() {
        byte[] buff = new byte[SIZE];
        int i = 0;
        i = writeBoolean(active, buff, i);
        return buff;
    }

    public short getHat() {
        return 0;
    }

    public boolean setHat(short hat) {
        return false;
    }

    public short getKit() {
        return 0;
    }

    public boolean setKit(short kit) {
        return false;
    }

    @Override
    public String toString() {
        return "SoclanTeamMember{}";
    }
}
