package com.pragmatix.app.model;

import com.pragmatix.app.settings.GenericAward;

/**
 * описание модели уровня
 * User: denis
 * Date: 15.11.2009
 * Time: 2:49:58
 */
public class Level {

    /**
     * номер уровня
     */
    private int level;

    /**
     * необходимо опыта для перехода на следующий уровень
     */
    private int nextLevelExp;

    /**
     * количество жизней у червя на текущем уровне
     */
    private Integer levelHp;

    /**
     * максимальное количество червей на текущем уровне(включая себя)
     */
    private Integer maxWormsCount;

    private int delay;

    private GenericAward award;

    public Level() {
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getNextLevelExp() {
        return nextLevelExp;
    }

    public void setNextLevelExp(int nextLevelExp) {
        this.nextLevelExp = nextLevelExp;
    }

    public Integer getLevelHp() {
        return levelHp;
    }

    public void setLevelHp(Integer levelHp) {
        this.levelHp = levelHp;
    }

    public Integer getMaxWormsCount() {
        return maxWormsCount;
    }

    public void setMaxWormsCount(Integer maxWormsCount) {
        this.maxWormsCount = maxWormsCount;
    }

    public GenericAward getAward() {
        return award;
    }

    public void setAward(GenericAward award) {
        this.award = award;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        Level that = (Level) o;

        if(level != that.level) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return level;
    }

    @Override
    public String toString() {
        return "Level{" +
                "level=" + level +
                ", nextLevelExp=" + nextLevelExp +
                '}';
    }

}
