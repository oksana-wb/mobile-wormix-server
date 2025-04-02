package com.pragmatix.quest.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pragmatix.dao.AbstractDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.12.2015 9:08
 */
@Service
public class QuestDao extends AbstractDao<QuestEntity> {

    public static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    @Value("${QuestDao.useJsonb:false}")
    private boolean useJsonb = false;

    protected QuestDao() {
        super(QuestEntity.class);
    }

    @PostConstruct
    public void init(){
        QuestDataUserType.useJsonb = useJsonb;
    }

    public QuestEntity newEntity(Long profileId) {
        QuestEntity entity = new QuestEntity();
        entity.setProfileId(profileId);
        entity.q1 = new com.pragmatix.quest.quest01.Data();
        entity.q2 = new com.pragmatix.quest.quest02.Data();
        entity.newly = true;
        return entity;
    }

    public void persist(QuestEntity entity) {
        if(entity.newly) {
            if(!entity.isEmpty()) {
                insert(entity);
                entity.dirty = false;
                entity.newly = false;
            }
        } else if(entity.dirty) {
            em.createQuery("update QuestEntity set " +
                    "q1 = :q1" +
                    ", q2 = :q2" +
                    ", q3 = :q3" +
                    ", q4 = :q4" +
                    " where profileId = :profileId")
                    .setParameter("q1", entity.q1)
                    .setParameter("q2", entity.q2)
                    .setParameter("q3", entity.q3 == null || entity.q3.isEmpty() ? null : entity.q3)
                    .setParameter("q4", entity.q4 == null || entity.q4.isEmpty() ? null : entity.q4)
                    .setParameter("profileId", entity.getProfileId())
                    .executeUpdate();
            entity.dirty = false;
        }
    }

}
