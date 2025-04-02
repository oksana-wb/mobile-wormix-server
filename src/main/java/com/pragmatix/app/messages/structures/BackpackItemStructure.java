package com.pragmatix.app.messages.structures;

import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.serialization.annotations.Structure;

/**
 * Структура для передачи между клиннтом и сервером конкретного оружия
 * User: denis
 * Date: 09.11.2009
 * Time: 22:23:32
 */
@Structure
public class BackpackItemStructure {
    /**
     * id оружия
     */
    public int weaponId;
    /**
     * количество данного оружия
     * <b>если -1, то количество бесконечно</b>
     */
    public int count;

    public BackpackItemStructure(BackpackItem backpackItem) {
        this.weaponId = backpackItem.getWeaponId();
        this.count = backpackItem.getCount();
    }

    public BackpackItemStructure() {
    }

    public BackpackItemStructure(int weaponId, int count) {
        this.weaponId = weaponId;
        this.count = count;
    }

    @Override
    public String toString() {
        return "{" + weaponId + (count != -1 ? ":" + count : "") + '}';
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        BackpackItemStructure that = (BackpackItemStructure) o;

        if(count != that.count) return false;
        if(weaponId != that.weaponId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = weaponId;
        result = 31 * result + count;
        return result;
    }

}
