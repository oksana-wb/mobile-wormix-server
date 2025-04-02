package com.pragmatix.achieve.dao;

import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.mappers.AchievementsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.07.11 16:06
 */
public class AchieveDao {

    private static final Logger log = LoggerFactory.getLogger(AchieveDao.class);

    private Map<Class<? extends ProfileAchievements>, AchievementsMapper> mappersMap;

    public <E extends ProfileAchievements> E selectById(String id, Class<E> eClass) {
        return (E) getMapper(eClass).selectAchievements(id);
    }

    public <E extends ProfileAchievements> boolean update(ProfileAchievements entity, int achievePoints, Class<E> eClass) {
        return getMapper(eClass).updateAchievements(entity, achievePoints) > 0;
    }

    public <E extends ProfileAchievements> boolean insert(ProfileAchievements entity, int achievePoints, Class<E> eClass) {
        try {
            return getMapper(eClass).insertAchievements(entity, achievePoints) > 0;
        } catch (Exception e) {
            log.error(e.toString(), e);
            return false;
        }
    }

    public <E extends ProfileAchievements> boolean wipe(String profileId, String wipedId, Class<E> eClass) {
        return getMapper(eClass).wipeAchievements(profileId, wipedId) > 0;
    }

    private <E extends ProfileAchievements> AchievementsMapper getMapper(Class<E> eClass) {
        AchievementsMapper achievementsMapper = mappersMap.get(eClass);
        if(achievementsMapper == null) {
            throw new IllegalArgumentException("AchievementsMapper not found for entity's class " + eClass);
        }
        return achievementsMapper;
    }

    public void setMappers(Map<String, AchievementsMapper> mappers) throws ClassNotFoundException {
        log.info("add mappers: {}", mappers.keySet());
        mappersMap = new ConcurrentHashMap<>();
        for(Map.Entry<String, AchievementsMapper> entry : mappers.entrySet()) {
            mappersMap.put((Class<? extends ProfileAchievements>) Class.forName(entry.getKey()), entry.getValue());
        }
    }

    public void persist(ProfileAchievements profileAchievements, int awardPoints) {
        persist(profileAchievements, awardPoints, true);
    }

    public boolean persist(ProfileAchievements profileAchievements, int awardPoints, boolean updateIfInsertFailure) {
        if(profileAchievements.isNewly() || profileAchievements.isDirty()) {
            if(profileAchievements.isNewly()) {
                boolean insertResult = insert(profileAchievements, awardPoints, profileAchievements.getClass());
                if(!insertResult) {
                    if(updateIfInsertFailure) {
                        update(profileAchievements, awardPoints, profileAchievements.getClass());
                    } else {
                        return false;
                    }
                }
                profileAchievements.setNewly(false);
            } else {
                update(profileAchievements, awardPoints, profileAchievements.getClass());
            }
            profileAchievements.setDirty(false);
        }
        return true;
    }

}
