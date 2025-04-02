package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.Identifiable;

import javax.persistence.Basic;
import javax.persistence.Id;
import javax.persistence.Transient;

/**
 * Для хранеия мастерства игрока в БД
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.04.13 11:09
 */
public class TrueSkillEntity implements Identifiable<Long> {

    @Id
    private long profileId;

    /**
     * The statistical mean value of the rating (also known as μ). *
     */
    @Basic
    private double mean;

    /**
     * The standard deviation (the spread) of the rating. This is also known as σ. *
     */
    @Basic
    private double standardDeviation;

    /**
     * на результатах скольких боев основано значение мастерства
     */
    @Basic
    private int battles;

    @Transient
    private volatile boolean dirty = false;

    @Transient
    private volatile boolean newly = false;

    public TrueSkillEntity() {
    }

    public TrueSkillEntity(long profileId, double mean, double standardDeviation) {
        this.profileId = profileId;
        this.mean = mean;
        this.standardDeviation = standardDeviation;
        this.newly = true;
    }

    //====================== Getters and Setters =================================================================================================================================================

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }

    public long getProfileId() {
        return profileId;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isNewly() {
        return newly;
    }

    public void setNewly(boolean newly) {
        this.newly = newly;
    }

    public int getBattles() {
        return battles;
    }

    public void setBattles(int battles) {
        this.battles = battles;
    }

    public void incBattles() {
        this.battles++;
        this.dirty = true;
    }

    @Override
    public Long getId() {
        return profileId;
    }

}
