package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.TypeableEnum;

import java.sql.Date;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.05.12 14:38
 */
public class CheaterStatisticItem {

    private long id;

    private final TypeableEnum actionType;

    private final String actionParam;

    private int count;

    private String note;

    public CheaterStatisticItem(TypeableEnum actionType, String actionParam, int count, String note) {
        this.actionType = actionType;
        this.actionParam = actionParam;
        this.count = count;
        this.note = note;
    }

    public void incCount() {
        count++;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public TypeableEnum getActionType() {
        return actionType;
    }

    public String getActionParam() {
        return actionParam;
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

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        CheaterStatisticItem item = (CheaterStatisticItem) o;

        if(!actionParam.equals(item.actionParam)) return false;
        if(!actionType.equals(item.actionType)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = actionType.hashCode();
        result = 31 * result + actionParam.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CheaterStatisticItem{" +
                "id=" + id +
                ", actionType=" + actionType +
                ", actionParam='" + actionParam + '\'' +
                ", count=" + count +
                ", note='" + note + '\'' +
                '}';
    }
}
