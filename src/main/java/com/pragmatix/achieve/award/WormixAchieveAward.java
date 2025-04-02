package com.pragmatix.achieve.award;

import com.pragmatix.achieve.domain.WormixAchievements;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.07.11 18:42
 */
public class WormixAchieveAward extends AchieveAward {

    @Override
    public void setAchievement(String achievementName) {
        setAchievementEnum(WormixAchievements.AchievementName.valueOf(achievementName));
    }

    @Override
    public int[] getAwardValues() {
        return new int[] {fuzy, ruby, reaction, itemId};
    }

    private int fuzy;
    private int ruby;
    private int reaction;
    private int itemId;

    public int getFuzy() {
        return fuzy;
    }

    public void setFuzy(int fuzy) {
        this.fuzy = fuzy;
    }

    public int getRuby() {
        return ruby;
    }

    public void setRuby(int ruby) {
        this.ruby = ruby;
    }

    public int getReaction() {
        return reaction;
    }

    public void setReaction(int reaction) {
        this.reaction = reaction;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getItemId() {
        return itemId;
    }

    @Override
    public String toString() {
        return "WormixAchieveAward{" +
                "fuzy=" + fuzy +
                ", ruby=" + ruby +
                ", reaction=" + reaction +
                ", itemId=" + itemId +
                '}';
    }
}
