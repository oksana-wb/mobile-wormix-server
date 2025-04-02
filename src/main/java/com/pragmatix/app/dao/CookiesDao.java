package com.pragmatix.app.dao;

import com.pragmatix.app.domain.CookiesEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.04.13 11:30
 */
@Component
public class CookiesDao extends AbstractDao<CookiesEntity> {

    public CookiesDao() {
        super(CookiesEntity.class);
    }

    public void persist(CookiesEntity entity) {
        if(entity == null) {
            return;
        }
        if(entity.newly) {
            // не сохраняем в базу пустую сущность
            if(!entity.isEmpty()) {
                insert(entity);
                entity.newly = false;
            }
        } else if(entity.dirty) {
            getEm().createNamedQuery("CookiesEntity.update").
                    setParameter("values", entity.getValues()).
                    setParameter("profileId", entity.getProfileId()).
                    executeUpdate();
        }
        entity.dirty = false;
    }

}
