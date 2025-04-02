package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.Identifiable;

import java.io.Serializable;

/**
 * User: denis
 * Date: 13.11.2009
 * Time: 1:02:47
 */
public class BackpackItemEntity implements Serializable {

    /**
     *  id профайла игрока которому принадлежит оружие
     */
    private int profileId;

    /**
     * id самого оружия
     */
    private short weaponId;

    /**
     * его количество
     */
    private short weaponCount;


    transient public boolean newly;

    /**
     * @return id профайла игрока которому принадлежит оружие
     */
    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    /**
     * @return  id самого оружия
     */
    public short getWeaponId() {
        return weaponId;
    }

    public void setWeaponId(short weaponId) {
        this.weaponId = weaponId;
    }

    /**
     * @return его количество
     */
    public short getWeaponCount() {
        return weaponCount;
    }

    public void setWeaponCount(short weaponCount) {
        this.weaponCount = weaponCount;
    }


    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        BackpackItemEntity that = (BackpackItemEntity) o;

        if(profileId != that.profileId) return false;
        if(weaponId != that.weaponId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = profileId;
        result = 31 * result + (int) weaponId;
        return result;
    }
}
