package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.Identifiable;

import java.util.Date;

/**
 * Статистика покупки разного стафа в игре
 *
 * @author denis
 *         Date: 13.01.2010
 *         Time: 20:53:46
 */
public class ShopStatisticEntity implements Identifiable {

    /**
     * id записи
     */
    private Integer id;

    /**
     * id профайла которому принадлежит запись
     */
    private Long profileId;

    /**
     * тип покупки (червяк в группу, предмет, ...)
     */
    private int itemType;

    /**
     * id предмета
     */
    private int itemId;

    /**
     * валюта которой была произведена оплата
     */
    private int moneyType;

    /**
     * цена за еденицу
     */
    private int price;

    /**
     * количество
     */
    private int count;

    /**
     * время
     */
    private Date date;

    /**
     * уровень игрока в момент покупки
     */
    private Short level;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public int getItemType() {
        return itemType;
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getMoneyType() {
        return moneyType;
    }

    public void setMoneyType(int moneyType) {
        this.moneyType = moneyType;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Short getLevel() {
        return level == null ? 0 : level;
    }

    public void setLevel(Short level) {
        this.level = level;
    }
}
