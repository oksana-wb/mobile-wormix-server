package com.pragmatix.app.services;

import com.pragmatix.app.dao.BackpackConfDao;
import com.pragmatix.app.domain.BackpackConfEntity;
import com.pragmatix.app.init.WeaponsCreator;
import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.Weapon;
import com.pragmatix.app.services.rating.SeasonService;
import io.vavr.Tuple;
import io.vavr.Tuple4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.Set;

import static com.pragmatix.app.model.Weapon.WeaponType.INFINITE;
import static com.pragmatix.app.model.Weapon.WeaponType.SEASONAL;

/**
 * Сервис со вспомогательными методами
 * для работы с рюкзаком игрока
 * <p/>
 * User: denis
 * Date: 11.12.2009
 * Time: 1:44:44
 */
@Service
public class WeaponService {

    private static final Logger logger = LoggerFactory.getLogger(WeaponService.class);
    public static final int MAX_BACKPACK_CONF_SIZE = 63;

    @Resource
    private WeaponsCreator weaponsCreator;

    @Resource
    private BackpackConfDao backpackConfDao;

    @Autowired(required = false)
    private SeasonService seasonService;

    @Value("#{achieveBonusItemsMap.keySet()}")
    private Set<Integer> achieveBonusItems;

    public boolean isPresentInfinitely(UserProfile userProfile, int weaponId) {
        BackpackItem item = userProfile.getBackpackItemByWeaponId(weaponId);
        Weapon weapon = weaponsCreator.getWeapon(weaponId);
        return item != null && weapon != null && weapon.isInfinitely(item.getCount());
    }

    public boolean isPresentInfinitely(BackpackItem backpackItem) {
        if(backpackItem == null)
            return false;
        Weapon weapon = getWeapon(backpackItem.getWeaponId());
        return weapon != null && weapon.isInfinitely(backpackItem.getCount());
    }

    /**
     * добавит новое оружие в рюкзак игрока или сделает increment оружию если такое уже есть
     *
     * @param userProfile профайл пользователя
     * @param weaponId    идентификатор оружия которое необходимо добавить
     * @param count       количество (>0 - выстрел, ==-1 - оружие)
     * @return было ли добавлено оружие или выстрел в рюкзак
     */
    public boolean addOrUpdateWeapon(UserProfile userProfile, int weaponId, int count) {
        return addOrUpdateWeaponReturnCount(userProfile, weaponId, count, -1) != 0;
    }

    public int addOrUpdateWeaponReturnCount(UserProfile userProfile, int weaponId, int count) {
        return addOrUpdateWeaponReturnCount(userProfile, weaponId, count, -1);
    }

    /**
     * добавит новое оружие в рюкзак игрока или сделает increment оружию если такое уже есть
     *
     * @param userProfile  профайл пользователя
     * @param weaponId     идентификатор оружия которое необходимо добавить
     * @param count        количество (>0 - выстрел, ==-1 - оружие)
     * @param maxShotCount ограничеваем количество выстрелов этого оружия в рюкзаке
     * @return было ли добавлено оружие или выстрел в рюкзак
     */
    public int addOrUpdateWeaponReturnCount(UserProfile userProfile, int weaponId, int count, int maxShotCount) {
        Weapon weapon = getWeapon(weaponId);
        if(weapon == null) {
            return 0;
        }
        if(count == 0) {
            logger.error("weapon count can not be zero!");
            return 0;
        }

        BackpackItem item = userProfile.getBackpackItemByWeaponId(weaponId);
        if(item != null && weapon.isInfinitely(item.getCount())) {
            // патаемся добавить оружие которое у него и так уже есть
            // в случае покупки выстрела, отрабатывает вариант когда вечное и одиночное - это один id
            if(logger.isDebugEnabled()) {
                logger.debug("try add already purchased weapon {} count={}", weapon, count);
            }
            return 0;
        }

        // добавление "выстрела" / очередного уровня оружия
        if(count > 0) {
            if(weapon.isType(SEASONAL) && seasonService != null && !seasonService.isEnabledInCurrentSeason(weapon)) {
                return 0;
            }
            // если данный выстрел уже есть, то обновляем его
            if(item != null) {
                //увеличиваем количество если нет ограничения или не достигнут максимум
                if(maxShotCount <= 0 || item.getCount() < maxShotCount) {
                    int oldValue = item.getCount();
                    int newValue = weapon.increment(item.getCount(), count);
                    userProfile.setBackpackItemCount(item.getWeaponId(), newValue);
                    return weapon.difference(newValue, oldValue);
                } else {
                    return 0;
                }
            } else {
                int newValue = weapon.increment(0, count);
                // иначе создаем новое оружие
                userProfile.addBackpackItem(new BackpackItem(weaponId, newValue, true));
                return count;
            }
        } else if(count == -1 && weapon.isType(INFINITE)) {
            // добавление оружия
            if(item != null) {
                // были выстрелы к этому оружию, делаем его бесконечным
                userProfile.setBackpackItemCount(item.getWeaponId(), weapon.getInfiniteValue());
            } else {
                // иначе создаем новое оружие
                userProfile.addBackpackItem(new BackpackItem(weaponId, weapon.getInfiniteValue(), true));
            }
            return -1;
        }

        if(logger.isDebugEnabled()) {
            logger.debug("invalid weapon count {}", count);
        }
        return 0;
    }

    /**
     * Версия removeOrUpdateWeapon, которая не ругается в лог в случае не найденного оружия
     * (полезна в случае, если есть вероятность, что данного оружия нет в бэкпаке в профиле, т.к. оно было подобрано в бое)
     */
    public void removeOrUpdateWeaponSilent(UserProfile userProfile, int weaponId, int count) {
        removeOrUpdateWeapon(null, userProfile, weaponId, count, false);
    }

    /**
     * используется при уменьшении количества оружия в рюкзаке
     */
    public void removeOrUpdateWeapon(@Null Short missionId, UserProfile userProfile, int weaponId, int count, boolean warnIfNotFound) {
        if(count == 0)
            return;
        count = Math.abs(count);
        BackpackItem item = userProfile.getBackpackItemByWeaponId(weaponId);
        // если данное оружие уже есть, то уменьшаем его количество
        if(item != null) {
            Weapon weapon = getWeapon(weaponId);
            userProfile.setBackpackItemCount(item.getWeaponId(), weapon.decrement(item.getCount(), count));
        } else if(warnIfNotFound) {
            logger.error("missionId=[{}] can't decrement weapon with id {} because weapon not found", missionId != null ? "" + missionId : "PVP", weaponId);
        }
    }

    /**
     * удалить оружие из рюкзака
     *
     * @param profile  для кого
     * @param weaponId id оружия
     * @return результат
     */
    public boolean removeWeapon(UserProfile profile, int weaponId) {
        BackpackItem item = profile.getBackpackItemByWeaponId(weaponId);
        // если данное оружие уже есть, то обновляем его количество
        if(item != null) {
            profile.setBackpackItemCount(item.getWeaponId(), 0);
            return true;
        } else {
            logger.error("can't decrement weapon with id {} because weapon not found", weaponId);
            return false;
        }
    }

    public Weapon getWeapon(int weaponId) {
        Weapon weapon = weaponsCreator.getWeapon(weaponId);
        if(weapon == null) {
            logger.error("Weapon not found by id [{}]", weaponId);
        }
        return weapon;
    }

    @Null
    public short[] getHotkeys(UserProfile profile) {
        BackpackConfEntity backpackConf = getBackpackConfEntity(profile);
        return backpackConf != null ? backpackConf.getHotkeys() : null;
    }

    public short[] getActiveBackpackConf(UserProfile profile) {
        BackpackConfEntity backpackConf = getBackpackConfEntity(profile);
        if(backpackConf != null) {
            if(backpackConf.getActiveConfig() == 2) {
                return backpackConf.getConfig2();
            } else if(backpackConf.getActiveConfig() == 3) {
                return backpackConf.getConfig3();
            } else {
                return backpackConf.getConfig();
            }
        } else {
            return null;
        }
    }

    public Tuple4<short[], short[], short[], Byte> getBackpackConfs(UserProfile profile) {
        BackpackConfEntity backpackConf = getBackpackConfEntity(profile);
        return backpackConf != null ? Tuple.of(backpackConf.getConfig(), backpackConf.getConfig2(), backpackConf.getConfig3(), backpackConf.getActiveConfig()) : Tuple.of(null, null, null, (byte) 0);
    }

    @Null
    public BackpackConfEntity getBackpackConfEntity(UserProfile profile) {
        if(profile.getBackpackConfs() == null) {
            profile.setBackpackConfs(backpackConfDao.get(profile.getId()));
        }
        return profile.getBackpackConfs();
    }

    public BackpackConfEntity getBackpackConfEntityOrCreate(UserProfile profile) {
        if(profile.getBackpackConfs() == null) {
            BackpackConfEntity backpackConfs = backpackConfDao.get(profile.getId());
            profile.setBackpackConfs(backpackConfs != null ? backpackConfs : new BackpackConfEntity(profile.getId()));
        }
        return profile.getBackpackConfs();
    }

    public boolean setHotkeys(UserProfile profile, short[] hotkeys) {
        BackpackConfEntity backpackConf = getBackpackConfEntity(profile);
        if(backpackConf != null) {
            backpackConf.setHotkeys(hotkeys);
            backpackConf.setDirty(true);
        } else {
            profile.setBackpackConfs(new BackpackConfEntity(profile.getProfileId(), null, null, null, hotkeys, (byte)0));
        }
        return true;
    }

    public boolean setBackpackConfs(UserProfile profile, short[] config1, short[] config2, short[] config3, byte activeConfig) {
        if(!validateSettingOfBackpackConf(config1, config2, config3)) return false;
        BackpackConfEntity backpackConfs = getBackpackConfEntity(profile);
        if(backpackConfs != null) {
            backpackConfs.setConfig(config1);
            backpackConfs.setConfig2(config2);
            backpackConfs.setConfig3(config3);
            backpackConfs.setActiveConfig(activeConfig);
            backpackConfs.setDirty(true);
        } else {
            profile.setBackpackConfs(new BackpackConfEntity(profile.getProfileId(), config1, config2, config3, null, activeConfig));
        }
        return true;
    }

    private boolean validateSettingOfBackpackConf(short[] config1, short[] config2, short[] config3) {
        return (config1 == null || config1.length <= MAX_BACKPACK_CONF_SIZE)
                && (config2 == null || config2.length <= MAX_BACKPACK_CONF_SIZE)
                && (config3 == null || config3.length <= MAX_BACKPACK_CONF_SIZE);
    }

    public void wipeBackpackConf(UserProfile profile) {
        if(profile.getBackpackConfs() != null) {
            backpackConfDao.delete(profile.getId());
            profile.setBackpackConfs(null);
        }
    }

//====================== Getters and Setters =================================================================================================================================================

    public void setWeaponsCreator(WeaponsCreator weaponsCreator) {
        this.weaponsCreator = weaponsCreator;
    }
}
