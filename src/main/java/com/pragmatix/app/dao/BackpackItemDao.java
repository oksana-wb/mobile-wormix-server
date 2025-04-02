package com.pragmatix.app.dao;

import com.pragmatix.app.domain.BackpackItemEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;

import static java.lang.String.format;

/**
 * Dao класс для сохронения и загрузки BackpackItemEntity
 * User: denis
 * Date: 15.11.2009
 * Time: 3:50:54
 */
@Component
public class BackpackItemDao {

    @PersistenceContext
    protected EntityManager em;

    protected EntityManager getEm() {
        return em;
    }

    /**
     * Установить новое значение weaponCount у оружия юзера
     *
     * @param weaponId    идентификатор оружия
     * @param profileId   id игрока
     * @param weaponCount новое значение количества данного оружия у игрока
     * @return true если обновить удалось
     */
    public boolean update(Long profileId, int weaponId, int weaponCount) {
        String query = format("update wormswar.backpack_item_%d set weapon_count = :count where profile_id = :profileId and weapon_id = :weaponId", getPartitionNumber(profileId));
        int count = getEm().createNativeQuery(query)
                .setParameter("profileId", profileId.intValue())
                .setParameter("weaponId", (short) weaponId)
                .setParameter("count", (short) weaponCount)
                .executeUpdate();
        return count > 0;
    }

    /**
     * создаст в БД BackpackItem
     *
     * @param profileId для кого создать
     * @param weaponId  какое оружие
     * @param count     какое количество
     * @return BackpackItemEntity
     */
    public BackpackItemEntity createBackpackItem(Long profileId, int weaponId, int count) {
        BackpackItemEntity backpackItemEntity = new BackpackItemEntity();
        backpackItemEntity.setProfileId(profileId.intValue());
        backpackItemEntity.setWeaponId((short) weaponId);
        backpackItemEntity.setWeaponCount((short) count);
        //сохроняем в БД
        insertNativeQuery(backpackItemEntity);
        return backpackItemEntity;
    }

    /**
     * удалит предмет из БД
     *
     * @param weaponId  записи в БД
     * @param profileId юзера
     * @return true если удалить удалось
     */
    public boolean deleteBackpackItem(Long profileId, int weaponId) {
        String query = format("delete from wormswar.backpack_item_%d WHERE profile_id = :profileId and weapon_id = :weaponId", getPartitionNumber(profileId));
        int count = getEm().createNativeQuery(query)
                .setParameter("profileId", profileId.intValue())
                .setParameter("weaponId", (short) weaponId)
                .executeUpdate();
        // setNeedCommit();
        return count > 0;
    }

    /**
     * удалит все содержимое рюкзака игроку
     *
     * @param profileId игрока
     * @return true если удалить удалось
     */
    public boolean deleteBackpack(Long profileId) {
        int count = getEm().
                createNativeQuery(
                        format("delete from wormswar.backpack_item_%d WHERE profile_id = :profileId",
                                getPartitionNumber(profileId)))
                .setParameter("profileId", profileId).executeUpdate();
        // setNeedCommit();
        return count > 0;
    }

    @SuppressWarnings("unchecked")
    public List<BackpackItemEntity> getBackpackByProfileId(Long profileId) {
        String query = format("getBackpackByProfileId%d", getPartitionNumber(profileId));
        return (List<BackpackItemEntity>) getEm().createNamedQuery(query)
                .setParameter("profileId", profileId)
                .getResultList();
    }

    private void insertNativeQuery(BackpackItemEntity entity) {
        getEm().createNativeQuery("SELECT wormswar.backpack_item_insert(:profileId, :weaponCount, :weaponId)")
                .setParameter("profileId", entity.getProfileId())
                .setParameter("weaponCount", entity.getWeaponCount())
                .setParameter("weaponId", entity.getWeaponId())
                .getSingleResult();
    }

    private long getPartitionNumber(Long profileId) {
        return profileId % 4;
    }

    /**
     * Запрашивает оружие игроков одним запросом
     *
     * @param idSet список id игроков
     * @return перечень оружия игроков разложенное по игрокам
     */
    @SuppressWarnings({"unchecked"})
    public Map<Integer, List<BackpackItemEntity>> getBackpackByProfileIds(Set<Long> idSet) {
        List<BackpackItemEntity> result = (List<BackpackItemEntity>) getEm().createNamedQuery("getBackpacksByProfileIds")
                .setParameter("profileIds", idSet)
                .getResultList();
        Map<Integer, List<BackpackItemEntity>> resultMap = new HashMap<>();
        for(BackpackItemEntity entity : result) {
            List<BackpackItemEntity> list;
            if(resultMap.containsKey(entity.getProfileId())) {
                list = resultMap.get(entity.getProfileId());
            } else {
                list = new ArrayList<>();
                resultMap.put(entity.getProfileId(), list);
            }
            list.add(entity);
        }
        return resultMap;
    }
}
