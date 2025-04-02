package com.pragmatix.app.model;

import com.pragmatix.app.domain.BackpackItemEntity;

/**
 * рюкзак
 */
public class BackpackItem {

    private final short weaponId;
    /**
     * доп. параметр оружия: количество или уровень,
     * если 0, то данный предмет нужно удалить из БД
     */
    private short count;

    private volatile boolean dirty = false;

    private volatile boolean newly = false;


    public BackpackItem(int weaponId) {
        this.weaponId = (short) weaponId;
    }

    public BackpackItem(int weaponId, int count, boolean isNew) {
        this.weaponId = (short) weaponId;
        this.count = (short) count;
        this.newly = isNew;
    }

    public BackpackItem(BackpackItemEntity backpackItemEntity) {
        this.weaponId = backpackItemEntity.getWeaponId();
        this.count = backpackItemEntity.getWeaponCount();
        this.newly = backpackItemEntity.newly;
    }

    public int getWeaponId() {
        return weaponId;
    }

    public int getCount() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean isNotEmpty() {
        return count != 0;
    }

    public void clean() {
        setCount((short) 0);
    }

    /**
     * установить новое значение количества оружия в рюкзаке
     * <b>внимание! необходимо использовать метод setBackpackItemCount()
     * у профайла т.к тот метод попутно вносит изменения в структуру для отправки на клиент </b>
     *
     * @param count новое значение количества оружия
     */
    public void setCount(short count) {
        this.dirty |= this.count != count;
        this.count = count;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isNewly() {
        return newly;
    }

    public void setNewly(boolean newly) {
        this.newly = newly;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        BackpackItem that = (BackpackItem) o;

        if(weaponId != that.weaponId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return weaponId;
    }

    @Override
    public String toString() {
        return "{" + weaponId + (count != -1 ? ":" + count : "") + '}';
    }
}
