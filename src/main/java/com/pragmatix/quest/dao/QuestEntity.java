package com.pragmatix.quest.dao;

import com.pragmatix.gameapp.common.Identifiable;
import com.pragmatix.quest.quest01.Quest01DataUserType;
import com.pragmatix.quest.quest02.Quest02DataUserType;
import com.pragmatix.quest.quest03.Quest03DataUserType;
import com.pragmatix.quest.quest04.Quest04DataUserType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.12.2015 9:02
 */
@Entity
@Table(schema = "wormswar", name = "quest_progress")
@TypeDef(name = "Quest01DataUserType", typeClass = Quest01DataUserType.class)
@TypeDef(name = "Quest02DataUserType", typeClass = Quest02DataUserType.class)
@TypeDef(name = "Quest03DataUserType", typeClass = Quest03DataUserType.class)
@TypeDef(name = "Quest04DataUserType", typeClass = Quest04DataUserType.class)
public class QuestEntity implements Identifiable<Long> {

    @Id
    private long profileId;

    @Column(name = "q1")
    @Type(type = "Quest01DataUserType")
    public com.pragmatix.quest.quest01.Data q1;

    @Column(name = "q2")
    @Type(type = "Quest02DataUserType")
    public com.pragmatix.quest.quest02.Data q2;

    @Column(name = "q3", columnDefinition = "character varying")
    @Type(type = "Quest03DataUserType")
    public com.pragmatix.quest.quest03.Data q3;

    @Column(name = "q4", columnDefinition = "character varying")
    @Type(type = "Quest04DataUserType")
    public com.pragmatix.quest.quest04.Data q4;

    @Transient
    public volatile boolean dirty;

    @Transient
    public volatile boolean newly;

    @Override
    public Long getId() {
        return profileId;
    }

    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }

    public com.pragmatix.quest.quest03.Data q3(){
        if(q3 == null)
            q3 = new com.pragmatix.quest.quest03.Data();
        return q3;
    }

    public com.pragmatix.quest.quest04.Data q4(){
        if(q4 == null)
            q4 = new com.pragmatix.quest.quest04.Data();
        return q4;
    }

    public boolean isEmpty() {
        return q1.isEmpty() && q2.isEmpty() && (q3 == null || q3.isEmpty() && (q4 == null || q4.isEmpty()));
    }

    @Override
    public String toString() {
        return "QuestEntity{" +
                "profileId=" + profileId +
                ", q1=" + q1 +
                ", q2=" + q2 +
                ", q3=" + q3 +
                ", q4=" + q4 +
                '}';
    }
}
