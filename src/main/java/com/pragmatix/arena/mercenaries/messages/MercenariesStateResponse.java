package com.pragmatix.arena.mercenaries.messages;

import com.pragmatix.arena.mercenaries.MercenariesErrorEnum;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

@Command(10126)
public class MercenariesStateResponse {

    public int num;
    public boolean open;
    public int win;
    public int defeat;
    public int draw;

    public int total_win;
    public int total_defeat;
    public int total_draw;

    public int error;

    public byte[] team;

    // сколько попыток сегодня ещё есть
    public int attemptsRemainToday;

    public MercenariesStateResponse() {
    }

    public MercenariesStateResponse(int num, boolean open
            , int win, int defeat, int draw, int total_win, int total_defeat, int total_draw
            , byte[] team, int attemptsRemainToday) {
        this.num = num;
        this.open = open;
        this.win = win;
        this.defeat = defeat;
        this.draw = draw;
        this.total_win = total_win;
        this.total_defeat = total_defeat;
        this.total_draw = total_draw;
        this.team = team;
        this.attemptsRemainToday = attemptsRemainToday;
    }

    public MercenariesStateResponse(int error, int attemptsRemainToday) {
        this.error = error;
        this.attemptsRemainToday = attemptsRemainToday;
    }

    @Override
    public String toString() {
        return "MercenariesStateResponse{" +
                "error=" + MercenariesErrorEnum.valueOf(error) +
                ", num=" + num +
                ", open=" + open +
                ", win=" + win +
                ", defeat=" + defeat +
                ", draw=" + draw +
                ", total_win=" + total_win +
                ", total_defeat=" + total_defeat +
                ", total_draw=" + total_draw +
                ", attemptsRemainToday=" + attemptsRemainToday +
                ", team=" + Arrays.toString(team) +
                '}';
    }
}
