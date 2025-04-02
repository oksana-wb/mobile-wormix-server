package com.pragmatix.app.messages.structures;

import com.pragmatix.app.domain.TrueSkillEntity;
import com.pragmatix.serialization.annotations.Structure;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 07.05.2014 10:53
 */
@Structure
public class TrueSkillStructure {
    /**
     * The statistical mean value of the rating (also known as μ). *
     */
    public double mean;

    /**
     * The standard deviation (the spread) of the rating. This is also known as σ. *
     */
    public double standardDeviation;

    /**
     * на результатах скольких боев основано значение мастерства
     */
    public int battles;

    public TrueSkillStructure() {
    }

    public TrueSkillStructure(TrueSkillEntity entity) {
        this.mean = entity.getMean();
        this.standardDeviation = entity.getStandardDeviation();
        this.battles = entity.getBattles();
    }

}
