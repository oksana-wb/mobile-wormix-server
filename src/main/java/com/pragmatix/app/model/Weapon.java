package com.pragmatix.app.model;

import com.pragmatix.app.common.ItemCheck;

/**
 * характеристики оружия
 */
public class Weapon implements IItem, Comparable<Weapon> {

    private static final int COMPLEX_BASE = 10;

    public enum WeaponType {
        INFINITE,// классическое бесконечное
        COMPLEX,// составное (оружие выкупается по частям)
        CONSUMABLE,// единичное (перевязка, антитоксин и т.д.)
        SEASONAL,// сезонное
    }

    private int weaponId;

    private String name;

    private int price;

    private int realprice;

    private int requiredLevel;

    private WeaponType type = WeaponType.INFINITE;
    /**
     * цена возмещения http://jira.pragmatix-corp.com/browse/WORMIX-4336
     */
    private int sellPrice;

    private int maxWeaponLevel;

    private int shotsByTurn;

    private int bulletsByShot;

    public boolean isInfinitely(int count) {
        switch (type) {
            case INFINITE:
                return count < 0;
            case COMPLEX:
                return count <= complexWeaponInfiniteValue() || count == -1;
            default:
                return false;
        }
    }

    private int complexWeaponInfiniteValue() {
        return complexWeaponLevelValue(maxWeaponLevel);
    }

    private int complexWeaponLevelValue(int value) {
        return -(COMPLEX_BASE + value);
    }

    public int complexWeaponLevel(int value) {
        if(value == -1) {
            return maxWeaponLevel;
        }
        if(value < -COMPLEX_BASE) {
            return Math.min(maxWeaponLevel, -value - COMPLEX_BASE);
        } else {
            return 0;
        }
    }

    public int getInfiniteValue() {
        switch (type) {
            case INFINITE:
                return -1;
            case COMPLEX:
                return complexWeaponInfiniteValue();
            default:
                return 0;
        }
    }

    public int getInfiniteCount() {
        switch (type) {
            case INFINITE:
                return -1;
            case COMPLEX:
                return maxWeaponLevel;
            default:
                return 0;
        }
    }

    public int difference(int value1, int value2) {
        switch (type) {
            case INFINITE:
                return value1 - value2;
            case COMPLEX:
                return complexWeaponLevel(value1) - complexWeaponLevel(value2);
            default:
                return value1 - value2;
        }
    }

    public int increment(int value, int count) {
        switch (type) {
            case INFINITE:
                return value < 0 || count < 0 ? -1 : value + count;
            case COMPLEX:
                return isInfinitely(value) ? value : complexWeaponLevelValue(Math.min(maxWeaponLevel, complexWeaponLevel(value) + Math.abs(count)));
            default:
                return value + Math.abs(count);
        }
    }

    public int decrement(int value, int count) {
        switch (type) {
            case INFINITE:
            case COMPLEX:
                return value <= 0 ? value : Math.max(0, value - Math.abs(count));
            default:
                return Math.max(0, value - Math.abs(count));
        }
    }

    public int getWeaponId() {
        return weaponId;
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

    public boolean isInfiniteOrComplex() {
        return type == WeaponType.INFINITE || type == WeaponType.COMPLEX;
    }

    public void setWeaponId(int weaponId) {
        this.weaponId = weaponId;
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

    public int getPrice() {
        return price;
    }

    public int getRealprice() {
        return realprice;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public boolean isSeasonal() {
        return type == WeaponType.SEASONAL;
    }

    public boolean isConsumable() {
        return type == WeaponType.CONSUMABLE;
    }

    public int getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(int sellPrice) {
        if(sellPrice > 0) {
            this.type = WeaponType.SEASONAL;
            this.sellPrice = sellPrice;
        }
    }

    public WeaponType getType() {
        return type;
    }

    public boolean isType(WeaponType type) {
        return this.type == type;
    }

    public void setType(WeaponType type) {
        this.type = type;
    }

    public int getMaxWeaponLevel() {
        return maxWeaponLevel;
    }

    public void setMaxWeaponLevel(int maxWeaponLevel) {
        if(maxWeaponLevel > 0) {
            this.type = WeaponType.COMPLEX;
            this.maxWeaponLevel = maxWeaponLevel;
        }
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        Weapon weapon = (Weapon) o;

        if(weaponId != weapon.weaponId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return weaponId;
    }

    @Override
    public int compareTo(Weapon weapon) {
        return Integer.compare(this.weaponId, weapon.weaponId);
    }

    public int getShotsByTurn() {
        return shotsByTurn;
    }

    public void setShotsByTurn(int shotsByTurn) {
        this.shotsByTurn = shotsByTurn;
    }

    public int getBulletsByShot() {
        return bulletsByShot;
    }

    public void setBulletsByShot(int bulletsByShot) {
        this.bulletsByShot = bulletsByShot;
    }

    @Override
    public String toString() {
        return "Weapon{" +
                "id=" + weaponId +
                ", name='" + name + '\'' +
                ", type=" + type +
                (ItemCheck.hasPrice(price) ? ", price=" + price : "") +
                (ItemCheck.hasPrice(realprice) ? ", realprice=" + realprice : "") +
                (requiredLevel > 1 ? ", requiredLevel=" + requiredLevel : "" ) +
                (sellPrice > 0 ? ", sellPrice=" + sellPrice : "") +
                (maxWeaponLevel > 0 ? ", maxWeaponLevel=" + maxWeaponLevel : "") +
                '}';
    }
}
