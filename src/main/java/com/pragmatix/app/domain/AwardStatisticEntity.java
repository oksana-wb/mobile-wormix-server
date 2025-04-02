package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.Identifiable;

import java.io.Serializable;
import java.util.Date;

/**
 * Статистика по наградам игрока
 * User: denis
 * Date: 01.08.2010
 * Time: 21:38:47
 */

public class AwardStatisticEntity implements Identifiable<Long>, Serializable {

    /**
     * id записи
     */
    private Long id;

    /**
     * id профайла которому принадлежит запись
     */
    private long profileId;

    /**
     * тип покупки (червяк в группу, предмет, ...)
     */
    private int awardType;

    /**
     * id предмета
     */
    private long itemId;

    /**
     * валюта которой была произведена оплата
     */
    private int money;

    /**
     * цена за еденицу
     */
    private int realmoney;

    /**
     * время
     */
    private Date date;

    /**
     * причина
     */
    private String note;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }

    public int getAwardType() {
        return awardType;
    }

    public void setAwardType(int awardType) {
        this.awardType = awardType;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getRealmoney() {
        return realmoney;
    }

    public void setRealmoney(int realmoney) {
        this.realmoney = realmoney;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
