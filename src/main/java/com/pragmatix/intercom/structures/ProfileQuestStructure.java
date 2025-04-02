package com.pragmatix.intercom.structures;

import com.pragmatix.quest.dao.QuestDao;
import com.pragmatix.quest.dao.QuestEntity;
import com.pragmatix.serialization.annotations.Structure;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.05.2017 14:04
 */
@Structure
public class ProfileQuestStructure {

    public String q1;
    public String q2;
    public String q3;
    public String q4;

    public ProfileQuestStructure() {
    }

    public void merge(QuestEntity entity) {
        entity.q1 = QuestDao.GSON.fromJson(q1, com.pragmatix.quest.quest01.Data.class);
        entity.q2 = QuestDao.GSON.fromJson(q2, com.pragmatix.quest.quest02.Data.class);
        entity.q3 = QuestDao.GSON.fromJson(q3, com.pragmatix.quest.quest03.Data.class);
        entity.q4 = QuestDao.GSON.fromJson(q4, com.pragmatix.quest.quest04.Data.class);

        entity.dirty = true;
    }

    public ProfileQuestStructure(QuestEntity entity) {
        this.q1 = QuestDao.GSON.toJson(entity.q1);
        this.q2 = QuestDao.GSON.toJson(entity.q2);
        this.q3 = QuestDao.GSON.toJson(entity.q3());
        this.q4 = QuestDao.GSON.toJson(entity.q4());
    }

    @Override
    public String toString() {
        return "{" +
                "q1='" + q1 + '\'' +
                ", q2='" + q2 + '\'' +
                ", q3='" + q3 + '\'' +
                ", q4='" + q4 + '\'' +
                '}';
    }
}
