package com.pragmatix.achieve.award;

import com.pragmatix.achieve.domain.IAchievementName;

import javax.validation.constraints.Null;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.07.11 17:07
 */
public abstract class AchieveAward {

    private int boolAchieveIndex = -1;
    private IAchievementName achievementEnum;
    private int progress;
    private int points;
    /**
     * id награды для занесения в статистику выдачи наград
     */
    private int awardType;

    public abstract void setAchievement(String achievementName);

    @Null
    public IAchievementName getAchievementEnum() {
        return achievementEnum;
    }

    public void setAchievementEnum(IAchievementName achievementEnum) {
        this.achievementEnum = achievementEnum;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getAwardType() {
        return awardType;
    }

    public void setAwardType(int awardType) {
        this.awardType = awardType;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public abstract int[] getAwardValues();

    public void setBoolAchieveIndex(int boolAchieveIndex) {
        this.boolAchieveIndex = boolAchieveIndex;
    }

    public int getBoolAchieveIndex() {
        return boolAchieveIndex;
    }

}
