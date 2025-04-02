package com.pragmatix.intercom.structures;

import com.pragmatix.app.domain.TrueSkillEntity;
import com.pragmatix.serialization.annotations.Structure;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.05.2017 13:59
 */
@Structure
public class ProfileTrueSkillStructure {

    public double mean;
    public double standardDeviation;
    public int battles;

    public ProfileTrueSkillStructure() {
    }

    public ProfileTrueSkillStructure(TrueSkillEntity trueSkillEntity) {
        mean = trueSkillEntity.getMean();
        standardDeviation = trueSkillEntity.getStandardDeviation();
        battles = trueSkillEntity.getBattles();
    }

    public void merge(TrueSkillEntity entity){
        entity.setMean(mean);
        entity.setStandardDeviation(standardDeviation);
        entity.setBattles(battles);
        entity.setDirty(true);
    }

    @Override
    public String toString() {
        return "{" +
                "mean=" + mean +
                ", standardDeviation=" + standardDeviation +
                ", battles=" + battles +
                '}';
    }
}
