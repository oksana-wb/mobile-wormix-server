package com.pragmatix.app.messages.structures;

import com.pragmatix.achieve.award.WormixAchieveAward;
import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.serialization.annotations.Structure;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 01.11.11 14:18
 */
@Structure
public class AchieveAwardStructure {

    public int money;

    public int realmoney;

    public int reaction;

    public int itemId;

    public int awardType;

    public int achievementIndex = -1;

    public int achievementProgress;

    public int boolAchieveIndex = -1;

    public AchieveAwardStructure() {
    }

    public AchieveAwardStructure(WormixAchieveAward achieveAward) {
        this.money = achieveAward.getFuzy();
        this.realmoney = achieveAward.getRuby();
        this.reaction = achieveAward.getReaction();
        this.itemId = achieveAward.getItemId();
        this.awardType = achieveAward.getAwardType();
        if(achieveAward.getAchievementEnum() != null) {
            this.achievementIndex = achieveAward.getAchievementEnum().getIndex();
            this.achievementProgress = achieveAward.getProgress();
        } else {
            this.boolAchieveIndex = achieveAward.getBoolAchieveIndex();
        }
    }

    public AchieveAwardStructure(int itemId, int awardType) {
        this.itemId = itemId;
        this.awardType = awardType;
    }

    @Override
    public String toString() {
        return "AwardStructure{" +
                "money=" + money +
                ", realmoney=" + realmoney +
                ", reaction=" + reaction +
                ", itemId=" + itemId +
                ", awardType=" + awardType +
                ", achievement=" + WormixAchievements.AchievementName.valueOf(achievementIndex) +
                ", achievementProgress=" + achievementProgress +
                ", boolAchieveIndex=" + boolAchieveIndex +
                '}';
    }
}
