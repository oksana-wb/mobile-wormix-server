package com.pragmatix.app.dao;

import com.pragmatix.app.domain.BanEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: denis
 * Date: 01.08.2010
 * Time: 21:44:51
 */
@Component
public class BanDao extends AbstractDao<BanEntity> {

    public BanDao() {
        super(BanEntity.class);
    }

    @SuppressWarnings("unchecked")
    public List<BanEntity> getActualBanList() {
        return (List<BanEntity>) getEm().createNamedQuery("getActualBanList").getResultList();
    }

    /**
     * удалит всю статистику бана игрока
     *
     * @param profileId игрока
     * @return true если удалить удалось
     */
    public boolean deleteFromBanList(Long profileId) {
        int count = getEm().createNamedQuery("deleteFromBanList").
                setParameter("profileId", profileId).executeUpdate();
        // setNeedCommit();
        return count > 0;
    }

    @SuppressWarnings({"unchecked"})
    public List<BanEntity> selectBanEntities(Long profileId){
        List<BanEntity> result = new ArrayList<BanEntity>();
        try {
            result = (List<BanEntity>)getEm().createNamedQuery("selectGamersBans").setParameter("profileId", profileId).getResultList();
        } catch(NoResultException e) {
        }
        return result;
    }
}