package com.pragmatix.app.services;

import com.google.gson.Gson;
import com.pragmatix.app.common.ItemCheck;
import com.pragmatix.app.common.ItemType;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.app.domain.AwardStatisticEntity;
import com.pragmatix.app.domain.PaymentStatisticEntity;
import com.pragmatix.app.domain.ShopStatisticEntity;
import com.pragmatix.app.domain.WipeStatisticEntity;
import com.pragmatix.app.model.IItem;
import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.Weapon;
import com.pragmatix.gameapp.services.TaskService;
import com.pragmatix.intercom.structures.UserProfileIntercomStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.Date;

/**
 * Сервис для сбора и записи статистики
 *
 * @author denis
 *         Date: 13.01.2010
 *         Time: 23:25:47
 */
@Service
public class StatisticService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private TaskService taskService;

    @Resource
    private DaoService daoService;

    @Resource
    private StuffService stuffService;

    @Resource
    private WeaponService weaponService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private CloneProfileService cloneProfileService;

    /**
     * Статистика покупки предметов
     *
     * @param profileId id профайла который совершает покупку
     * @param itemId    id предмета
     * @param moneyType тип денег за которые совершаеться покупка
     * @param count     количество
     */
    public void buyShopItemStatistic(final Long profileId, final int itemId, final int moneyType, final int count, final int level) {
        TransactionCallback transactionTask = status -> {
            IItem item;
            ShopStatisticEntity entity = new ShopStatisticEntity();
            if(ItemCheck.isWeapon(itemId)) {
                item = weaponService.getWeapon(itemId);
                entity.setItemType(ItemType.WEAPON.getType());
                entity.setItemId(((Weapon) item).getWeaponId());
            } else {
                //приводим к short тк в кеше предметы храним по ключу объекта типа Short
                item = stuffService.getStuff((short) itemId);
                entity.setItemType(ItemType.STUFF.getType());
                entity.setItemId(((Stuff) item).getStuffId());
            }
            entity.setProfileId(profileId);
            entity.setLevel((short) level);
            entity.setMoneyType(moneyType);
            if(moneyType == 0) {
                entity.setPrice(item.needRealMoney());
            } else {
                entity.setPrice(item.needMoney());
            }
            entity.setCount(Math.abs(count));
            entity.setDate(new Date());
            daoService.getShopStatisticDao().insert(entity);
            return null;
        };
        taskService.addTransactionTask(transactionTask);
    }

    /**
     * Логирование статистики покупки
     *
     * @param profileId id профайла который совершает покупку
     * @param moneyType тип денег за которые совершаеться покупка
     * @param price     цена покупки
     * @param itemType
     * @param count
     * @param itemId
     * @param level
     */
    public void buyItemStatistic(final Long profileId, final int moneyType, final int price, final ItemType itemType, final int count, final int itemId, final int level) {
        TransactionCallback transactionTask = status -> {
            ShopStatisticEntity entity = new ShopStatisticEntity();
            entity.setProfileId(profileId);
            entity.setItemType(itemType.getType());
            entity.setItemId(itemId);
            entity.setMoneyType(moneyType);
            entity.setPrice(price);
            entity.setCount(count);
            entity.setDate(new Date());
            entity.setLevel((short) level);
            daoService.getShopStatisticDao().insert(entity);
            return null;
        };
        taskService.addTransactionTask(transactionTask);
    }

    /**
     * логирование статистики по выдачи призов
     *
     * @param profileId   профайл игрока котором выдаи приз
     * @param money       деньги
     * @param realMoney   реалы
     * @param itemId      в какой группе
     * @param type        за что выдали
     * @param chainedTask задача(и) которые необходимо выполнить в рамках данной транзакции
     */
    public void awardStatistic(final Long profileId, final int money, final int realMoney, final long itemId, final int type, final String note, final Runnable... chainedTask) {
        TransactionCallback transactionTask = status -> {
            AwardStatisticEntity entity = new AwardStatisticEntity();
            entity.setProfileId(profileId);
            entity.setAwardType(type);
            entity.setItemId(itemId);
            entity.setMoney(money);
            entity.setRealmoney(realMoney);
            entity.setDate(new Date());
            entity.setNote(note);
            daoService.getAwardStatisticDao().insert(entity);

            for(Runnable task : chainedTask) {
                if(task != null) {
                    task.run();
                }
            }

            return null;
        };
        taskService.addTransactionTask(transactionTask);
    }

    /**
     * логирование статистики обнуления игрока
     */
    public long wipeStatistic(final UserProfile profile, final String cmd, @Null final String adminLogin) {
        return insertWipeStatisticEntity(getWipeStatisticEntity(profile, cmd, adminLogin));
    }

    public long insertWipeStatisticEntity(final WipeStatisticEntity entity) {
        return transactionTemplate.execute(transactionStatus -> {
            daoService.getWipeStatisticDao().insert(entity);
            return entity.getId();
        });
    }

    public WipeStatisticEntity getWipeStatisticEntity(final UserProfile profile, final String cmd, @Null final String adminLogin) {
        String profileStructure = "";
        try {
            UserProfileIntercomStructure profileIntercomStructure = cloneProfileService.dumpToStructure(profile);
            profileStructure = new Gson().toJson(profileIntercomStructure);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        WipeStatisticEntity entity = new WipeStatisticEntity();
        entity.setProfileId(profile.getId());
        entity.setDate(new Date());
        entity.setMoney(profile.getMoney());
        entity.setRealmoney(profile.getRealMoney());
        entity.setLevel(profile.getLevel());
        entity.setRating(profile.getRating());
        entity.setReactionRate(profile.getReactionRate());
        entity.setAdminLogin(adminLogin);
        entity.setProfileStructure(profileStructure);
        entity.setCmd(cmd);

        return entity;
    }

    /**
     * Логирование статистики сброса параметров игрока
     *
     * @param profileId id профайла который совершает покупку
     * @param moneyType тип денег за которые совершаеться покупка
     * @param price     цена покупки
     */
    public void resetParametersStatistic(final Long profileId, final int moneyType, final int price, final int level) {
        TransactionCallback transactionTask = status -> {
            ShopStatisticEntity entity = new ShopStatisticEntity();
            entity.setProfileId(profileId);
            entity.setItemType(ItemType.PARAMETERS.getType());
            entity.setItemId(0);
            entity.setMoneyType(moneyType);
            entity.setPrice(price);
            entity.setCount(1);
            entity.setDate(new Date());
            entity.setLevel((short) level);
            daoService.getShopStatisticDao().insert(entity);
            return null;
        };
        taskService.addTransactionTask(transactionTask);
    }

    /**
     * Логирование статистики добавления в группу
     *
     * @param profileId     id профайла который совершает покупку
     * @param moneyType     тип денег за которые совершаеться покупка
     * @param price         цена покупки
     * @param wormProfileId кого добавляем в команду
     */
    public void addToGroupStatistic(final Long profileId, final MoneyType moneyType, final int price, final Long wormProfileId, final int level) {
        TransactionCallback transactionTask = status -> {
            ShopStatisticEntity entity = new ShopStatisticEntity();
            entity.setProfileId(profileId);
            entity.setItemType(ItemType.ADD_IN_GROUP.getType());
            entity.setItemId(wormProfileId.intValue());
            entity.setMoneyType(moneyType.getType());
            entity.setPrice(price);
            entity.setCount(1);
            entity.setDate(new Date());
            entity.setLevel((short) level);
            daoService.getShopStatisticDao().insert(entity);
            return null;
        };
        taskService.addTransactionTask(transactionTask);
    }

    /**
     * Статистика удаления червя из группы
     *
     * @param profileId     id профайла который совершает удаление
     * @param wormProfileId кого удаляем из команды
     */
    public void removeFromGroupStatistic(final Long profileId, final Long wormProfileId, final int level) {
        TransactionCallback transactionTask = status -> {
            ShopStatisticEntity entity = new ShopStatisticEntity();
            entity.setProfileId(profileId);
            entity.setItemType(ItemType.REMOVE_FROM_GROUP.getType());
            entity.setItemId(wormProfileId.intValue());
            entity.setMoneyType(0);
            entity.setPrice(0);
            entity.setCount(1);
            entity.setDate(new Date());
            entity.setLevel((short) level);
            daoService.getShopStatisticDao().insert(entity);
            return null;
        };
        taskService.addTransactionTask(transactionTask);
    }

    /**
     * Статистика платежей
     *
     * @param profileId     id профайла который перевел голоса на счет приложения
     * @param moneyType     тип денег (0 - реалы, 1 - фузы)
     * @param value         количество голосов
     * @param paymentStatus первоначальный статус платежа
     * @param amount        количество валюты
     * @param balanse       баланс игрока в валюте платежа после применения платежа
     * @return id платежа
     */
    public Integer insertPaymentRequest(Long profileId, int moneyType, float value, short paymentStatus, boolean completed, int amount, int balanse, String item) {
        PaymentStatisticEntity statistic = new PaymentStatisticEntity();
        statistic.setProfileId(profileId);
        statistic.setMoneyType(moneyType);
        statistic.setVotes(value);
        statistic.setDate(new Date());
        statistic.setPaymentStatus(paymentStatus);
        statistic.setCompleted(completed);
        statistic.setAmount(amount);
        statistic.setBalanse(balanse);
        statistic.setItem(item);
        PaymentStatisticEntity statisticEntity = daoService.getPaymentStatisticDao().insert(statistic);
        return statisticEntity.getId();
    }

    public boolean updatePaymentRequest(final Integer paymentId, short paymentStatus, boolean completed) {
        return daoService.getPaymentStatisticDao().updatePaymentRequest(paymentId, paymentStatus, completed);
    }

}
