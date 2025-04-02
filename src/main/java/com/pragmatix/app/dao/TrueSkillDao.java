package com.pragmatix.app.dao;

import com.pragmatix.app.domain.TrueSkillEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.04.13 11:30
 */
@Component
public class TrueSkillDao extends AbstractDao<TrueSkillEntity> {

    public TrueSkillDao() {
        super(TrueSkillEntity.class);
    }

    public boolean persist(TrueSkillEntity entity) {
        if(entity == null) {
            return false;
        }
        if(entity.isNewly()) {
            // не сохраняем в базу первоночальное значение мастерства
            if(entity.getBattles() > 0) {
                insert(entity);
                entity.setNewly(false);
                return true;
            } else {
                return false;
            }
        } else if(entity.isDirty()) {
            int rows = getEm().createNamedQuery("TrueSkillEntity.update").
                    setParameter("mean", entity.getMean()).
                    setParameter("ststandardDeviation", entity.getStandardDeviation()).
                    setParameter("battles", entity.getBattles()).
                    setParameter("profileId", entity.getProfileId()).
                    executeUpdate();
            entity.setDirty(false);
            return rows == 1;
        } else {
            return false;
        }
    }

}
