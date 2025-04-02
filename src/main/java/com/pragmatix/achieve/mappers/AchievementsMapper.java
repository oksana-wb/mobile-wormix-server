package com.pragmatix.achieve.mappers;

import com.pragmatix.achieve.domain.ProfileAchievements;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.07.11 15:28
 */
public interface AchievementsMapper<T extends ProfileAchievements> {

    T selectAchievements(String profileId);

    int updateAchievements(T entity, int achievePoints);

    int insertAchievements(T entity, int achievePoints);

    int wipeAchievements(String profileId, String wipedId);

}
