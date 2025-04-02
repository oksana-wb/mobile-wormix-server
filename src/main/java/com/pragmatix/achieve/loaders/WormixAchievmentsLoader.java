package com.pragmatix.achieve.loaders;

import com.pragmatix.achieve.dao.AchieveDao;
import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.gameapp.cache.loaders.ILoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.07.11 13:53
 */
@Component
public class WormixAchievmentsLoader implements ILoader<WormixAchievements, String> {

    private static final Logger log = LoggerFactory.getLogger(WormixAchievmentsLoader.class);

    @Resource
    private AchieveDao achievementDao;

    @Override
    public WormixAchievements load(String key) {
        WormixAchievements entity = achievementDao.selectById(key, getLoadedClass());
        if (entity != null) {
            if(log.isTraceEnabled()){
                log.trace("loaded from DB: {}", entity);
            }
        } else {
            log.warn("Can't load  WormixAchievementsEntity by id: {}", key);
            return null;
        }
        return entity;
    }

    @Override
    public Class<WormixAchievements> getLoadedClass() {
        return WormixAchievements.class;
    }
}
