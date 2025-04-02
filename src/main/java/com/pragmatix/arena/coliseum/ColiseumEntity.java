package com.pragmatix.arena.coliseum;

import java.util.Arrays;

public class ColiseumEntity {

    public int profileId;
    public boolean open; // активна ли серия в данный момент
    public int num; // порядковый номер серии
    public byte win;
    public byte defeat;
    public byte draw;
    public GladiatorTeamMemberStructure[] candidats;//текущий набор на выбор очередного участника
    public GladiatorTeamMemberStructure[] team;
    public volatile boolean dirty;
    public volatile boolean newly;
    public int startSeries;// время начала серии (в секундах)

    public ColiseumEntity(int profileId, boolean open, int num, byte win, byte defeat, byte draw, GladiatorTeamMemberStructure[] candidats, GladiatorTeamMemberStructure[] team, boolean dirty, boolean newly, int startSeries) {
        this.profileId = profileId;
        this.open = open;
        this.num = num;
        this.win = win;
        this.defeat = defeat;
        this.draw = draw;
        this.candidats = candidats;
        this.team = team;
        this.dirty = dirty;
        this.newly = newly;
        this.startSeries = startSeries;
    }

    public boolean isTeamFull() {
        for(GladiatorTeamMemberStructure member : team) {
            if(member == null)
                return false;
        }
        return true;
    }

    public void touch() {
        dirty = true;
    }

    @Override
    public String toString() {
        return "ColiseumEntity{" +
                "profileId=" + profileId +
                ", open=" + open +
                ", num=" + num +
                ", win=" + win +
                ", defeat=" + defeat +
                ", draw=" + draw +
                ", candidats=" + Arrays.toString(candidats) +
                ", team=" + Arrays.toString(team) +
                ", dirty=" + dirty +
                ", newly=" + newly +
                ", startSeries=" + startSeries +
                '}';
    }
}
