package com.pragmatix.app.model;

import com.pragmatix.app.common.BoostFamily;

import java.util.concurrent.TimeUnit;

/**
 * Краткие характеристики предмета
 * <p/>
 * User: denis
 * Date: 29.09.2010
 * Time: 15:47:13
 */
public class Stuff implements IItem {

    public static final TimeUnit EXPIRE_TIME_UNIT = TimeUnit.MILLISECONDS;

    /**
     * id предмета
     */
    private Short stuffId;

    private String name;

    private int price;

    private int realprice;

    private int requiredLevel;

    /**
     * hp количество жизней которое добавляет шапка команде
     */
    private int hp;

    private int reaction;

    /**
     * шапки для акций
     */
    private boolean special;

    private boolean kit;

    private boolean temporal;

    /**
     * время "жизни" предмета
     */
    private long expire;

    // предмет является ускорителем (if != NULL)
    private BoostFamily boostFamily;

    private int boostParam;

    private boolean sticker;

    private boolean craftBase;

    public Stuff() {
    }

    public Short getStuffId() {
        return stuffId;
    }

    public String getName() {
        return name;
    }

    public int needMoney() {
        return price;
    }

    public int needRealMoney() {
        return realprice;
    }

    public int needLevel() {
        return requiredLevel;
    }

    public int getHp() {
        return hp;
    }

    public int getPrice() {
        return price;
    }

    public int getRealprice() {
        return realprice;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public void setStuffId(Short stuffId) {
        this.stuffId = stuffId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setRealprice(int realprice) {
        this.realprice = realprice;
    }

    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getReaction() {
        return reaction;
    }

    public void setReaction(int reaction) {
        this.reaction = reaction;
    }

    public boolean isSpecial() {
        return special;
    }

    public void setSpecial(boolean special) {
        this.special = special;
    }

    public void setKit(boolean kit) {
        this.kit = kit;
    }

    public boolean isKit() {
        return kit && !isBoost() && !isCraftBase();
    }

    public boolean isHat() {
        return !kit && !isBoost() && !isCraftBase();
    }

    public boolean isBoost() {
        return boostFamily != null;
    }

    public boolean isTemporal() {
        return temporal;
    }

    public void setTemporal(boolean temporal) {
        this.temporal = temporal;
    }

    public long getExpireTime() {
        return expire;
    }

    public int getExpireTimeInSeconds() {
        return (int) EXPIRE_TIME_UNIT.toSeconds(expire);
    }

    public int getExpireTimeInHours() {
        return (int) EXPIRE_TIME_UNIT.toHours(expire);
    }

    public void setExpire(String expire) {
        if(expire != null && !expire.isEmpty()) {
            TimeUnit timeUnit = TimeUnit.valueOf(expire.split(":")[1]);
            this.expire = EXPIRE_TIME_UNIT.convert(Long.valueOf(expire.split(":")[0]), timeUnit);
            this.temporal = true;
        }
    }

    public BoostFamily getBoostFamily() {
        return boostFamily;
    }

    public void setBoostFamily(BoostFamily boostFamily) {
        this.boostFamily = boostFamily;
        if(boostFamily != null){
            this.temporal = true;
        }
    }

    public int getBoostParam() {
        return boostParam;
    }

    public void setBoostParam(int boostParam) {
        this.boostParam = boostParam;
    }

    public boolean isSticker() {
        return sticker;
    }

    public void setSticker(boolean sticker) {
        this.sticker = sticker;
    }

    public boolean isCraftBase() {
        return craftBase;
    }

    public void setCraftBase(boolean craftBase) {
        this.craftBase = craftBase;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        Stuff stuff = (Stuff) o;

        if(!stuffId.equals(stuff.stuffId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return stuffId.hashCode();
    }

    @Override
    public String toString() {
        String type;
        if(isBoost()) {
            type = "Boost";
        } else if(isCraftBase()) {
            type = "CraftBase";
        } else if(isSticker()) {
            type = "Sticker";
        } else {
            type = !isKit() ? "Hat" : "Kit";
        }
        return type + "{" +
                "stuffId=" + stuffId +
                ", name='" + name + '\'' +
                '}';
    }
}