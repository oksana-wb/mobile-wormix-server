package com.pragmatix.intercom.structures;

import com.pragmatix.arena.coliseum.ColiseumDao;
import com.pragmatix.arena.coliseum.ColiseumEntity;
import com.pragmatix.arena.mercenaries.MercenariesEntity;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.05.2017 14:04
 */
@Structure(nullable = true)
public class ProfileMercenariesStructure {

    public boolean open; // активна ли серия в данный момент
    public int num; // порядковый номер серии
    public byte win;
    public byte defeat;
    public byte draw;
    public byte[] team; //текущая команда наёмников
    public int total_win;
    public int total_defeat;
    public int total_draw;
    public int startSeries;// время начала серии (в секундах)

    public ProfileMercenariesStructure() {
    }

    public ProfileMercenariesStructure(MercenariesEntity entity) {
        this.open = entity.open;
        this.num = entity.num;
        this.win = entity.win;
        this.defeat = entity.defeat;
        this.draw = entity.draw;
        this.total_win = entity.total_win;
        this.total_defeat = entity.total_defeat;
        this.total_draw = entity.total_draw;
        this.team = entity.team;
        this.startSeries = entity.startSeries;
    }

    public void merge(MercenariesEntity entity) {
        entity.open = open;
        entity.num = num;
        entity.win = win;
        entity.defeat = defeat;
        entity.draw = draw;
        entity.total_win = total_win;
        entity.total_defeat = total_defeat;
        entity.total_draw = total_draw;
        entity.team = team;
        entity.startSeries = startSeries;

        entity.dirty = true;
    }

    @Override
    public String toString() {
        return "{" +
                "open=" + open +
                ", num=" + num +
                ", win=" + win +
                ", defeat=" + defeat +
                ", draw=" + draw +
                ", team=" + Arrays.toString(team) +
                ", total_win=" + total_win +
                ", total_defeat=" + total_defeat +
                ", total_draw=" + total_draw +
                ", startSeries=" + startSeries +
                '}';
    }
}
