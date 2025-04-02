package com.pragmatix.arena.coliseum.messages;

import com.pragmatix.arena.coliseum.GladiatorTeamMemberStructure;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

@Command(10110)
public class ColiseumStateResponse {

    public int num;
    public boolean open;
    public int win;
    public int defeat;
    public int draw;

    public int error;

    public GladiatorTeamMemberStructure[] teamCandidats;

    public GladiatorTeamMemberStructure[] team;

    // время досткпности арены в секундах
    public int openFrom;
    public int openTo;

    public ColiseumStateResponse() {
    }


    public ColiseumStateResponse(int num, boolean open, int win, int defeat, int draw, GladiatorTeamMemberStructure[] teamCandidats, GladiatorTeamMemberStructure[] team, int openFrom, int openTo) {
        this.num = num;
        this.open = open;
        this.win = win;
        this.defeat = defeat;
        this.draw = draw;
        this.teamCandidats = teamCandidats;
        this.team = team;
        this.openFrom = openFrom;
        this.openTo = openTo;
    }

    public ColiseumStateResponse(int error, int openFrom, int openTo) {
        this.error = error;
        this.openFrom = openFrom;
        this.openTo = openTo;
    }

    @Override
    public String toString() {
        return "ColiseumStateResponse{" +
                "error=" + error +
                ", num=" + num +
                ", open=" + open +
                ", win=" + win +
                ", defeat=" + defeat +
                ", draw=" + draw +
                ", open=(" + AppUtils.formatDateInSeconds(openFrom) + " - "+ AppUtils.formatDateInSeconds(openTo) +")"+
                ", teamCandidats=" + Arrays.toString(teamCandidats) +
                ", team=" + Arrays.toString(team) +
                '}';
    }
}
