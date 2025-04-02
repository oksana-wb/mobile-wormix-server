package com.pragmatix.app.dao;

import com.pragmatix.app.domain.CheaterStatisticEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.09.11 11:32
 */
@Component
public class CheaterStatisticDao extends AbstractDao<CheaterStatisticEntity> {

    protected CheaterStatisticDao() {
        super(CheaterStatisticEntity.class);
    }

    public void updateCheaterStatistic(long id, int count, String note) {
        getEm().createNamedQuery("updateCheaterStatistic").setParameter("count", count).setParameter("note", note).setParameter("id", id).executeUpdate();
    }

}
