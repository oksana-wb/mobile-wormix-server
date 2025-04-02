package com.pragmatix.app.dao;

import com.pragmatix.app.domain.ReferralLinkEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.04.13 11:30
 */
@Component
public class ReferralLinkDao extends AbstractDao<ReferralLinkEntity> {

    public ReferralLinkDao() {
        super(ReferralLinkEntity.class);
    }

    public void updateVisitors(ReferralLinkEntity entity) {
        getEm().createNamedQuery("ReferralLinkEntity.updateVisitors").
                setParameter("id", entity.getId()).
                setParameter("visitors", entity.getVisitors()).
                executeUpdate();
    }
}
