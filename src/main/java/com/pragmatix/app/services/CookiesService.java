package com.pragmatix.app.services;

import com.pragmatix.app.dao.CookiesDao;
import com.pragmatix.app.domain.CookiesEntity;
import com.pragmatix.app.model.UserProfile;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.11.2016 13:45
 */
@Service
public class CookiesService {

    @Resource
    private CookiesDao cookiesDao;

    public CookiesEntity getCookiesFor(UserProfile userProfile) {
        if(userProfile.getCookiesEntity() == null) {
            int profileId = userProfile.getId().intValue();
            CookiesEntity entity = cookiesDao.get(profileId);
            if(entity == null) {
                entity = new CookiesEntity(profileId);
            }
            userProfile.setCookiesEntity(entity);
        }
        return userProfile.getCookiesEntity();
    }

    public String[] cookiesToArray(CookiesEntity cookiesEntity) {
        Object[] values = cookiesEntity.getValues();
        String[] result = new String[values.length];
        for(int i = 0; i < values.length; i++) {
            result[i] = "" + values[i];
        }
        return result;
    }

}
