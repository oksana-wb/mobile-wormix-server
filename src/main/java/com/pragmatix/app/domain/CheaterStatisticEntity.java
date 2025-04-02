package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.Identifiable;

import java.util.Date;

/**
 * Таблица содержит статистику "подозрительных" действий клиента
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.05.12 14:38
 */
public class CheaterStatisticEntity implements Identifiable<Long> {

    private long id;

    private Date date = new Date();

    private Long profileId;

    private short actionType;

    private String actionParam;

    private int count;

    private String note;

    public CheaterStatisticEntity() {
    }

    public CheaterStatisticEntity(Long profileId, short actionType, String actionParam, int count, String note) {
        this.profileId = profileId;
        this.actionType = actionType;
        this.actionParam = actionParam;
        this.count = count;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public short getActionType() {
        return actionType;
    }

    public void setActionType(short actionType) {
        this.actionType = actionType;
    }

    public String getActionParam() {
        return actionParam;
    }

    public void setActionParam(String actionParam) {
        this.actionParam = actionParam;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
