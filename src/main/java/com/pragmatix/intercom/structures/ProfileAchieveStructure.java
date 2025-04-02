package com.pragmatix.intercom.structures;

import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.serialization.annotations.Structure;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.05.2017 14:26
 */
@Structure(nullable = true)
public class ProfileAchieveStructure {

    public String profileId;

    public byte investedAwardPoints;

    public short[] achievements;

    public int[] statistics;

    public byte[] boolAchievements;

    public int timeSequence;

    public ProfileAchieveStructure() {
    }

    public ProfileAchieveStructure(ProfileAchievements profileAchievements) {
        profileId = profileAchievements.getProfileId();
        investedAwardPoints = profileAchievements.getInvestedAwardPoints();
        achievements = profileAchievements.getAchievements();
        statistics = profileAchievements.getStatistics();
        boolAchievements = profileAchievements.getBoolAchievements();
        timeSequence = profileAchievements.getTimeSequence();
    }

    public void merge(ProfileAchievements profileAchievements) {
        profileAchievements.setInvestedAwardPoints(investedAwardPoints);
        profileAchievements.setAchievements(achievements);
        profileAchievements.setStatistics(statistics);
        profileAchievements.setBoolAchievements(boolAchievements);
        profileAchievements.setTimeSequence(timeSequence);

        profileAchievements.setDirty(true);
    }
}
