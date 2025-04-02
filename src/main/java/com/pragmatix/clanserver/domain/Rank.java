package com.pragmatix.clanserver.domain;

import com.pragmatix.serialization.annotations.EnumKey;

/**
 * Author: Vladimir
 * Date: 04.04.13 9:39
 */
public enum Rank {
    LEADER(1, 999, 20, Permissions.ALL, Integer.MAX_VALUE),
    OFFICER(2, 99, 20, Permissions.INVITE | Permissions.EXPEL | Permissions.EXPAND | Permissions.POST_NEWS, 3),
    SOLDIER(3, 0, 0, 0, 0);

    private static final Rank[] seniors = {LEADER, LEADER, OFFICER};
    private static final Rank[] subordinates = {OFFICER, SOLDIER, SOLDIER};

    public final int code;
    private final int power;
    public final int inviteLimit;
    private final int permissions;
    private final int maxExpelByDay;

    Rank(int code, int power, int inviteLimit, int permissions, int maxExpelByDay) {
        this.code = code;
        this.power = power;
        this.inviteLimit = inviteLimit;
        this.permissions = permissions;
        this.maxExpelByDay = maxExpelByDay;
    }

    @EnumKey
    public int getCode() {
        return code;
    }

    public static Rank valueOf(int code) {
        for(Rank value : values()) {
            if(value.code == code) {
                return value;
            }
        }

        throw new IllegalArgumentException("Код не найден " + code);
    }

    public boolean canInvite() {
        return isGranted(Permissions.INVITE);
    }

    public boolean canExpel() {
        return isGranted(Permissions.EXPEL);
    }

    public boolean canPromoteInRank() {
        return isGranted(Permissions.PROMOTE_IN_RANK);
    }

    public boolean canLowerInRank() {
        return isGranted(Permissions.LOWER_IN_RANK);
    }

    public boolean canExpand() {
        return isGranted(Permissions.EXPAND);
    }

    public boolean canPostNews() {
        return isGranted(Permissions.POST_NEWS);
    }

    public boolean canEdit() {
        return isGranted(Permissions.EDIT);
    }

    private boolean isGranted(int permission) {
        return (permissions & permission) != 0;
    }

    public boolean isHigherThan(Rank other) {
        return power > other.power;
    }

    public Rank upper() {
        return seniors[ordinal()];
    }

    public Rank lower() {
        return subordinates[ordinal()];
    }

    public int getMaxExpelByDay() {
        return maxExpelByDay;
    }
}
