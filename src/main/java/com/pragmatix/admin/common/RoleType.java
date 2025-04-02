package com.pragmatix.admin.common;

/**
 * User: denis
 * Date: 28.01.2011
 * Time: 14:24:18
 */
public enum RoleType {

    /**
    * роль супер админа
    */
    SUPER_ADMIN_ROLE(2, 5),;

    private byte type;

    /**
     * "чин" - чем выше тем главнее
     */
    private final int rank;

    RoleType(int type, int rank) {
        this.type = (byte) type;
        this.rank = (byte) rank;
    }

    public byte getType() {
        return type;
    }

    public int getRank() {
        return rank;
    }

    public static RoleType getRole(byte role) {
        for(RoleType roleType : RoleType.values()) {
            if(roleType.getType() == role) {
                return roleType;
            }
        }
        return null;
    }

    public static boolean isAvalable(byte role) {
        return getRole(role) != null;
    }
}
