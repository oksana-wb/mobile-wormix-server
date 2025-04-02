package com.pragmatix.app.dao;

import com.pragmatix.app.domain.RestrictionEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 02.06.2016 12:45
 */
@Component
public class RestrictionDao extends AbstractDao<RestrictionEntity> {

    public RestrictionDao() {
        super(RestrictionEntity.class);
    }

    @SuppressWarnings("unchecked")
    public List<RestrictionEntity> getActualRestrictions() {
        return getEm().createNamedQuery("getActualRestrictions").getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<RestrictionEntity> getRestrictionsHistoryFor(Long profileId) {
        return getEm().createNamedQuery("getRestrictionsHistoryFor")
                      .setParameter("profileId", profileId)
                      .getResultList();
    }

    public boolean deleteRestrictionsFor(Long profileId) {
        int count = getEm().createNamedQuery("deleteRestrictionsFor")
                           .setParameter("profileId", profileId)
                           .executeUpdate();
        return count > 0;
    }

}
