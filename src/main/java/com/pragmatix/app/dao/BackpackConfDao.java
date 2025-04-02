package com.pragmatix.app.dao;

import com.pragmatix.app.domain.BackpackConfEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.04.13 11:30
 */
@Component
public class BackpackConfDao extends AbstractDao<BackpackConfEntity> {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    public BackpackConfDao() {
        super(BackpackConfEntity.class);
    }

    public void delete(Long profileId) {
        getEm().createQuery("delete from BackpackConfEntity where profileId = :profileId")
                .setParameter("profileId", profileId)
                .executeUpdate();
    }

    public void persist(BackpackConfEntity entity) {
        if(entity == null)
            return;
        if(entity.isNewly()) {
            insert(entity);
            entity.setNewly(false);
        } else if(entity.isDirty()) {
            getEm().createNamedQuery("BackpackConfEntity.update").
                    setParameter("config1", entity.getConfig()).
                    setParameter("config2", entity.getConfig2()).
                    setParameter("config3", entity.getConfig3()).
                    setParameter("activeConfig", entity.getActiveConfig()).
                    setParameter("hotkeys", entity.getHotkeys()).
                    setParameter("profileId", entity.getProfileId()).
                    executeUpdate();
            entity.setDirty(false);
        }
    }

    public void updateSeasonsBestRank(BackpackConfEntity entity) {
        transactionTemplate.execute(transactionStatus ->
                jdbcTemplate.update("UPDATE wormswar.backpack_conf SET seasons_best_rank = ? WHERE profile_id = ?", entity.getSeasonsBestRank(), entity.getProfileId())
        );
    }
}
