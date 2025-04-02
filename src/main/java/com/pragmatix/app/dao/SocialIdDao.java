package com.pragmatix.app.dao;

import com.pragmatix.app.domain.SocialIdEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.09.11 11:32
 */
@Component
public class SocialIdDao extends AbstractDao<SocialIdEntity> {

    protected SocialIdDao() {
        super(SocialIdEntity.class);
    }
    
    public SocialIdEntity selectByStringId(String stringId){
      return (SocialIdEntity) getEm().createNamedQuery("selectByStringId").setParameter("stringId", stringId).getSingleResult();
    }

    public void reassignStingIdToNewProfile(Long profileId, String stringId){
      getEm().createNamedQuery("reassignStingIdToNewProfile")
              .setParameter("profileId", profileId)
              .setParameter("stringId", stringId)
              .executeUpdate();
    }

    public void dissociateStingIdFromProfile(Long profileId, String stringId){
      getEm().createNamedQuery("dissociateStingIdFromProfile")
              .setParameter("profileId", profileId)
              .setParameter("stringId", stringId)
              .executeUpdate();
    }

}
