package com.pragmatix.app.init;

import com.pragmatix.app.common.ItemCheck;
import com.pragmatix.app.model.Weapon;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Класс для создания списка оружия которое доступно в игрку
 * <b>необходим при первом старте сервера на пустой БД</b>
 * <p/>
 * User: denis, eugene
 * Date: 19.11.2009
 * Time: 21:50:56
 */

public class WeaponsCreator {

    private Map<Integer, Weapon> weaponsMap = new ConcurrentHashMap<>();

    // бесконечное оружие, которое можно купить в магазине
    private short[] infinityWeaponsWithPrice;

    public WeaponsCreator(List<Weapon> weaponEntities) {
        for(Weapon weaponEntity : weaponEntities) {
            weaponsMap.put(weaponEntity.getWeaponId(), weaponEntity);
        }

        this.infinityWeaponsWithPrice = ArrayUtils.toPrimitive(weaponsMap.values().stream()
                .filter(weapon -> weapon.isInfiniteOrComplex() && weapon.getPrice() < ItemCheck.EMPTY_PRICE)
                .map(weapon -> (short) weapon.getWeaponId()).distinct().toArray(Short[]::new));
    }

    public Collection<Weapon> getWeapons() {
        return weaponsMap.values();
    }

    public Weapon getWeapon(int weaponId) {
        return weaponsMap.get(weaponId);
    }

    public short[] getInfinityWeaponsWithPrice() {
        return infinityWeaponsWithPrice;
    }

}
