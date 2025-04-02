package com.pragmatix.app.cache.loaders;

import com.pragmatix.app.domain.UserProfileEntity;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.DaoService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.gameapp.cache.loaders.ILoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Класс загружает из БД  UserProfileEntity и преобразует его в UserProfile
 * <p/>
 * User: denis
 * Date: 22.11.2009
 * Time: 17:18:42
 */
@Component
public class UserProfileLoader implements ILoader<UserProfile, Object> {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileLoader.class);

    @Resource
    private DaoService daoService;

    @Resource
    private ProfileService profileService;

    // блокируем загрузку профиля из БД (например при закрытии сезона)
    private boolean enabled = true;

    @Override
    public UserProfile load(Object profileId) {
        if(!enabled){
            logger.warn("[{}] загрузка профиля из базы заблокирована", profileId);
            return null;
        }
        UserProfileEntity userProfileEntity;
        if (profileId instanceof Number) {
            userProfileEntity = daoService.getUserProfileDao().get(profileId);
        } else {
            throw new IllegalArgumentException("загрузка профиля из базы возможна только по числовому Id");
        }
        if (userProfileEntity != null) {
            if(logger.isTraceEnabled()){
                logger.trace("loaded from DB: {}", userProfileEntity);
            }
            return profileService.initProfile(userProfileEntity);
        } else {
            logger.warn("Can't load  UserProfileEntity by socialId: {}", profileId);
            return null;
        }
    }

    public Class<UserProfile> getLoadedClass() {
        return UserProfile.class;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
