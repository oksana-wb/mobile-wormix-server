package com.pragmatix.intercom.structures;

import com.pragmatix.arena.coliseum.ColiseumDao;
import com.pragmatix.arena.coliseum.ColiseumEntity;
import com.pragmatix.arena.coliseum.GladiatorTeamMemberStructure;
import com.pragmatix.serialization.annotations.Structure;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.05.2017 14:04
 */
@Structure(nullable = true)
public class ProfileColiseumStructure {
    public boolean open;
    public int num;
    public int startSeries;
    public byte[] data;

    public ProfileColiseumStructure() {
    }

    public ProfileColiseumStructure(ColiseumEntity entity) {
        this.open = entity.open;
        this.num = entity.num;
        this.startSeries = entity.startSeries;
        this.data = ColiseumDao.serialize(entity);
    }

    public void merge(ColiseumEntity entity, ColiseumDao coliseumDao){
        entity.open = open;
        entity.num = num;
        entity.startSeries = startSeries;
        ColiseumDao.DataUnapply dataUnapply = new ColiseumDao.DataUnapply(coliseumDao, data).invoke();
        entity.win = dataUnapply.getWin();
        entity.defeat = dataUnapply.getDefeat();
        entity.draw = dataUnapply.getDraw();
        entity.candidats = dataUnapply.getCandidats();
        entity.team = dataUnapply.getTeam();

        entity.dirty = true;
    }

    @Override
    public String toString() {
        return "{" +
                "open=" + open +
                ", num=" + num +
                ", startSeries=" + startSeries +
//                ", data=" + Arrays.toString(data) +
                '}';
    }
}
