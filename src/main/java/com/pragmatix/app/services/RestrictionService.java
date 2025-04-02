package com.pragmatix.app.services;

import com.pragmatix.app.domain.RestrictionEntity;
import com.pragmatix.app.messages.RestrictionItemStructure;
import com.pragmatix.app.messages.server.ProfileRestrictions;
import com.pragmatix.app.model.RestrictionItem;
import com.pragmatix.app.model.RestrictionItem.BlockFlag;
import com.pragmatix.app.model.RestrictionItem.Operation;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.serialization.AppBinarySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static com.pragmatix.app.model.RestrictionItem.isInfinite;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 02.06.2016 12:29
 *         <p>
 *         Сервис запретов - ограничений игроков на отдельные функции (неполный бан)
 *         http://jira.pragmatix-corp.com/browse/WM-5084
 * @see BanService аналог в системе банов
 */
@Service
public class RestrictionService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    DaoService daoService;

    @Resource
    TransactionTemplate transactionTemplate;

    @Resource
    ProfileService profileService;

    @Resource
    AppBinarySerializer serializer;

    /**
     * profileId -> список активных в данный момент блокировок
     */
    private Map<Long, List<RestrictionItem>> restrictionsByProfile = new ConcurrentHashMap<>();

    /**
     * считывает активные блокировки из базы в память при запуске
     */
    public void init() {
        restrictionsByProfile.clear();
        for(RestrictionEntity entity : daoService.getRestrictionDao().getActualRestrictions()) {
            List<RestrictionItem> restrictions = getRestrictionsListOrCreateNew(entity.getProfileId());
            restrictions.add(new RestrictionItem(entity));
        }
    }

    /**
     * Проверяет, заблокирована ли данная возможность у данного игрока в настоящий момент времени
     *
     * @param profileId профиль игрока
     * @param blockFlag какая возможность
     * @return true если заблокирована, иначе false
     */
    public boolean isRestricted(Long profileId, final BlockFlag blockFlag) {
        return getRestrictions(profileId).stream().anyMatch(item -> item.isBlocking(blockFlag));
    }

    /**
     * Проверяет, что игрок забанен (т.е. на него наложен полный запрет)
     *
     * @param profileId id профиля игрока
     * @return true если на нём есть {@link BlockFlag#TOTAL} блокировка
     */
    public boolean isRestrictedTotally(Long profileId) {
        return getRestrictions(profileId).stream().anyMatch(RestrictionItem::isTotal);
    }

    /**
     * Добавляет игроку новый запрет на несколько функций
     *
     * @param profileId      профиль игрока
     * @param blocks         комбинация флагов блокировки {@link BlockFlag}
     * @param durationInDays продолжительность в днях (null - вечный запрет)
     * @param reason         причина: один из {@link com.pragmatix.app.common.BanType} или другой, дополнительный код
     * @param note           комментарий от админа
     * @param admin          логин админа
     * @return созданную блокировку либо null (если она дублирует одну из прошлых)
     */
    @Null
    public RestrictionItem addRestriction(final Long profileId, final short blocks, @Null Long durationInDays, int reason, String note, String admin) {
        long startDate = System.currentTimeMillis();
        final Long endDate = isInfinite(durationInDays) ? null : startDate + TimeUnit.DAYS.toMillis(durationInDays);

        List<RestrictionItem> existingRestrictions = getRestrictions(profileId);
        boolean isAlreadyBlocked = existingRestrictions.stream().anyMatch(existing -> {
            if(existing.includes(profileId, blocks, endDate)) {
                log.warn("Gamer [{}] is already restricted for {} until {}! History: {}", profileId, BlockFlag.mkString(blocks), existing.endDateToString(), Arrays.toString(existing.getHistory()));
                return true;
            }
            return false;
        });

        if(!isAlreadyBlocked) {
            final TransactionCallback<RestrictionItem> task = addRestrictionReturnTask(profileId, blocks, reason, startDate, endDate, note, admin);
            RestrictionItem result = transactionTemplate.execute(task);
            notifyProfile(profileId, getRestrictions(profileId));
            return result;
        }
        return null;
    }

    private void notifyProfile(Long profileId, List<RestrictionItem> restrictions) {
        UserProfile profile = profileService.getUserProfile(profileId);
        if(profile != null && profile.isOnline()){
            Session session = Sessions.get(profile);
            if(session != null){
                ProfileRestrictions command = new ProfileRestrictions(transformToStructures(restrictions));
                session.getConnection().send(command, serializer);
            }
        }
    }

    /**
     * Добавляет игроку новый запрет на одну функцию
     *
     * @param profileId      профиль игрока
     * @param block          флаг блокировки {@link BlockFlag}
     * @param durationInDays продолжительность в днях (null - вечный запрет)
     * @param reason         причина: один из {@link com.pragmatix.app.common.BanType} или другой, дополнительный код
     * @param note           комментарий от админа
     * @param admin          логин админа
     * @return созданную блокировку либо null (если она дублирует одну из прошлых)
     */
    public RestrictionItem addRestriction(Long profileId, BlockFlag block, @Null Long durationInDays, int reason, String note, String admin) {
        return addRestriction(profileId, block.getFlag(), durationInDays, reason, note, admin);
    }

    protected TransactionCallback<RestrictionItem> addRestrictionReturnTask(Long profileId, short blocks, int reason, long startDate, @Null Long endDate, String note, String admin) {
        List<RestrictionItem> restrictions = getRestrictionsListOrCreateNew(profileId);

        final RestrictionItem newRestriction = new RestrictionItem(profileId, blocks, startDate, endDate, reason, note, admin);
        restrictions.add(newRestriction);

        log.warn("Gamer [{}] RESTRICTED for {} until [{}] by admin [{}]", profileId, BlockFlag.mkString(blocks), newRestriction.endDateToString(), admin);

        return status -> {
            RestrictionEntity e = new RestrictionEntity(newRestriction);
            daoService.getRestrictionDao().insert(e);
            newRestriction.setId(e.getId());
            return newRestriction;
        };
    }

    /**
     * Изменяет длительность запрета, тем самым продляет, сокращает или отменяет его
     *
     * @param profileId      профиль игрока
     * @param restrictionId  id существующего запрета
     * @param durationInDays новая длительность (0 или -1 отменяет немедленно)
     * @param note           комментарий от админа
     * @param admin          логин админа
     * @return true если запрет успешно обновлен, false - если запрет не найден
     */
    public boolean changeRestrictionDuration(Long profileId, int restrictionId, @Null Long durationInDays, String note, String admin) {
        final TransactionCallback<Boolean> task = changeRestrictionDurationReturnTask(profileId, restrictionId, durationInDays, note, admin);
        if(task != null){
            transactionTemplate.execute(task);
            notifyProfile(profileId, getRestrictions(profileId));
            return true;
        }else{
            return false;
        }
    }

    @Null // <- если RestrictionItem не найден или параметры не верны
    protected TransactionCallback<Boolean> changeRestrictionDurationReturnTask(Long profileId, int restrictionId, @Null Long durationInDays, String note, String admin) {
        List<RestrictionItem> restrictions = getRestrictions(profileId);
        final RestrictionItem item = findRestriction(profileId, restrictionId);
        if(item == null) {
            log.error("Not found restriction #{} for gamer [{}] requested by admin [{}] to change. Probably it has already expired.", restrictionId, profileId, admin);
            return null;
        }
        Operation operation;
        long now = System.currentTimeMillis();
        if(isInfinite(durationInDays)) {
            // запрет стал бесконечным
            operation = Operation.PROLONG;
            item.setEndDate(null);
        } else if(durationInDays < 0) {
            // запрет снят однозначно
            operation = Operation.CANCEL;
            item.setEndDate(now);
            restrictions.remove(item);
        } else {
            long newEndDate = item.getStartDate() + TimeUnit.DAYS.toMillis(durationInDays);
            // сократили или продлили?
            operation = (item.getEndDate() == null || item.getEndDate() > newEndDate) ? Operation.REDUCE : Operation.PROLONG;
            if(newEndDate < now) {
                // уже "отсидел"
                restrictions.remove(item);
            }
            item.setEndDate(newEndDate);
        }
        item.addHistory(operation, now, note, admin);
        log.warn("Gamer [{}] restriction for {} changed: {} until [{}] by admin [{}]", profileId, BlockFlag.mkString(item.getBlocks()), operation, item.endDateToString(), admin);

        return status -> {
            daoService.getRestrictionDao().update(new RestrictionEntity(item));
            return true;
        };
    }

    /**
     * Находит запрет с данным id у данного игрока
     *
     * @param profileId     профиль игрока
     * @param restrictionId id запрета
     * @return найденный запрет, либо null если такого запрета не было, либо он уже истек
     */
    @Null
    private RestrictionItem findRestriction(Long profileId, int restrictionId) {
        List<RestrictionItem> restrictions = getRestrictions(profileId);
        for(RestrictionItem restrictionItem : restrictions) {
            if(restrictionItem.getId() == restrictionId) {
                return restrictionItem;
            }
        }
        return null;
    }

    /**
     * Возвращает список запретов, наложенных на данного игрока в настоящий момент
     * Если какой-либо из запретов уже истек, он автоматически удаляется
     *
     * @param profileId профиль игрока
     * @return список актуальных запретов
     */
    public List<RestrictionItem> getRestrictions(Long profileId) {
        List<RestrictionItem> restrictions = restrictionsByProfile.get(profileId);
        if(restrictions != null) {
            restrictions.removeIf(RestrictionItem::isExpired);
            // если это был последний запрет и он истек - совсем удаляем игрока из Map'ы
            if(restrictions.isEmpty()) {
                restrictionsByProfile.remove(profileId);
            }
            return restrictions;
        } else {
            return Collections.emptyList();
        }
    }

    public static short aggregateBlocks(List<RestrictionItem> items) {
        int blocks = 0;
        for(RestrictionItem item : items) {
            blocks |= item.getBlocks();
        }
        return (short) blocks;
    }

    // возвращает список запретов игрока для добавления в него нового запрета (т.е. создает, если его нет).
    // NB: не проверяет имеющиеся запреты на актуальность
    protected List<RestrictionItem> getRestrictionsListOrCreateNew(Long profileId) {
        return restrictionsByProfile.computeIfAbsent(profileId, k -> new CopyOnWriteArrayList<>());
    }

    /**
     * Очищает все запреты, наложенные на игрока
     *
     * @param profileId профиль игрока
     */
    public void remove(final Long profileId) {
        restrictionsByProfile.remove(profileId);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                log.warn("remove user [{}] from restrictions!", profileId);
                daoService.getRestrictionDao().deleteRestrictionsFor(profileId);
            }
        });
    }

    public static RestrictionItemStructure[] transformToStructures(List<RestrictionItem> restrictionItems) {
        if(restrictionItems.size() == 0) {
            return RestrictionItemStructure.EMPTY_ARRAY;
        }
        RestrictionItemStructure[] result = new RestrictionItemStructure[restrictionItems.size()];
        for(int i = 0; i < result.length; i++) {
            RestrictionItem restrictionItem = restrictionItems.get(i);
            RestrictionItemStructure structure = new RestrictionItemStructure();
            structure.endDate = restrictionItem.getEndDate() != null ? (int) (restrictionItem.getEndDate() / 1000L) : 0;
            structure.reason = restrictionItem.getReason();
            structure.blocks = BlockFlag.expand(restrictionItem.getBlocks());
            result[i] = structure;
        }
        return result;
    }

}
