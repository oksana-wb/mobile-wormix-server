package com.pragmatix.app.settings;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.02.12 12:16
 */
public class AwardBackpackItem implements Cloneable {
    
    private int weaponId;
    
    private int count;
    
    private int stuffId;

    // выдать предмет на количество часов
    private int expireHours;

    // выдать предмет до конкретного времени
    private int expireTimeInSeconds;

    public int getWeaponId() {
        return weaponId;
    }

    public void setWeaponId(int weaponId) {
        this.weaponId = weaponId;
    }

    public int getStuffId() {
        return stuffId;
    }

    public void setStuffId(int stuffId) {
        this.stuffId = stuffId;
    }

    public int getExpireHours() {
        return expireHours;
    }

    public void setExpireHours(int expireHours) {
        this.expireHours = expireHours;
    }

    public int getExpireTimeInSeconds() {
        return expireTimeInSeconds;
    }

    public void setExpireTimeInSeconds(int expireTimeInSeconds) {
        this.expireTimeInSeconds = expireTimeInSeconds;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "{" +
                "weaponId=" + weaponId +
                ", count=" + count +
//                ", stuffId=" + stuffId +
//                ", expireHours=" + expireHours +
                '}';
    }

    @Override
    public final AwardBackpackItem clone() {
        try {
            return (AwardBackpackItem)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
