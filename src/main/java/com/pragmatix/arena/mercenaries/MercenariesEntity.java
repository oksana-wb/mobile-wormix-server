package com.pragmatix.arena.mercenaries;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

import static com.pragmatix.common.utils.AppUtils.*;

public class MercenariesEntity {

    public static final int TEAM_SIZE = 3;

    public int profileId;
    public boolean open; // активна ли серия в данный момент
    public int num; // порядковый номер серии
    public byte win;
    public byte defeat;
    public byte draw;
    public byte[] team; //текущая команда наёмников
    public int total_win;
    public int total_defeat;
    public int total_draw;
    public volatile boolean dirty;
    public volatile boolean newly;
    public int startSeries;// время начала серии (в секундах)

    public MercenariesEntity(int profileId, boolean open, int num,
                             byte win, byte defeat, byte draw, int total_win, int total_defeat, int total_draw,
                             byte[] team, boolean dirty, boolean newly, int startSeries) {
        this.profileId = profileId;
        this.open = open;
        this.num = num;
        this.win = win;
        this.defeat = defeat;
        this.draw = draw;
        this.total_win = total_win;
        this.total_defeat = total_defeat;
        this.total_draw = total_draw;
        this.team = team;
        this.dirty = dirty;
        this.newly = newly;
        this.startSeries = startSeries;
    }

    public boolean isTeamFull() {
        return ArrayUtils.indexOf(team, (byte) 0) == -1;
    }

    public void touch() {
        dirty = true;
    }

    @Override
    public String toString() {
        return "MercenariesEntity{" +
                "profileId=" + profileId +
                ", open=" + open +
                ", num=" + num +
                ", win=" + win +
                ", defeat=" + defeat +
                ", draw=" + draw +
                ", total_win=" + total_win +
                ", total_defeat=" + total_defeat +
                ", total_draw=" + total_draw +
                ", team=" + Arrays.toString(team) +
                ", dirty=" + dirty +
                ", newly=" + newly +
                ", startSeries=" + formatDateInSeconds(startSeries) +
                '}';
    }
}
