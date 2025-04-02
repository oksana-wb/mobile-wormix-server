package com.pragmatix.achieve.messages.server;

import com.pragmatix.achieve.domain.IAchievementName;
import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.achieve.domain.WormixAchievements.AchievementName;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

import java.util.*;

/**
 * Значения достижений
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.07.11 15:56
 */
@Command(13003)
public class GetAchievementsResult implements SecuredResponse {

    public String profileId;

    /**
     * индексы достижений
     */
    public List<Integer> achievementsIndex;

    /**
     * текущее значения соответствующих достижений
     */
    public List<Integer> achievementsValues;

    /**
     * установленные фоаговые достижения
     */
    public List<Integer> boolAchievements;

    /**
     * количество вложенных очков выбора наград
     */
    public byte investedAwardPoints;

    public int timeSequence;

    public GetAchievementsResult() {
    }

    public GetAchievementsResult(String profileId) {
        this.profileId = profileId;
    }

    public GetAchievementsResult(String profileId, byte investedAwardPoints, List<Integer> achievementsIndex, List<Integer> achievementsValues, List<Integer> boolAchievements, int timeSequence) {
        this.profileId = profileId;
        this.investedAwardPoints = investedAwardPoints;
        this.achievementsIndex = achievementsIndex;
        this.achievementsValues = achievementsValues;
        this.boolAchievements = boolAchievements;
        this.timeSequence = timeSequence;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        Map<IAchievementName, Integer> achievements = new TreeMap<>();
        for(int i = 0; i < achievementsIndex.size(); i++) {
            achievements.put(AchievementName.valueOf(achievementsIndex.get(i)), achievementsValues.get(i));
        }
        return "GetAchievementsResult{" +
                "profileId='" + profileId + '\'' +
                ", timeSequence=" + timeSequence +
                ", investedAwardPoints=" + investedAwardPoints +
                ", achievements=" + achievements +
                ", flags=" + boolAchievements +
                '}';
    }

}
