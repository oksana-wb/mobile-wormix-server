package com.pragmatix.clanserver.common;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.01.2015 17:29
 */
public enum ClanActionEnum {
    ADMIN_LOWER(-7),
    ADMIN_PROMOTE(-6),
    ADMIN_EXPELL(-4),
    ADMIN_ADD(-3),
    ADMIN_UPDATE(-1),
    CREATE(1),
    DELETE(2),
    JOIN(3),
    EXPELL(4),
    QUIT(5),
    PROMOTE(6),
    LOWER(7),
    EXPAND(8),
    RENAME(9),
    CHANGE_DESCRIPTION(10),
    CHANGE_EMBLEM(11),
    CHANGE_CLOSE_STATE(12),
    DONATE(14),
    SET_MEDAL_PRICE(15),
    CASH_MEDALS(16, false),
    SET_EXPEL_PERMIT(17),
    SET_MUTE_MODE(18),
    ;

    public final int type;
    public final boolean logable;

    ClanActionEnum(int type) {
        this.type = type;
        this.logable = true;
    }

    ClanActionEnum(int type, boolean logable) {
        this.type = type;
        this.logable = logable;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }

    public static  ClanActionEnum valueOf(int type){
        for(ClanActionEnum item : values()) {
            if(item.type == type)
                return  item;
        }
        return null;
    }
}
