package com.pragmatix.app.services;

import com.pragmatix.app.common.BoostFamily;
import com.pragmatix.app.common.ItemType;
import com.pragmatix.app.messages.server.PumpReactionRateResult;
import com.pragmatix.app.messages.structures.PumpReactionRateStructure;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.app.model.ReactionConf;
import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum;
import com.pragmatix.app.services.persist.WhoPumpedReactionKeeper;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.services.DailyTaskAvailable;
import com.pragmatix.gameapp.services.IServiceTask;
import com.pragmatix.gameapp.services.TaskService;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.server.Server;
import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Сервис хранит информацию кто кому сегодня прокачал скорость реакции
 * <p/>
 * Created: 29.04.11 14:07
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
@Service
public class ReactionRateService implements DailyTaskAvailable {

    private static final Logger logger = LoggerFactory.getLogger(ReactionRateService.class);
    /**
     * скольким друзьям ты можеш прокачать реакцию в течении дня
     */
    private int maxPumpedFriendsByDay = 100;
    /**
     * сколько дней храним информацию о прокачках реакции
     */
    private static final int maxDaysStore = 3;
    /**
     * Отслеживает кто кому прокачивал реакцию (храним данные за сегодня + 2 дня),
     * дабы проконтролировать что один игрок сегодня прокачал рейтин другому игроку только один раз
     */
    private List<Map<Long, Object>> whoPumpedRateByDays;

    @Resource
    private SoftCache softCache;

    @Resource
    private DaoService daoService;

    @Resource
    private TaskService taskService;

    @Resource
    private PersistenceService persistenceService;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private StatisticService statisticService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private StuffService stuffService;

    @Value("${debug.disableValidatePumpReaction:false}")
    private boolean debugDisableValidatePumpReaction = false;

    public static final String keepFileNameByDay = "ReactionRateService.whoPumpedRateByDay";

    private boolean initialized = false;

    private int maxConfiguredReactionLevel = 0;
    private int maxReactionLevel = 1;

    private Map<Integer, ReactionConf> reactionLevelsConf;

    @Value("#{reactionRateConf}")
    public void setReactionRateConf(List<ReactionConf> reactionRateConf) {
        reactionLevelsConf = new HashMap<>();
        for(ReactionConf reactionConf : reactionRateConf) {
            reactionLevelsConf.put(reactionConf.level, reactionConf);
        }

        maxReactionLevel = Collections.max(reactionLevelsConf.keySet());
        for(Map.Entry<Integer, ReactionConf> levelConfEntry : reactionLevelsConf.entrySet()) {
            if(levelConfEntry.getValue().price > 0)
                maxConfiguredReactionLevel = Math.max(maxConfiguredReactionLevel, levelConfEntry.getKey());
        }
    }

    public void init() {
        // пытаемся восстановить состояние с диска
        try {
            CopyOnWriteArrayList<Map<Long, Object>> list = new CopyOnWriteArrayList<>();
            for(int i = 0; i < maxDaysStore; i++) {
                int forDate = -i;
                Map<Long, Object> defaultMap = i == 0 ? new ConcurrentHashMap<>() : new HashMap<>();
                Map<Long, Object> map = persistenceService.restoreObjectFromFile(Map.class, keepFileNameByDay + forDate, new WhoPumpedReactionKeeper(forDate), defaultMap);
                list.add(map);
            }
            whoPumpedRateByDays = list;
        } catch (Exception e) {
            Server.sysLog.error("Error during restore state of ReactionRateService: " + e.toString(), e);
        }

        // во время сосстановления произошли ошибки, или на диске отсутствуют данные
        if(whoPumpedRateByDays == null) {
            Server.sysLog.error("State of ReactionRateService is empty");
            initByDefault();
        }

        initialized = true;
    }

    // очищаем кэш контролирующий прокачку скорость реакции
    private IServiceTask dailyTask = new IServiceTask() {
        @Override
        public void runServiceTask() {
            // сдвигаем
            for(int i = maxDaysStore - 2; i >= 0; i--) {
                Map<Long, Object> rateTrackMap = whoPumpedRateByDays.get(i);
                if(i == 0) {
                    // данные readonly нет смысла держать в конкурентных структурах
                    rateTrackMap = unConcurrentTheMap(rateTrackMap);
                }
                whoPumpedRateByDays.set(i + 1, rateTrackMap);
            }
            // текущий день начинаем с чистого листа
            Map<Long, Object> rateTrackMap = new ConcurrentHashMap<>();
            whoPumpedRateByDays.set(0, rateTrackMap);

            // сохраняем состояние на диск
            persistToDisk();
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    };

    public IServiceTask getDailyTask() {
        return dailyTask;
    }

    public void initByDefault() {
        whoPumpedRateByDays = new CopyOnWriteArrayList<>();
        // данные только за сегодня размещяем в конкурентных структурах
        whoPumpedRateByDays.add(new ConcurrentHashMap<>());
        for(int i = 1; i < maxDaysStore; i++) {
            whoPumpedRateByDays.add(new HashMap<>());
        }
    }

    public void persistToDisk() {
        if(initialized) {
            int forDate = 0;
            for(Map<Long, Object> whoPumpedRateByDay : whoPumpedRateByDays) {
                persistenceService.persistObjectToFile(whoPumpedRateByDay, keepFileNameByDay + forDate, new WhoPumpedReactionKeeper(forDate));
                forDate--;
            }
        }
    }

    // заменяем ConcurrentHashMap на HashMap, ConcurrentHashSet<Long> -> arr[int]
    private Map<Long, Object> unConcurrentTheMap(Map<Long, Object> rateTrackMap) {
        Map<Long, Object> result = new HashMap<>();
        for(Map.Entry<Long, Object> profileIdSetEntry : rateTrackMap.entrySet()) {
            Set<Long> set = (Set<Long>) profileIdSetEntry.getValue();
            int[] arr = new int[set.size()];
            int i = 0;
            for(Long id : set) {
                arr[i] = id.intValue();
                i++;
            }
            result.put(profileIdSetEntry.getKey(), arr);
        }
        return result;
    }

    public PumpReactionRateStructure[] pumpReactionRates(final UserProfile profile, long[] friendIds) {
        final List<PumpReactionRateStructure> resultList = new ArrayList<>(friendIds.length);

        // скольким друзьям он уже сегодня прокачал реакцию
        byte todayPumpedFriends = dailyRegistry.getHowManyPumped(profile.getId());

        Map<Long, Object> whoPumpedToday = whoPumpedRateByDays.get(0);

        final Map<Long, Byte> notInCacheFriends = new HashMap<>();
        final List<UserProfile> offlineFriends = new ArrayList<>();

        for(long friendId : friendIds) {
            // самому себе прокачивать нельзя
            if(profile.getId() == friendId) {
                continue;
            }

            PumpReactionRateStructure resultItem = new PumpReactionRateStructure();
            resultList.add(resultItem);
            resultItem.friendId = friendId;

            // если превышен лимит - то в сад
            if(todayPumpedFriends >= maxPumpedFriendsByDay) {
                resultItem.result = (byte) PumpReactionRateResult.ResultEnum.lIMIT_EXEEDED.getType();
                //как только достигли предела, дальше не идем ибо смысл
                break;
            }

            // кто сегодня уже прокачивал реакцию игроку
            Set<Long> friendsSet = (Set<Long>) whoPumpedToday.get(friendId);
            if(friendsSet == null) {
                friendsSet = new ConcurrentHashSet<>();
                whoPumpedToday.put(friendId, friendsSet);
            }
            if(friendsSet.contains(profile.getId())) {
                resultItem.result = (byte) PumpReactionRateResult.ResultEnum.TODAY_ALREADY_PUMPED.getType();
                continue;
            }

            if(friendsSet.add(profile.getId())) {
                resultItem.result = (byte) PumpReactionRateResult.ResultEnum.OK.getType();
                todayPumpedFriends = (byte) (todayPumpedFriends + 1);

                //загружаем профайл друга которому хотим прокачать реакцию, только из кеша
                final UserProfile friendProfile = softCache.get(UserProfile.class, friendId, false);
                // друг был найден в кеше
                if(friendProfile != null) {
                    friendProfile.setReactionRate(friendProfile.getReactionRate() + 1);
                    firePumpReactionEvent(profile, friendProfile, todayPumpedFriends);
                    //будем сохранять в БД инфу о том, что скорость реакции прокачена если игрока нету в онлайн
                    if(!friendProfile.isOnline()) {
                        offlineFriends.add(friendProfile);
                    }
                } else {
                    //будем инкрементировать скорость реакции в БД если игрок не загружен в кеш
                    notInCacheFriends.put(friendId, todayPumpedFriends);
                }
            } else {
                // теоритически такого быть не должно, но на всякий случай
                resultItem.result = (byte) PumpReactionRateResult.ResultEnum.TODAY_ALREADY_PUMPED.getType();
            }
        }

        dailyRegistry.setHowManyPumped(profile.getId(), todayPumpedFriends);

        // даем задачу на пакетное обновление профилй игроков
        final TransactionCallback transactionTask = new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus status) {

                Set<Long> profilesToUpdate = new ConcurrentHashSet<>();
                profilesToUpdate.addAll(notInCacheFriends.keySet());

                for(Long notInCacheFriendId : notInCacheFriends.keySet()) {
                    UserProfile friendProfile = softCache.get(UserProfile.class, notInCacheFriendId, false);
                    // игрок был загружен в кеш, пока задача ожидала своей очереди
                    if(friendProfile != null) {
                        friendProfile.setReactionRate(friendProfile.getReactionRate() + 1);
                        byte pumpedToday = notInCacheFriends.get(notInCacheFriendId);
                        firePumpReactionEvent(profile, friendProfile, pumpedToday);
                        if(friendProfile.isOnline()) {
                            // если он online обновлять в базе не будем
                            profilesToUpdate.remove(notInCacheFriendId);
                        }
                    }
                }

                for(UserProfile offlineFriend : offlineFriends) {
                    // если игрок всё еще offline
                    if(!offlineFriend.isOnline()) {
                        profilesToUpdate.add(offlineFriend.getId());
                    }
                }

                int result = daoService.getUserProfileDao().increaseReactionRates(profilesToUpdate);

                if(logger.isDebugEnabled()) {
                    logger.debug("increased reaction for {} profiles from {}", result, profilesToUpdate.size());
                }

                return null;
            }
        };
        if(notInCacheFriends.size() + offlineFriends.size() > 0) {
            taskService.addTransactionTask(transactionTask);
        }

        return resultList.toArray(new PumpReactionRateStructure[resultList.size()]);
    }

    private void firePumpReactionEvent(UserProfile profile, UserProfile friendProfile, byte pumpedToday) {
        profileEventsService.fireProfileEventAsync(ProfileEventEnum.PUMP_REACTION, friendProfile,
                Param.profile_reaction, friendProfile.getReactionRate(),
                Param.profile_reactionLevel, getReactionLevel(friendProfile.getReactionRate()),
                Param.reaction, 1,
                Param.friendId, profile.getId(),
                "friendPumpedToday", pumpedToday
        );
    }

    public PumpReactionRateResult.ResultEnum pumpReactionRate(UserProfile profile, long friendId) {
        PumpReactionRateResult.ResultEnum result;
        if(profile.getId() == friendId) {
            // самому себе прокачивать нельзя
            return PumpReactionRateResult.ResultEnum.ERROR;
        }
        // скольким друзьям он уже сегодня прокачал реакцию
        byte todayPumpedFriends = dailyRegistry.getHowManyPumped(profile.getId());

        // если превышен лимит - то в сад
        if(todayPumpedFriends >= maxPumpedFriendsByDay) {
            return PumpReactionRateResult.ResultEnum.lIMIT_EXEEDED;
        }

        Map<Long, Object> rateTrackMap = whoPumpedRateByDays.get(0);

        // кто сегодня уже прокачивал реакцию игроку
        Set<Long> friendsSet = (Set<Long>) rateTrackMap.get(friendId);
        if(friendsSet == null) {
            friendsSet = new ConcurrentHashSet<>();
            rateTrackMap.put(friendId, friendsSet);
        }
        if(friendsSet.contains(profile.getId()) && !debugDisableValidatePumpReaction) {
            return PumpReactionRateResult.ResultEnum.TODAY_ALREADY_PUMPED;
        }

        //загружаем профайл друга кому хотим прокачать реакцию
        final UserProfile friendProfile = softCache.get(UserProfile.class, friendId);
        if(friendProfile == null) {
            logger.error("can't pump reaction rate cause of user profile not found by id: {}", friendId);
            result = PumpReactionRateResult.ResultEnum.ERROR;
        } else {
            result = PumpReactionRateResult.ResultEnum.OK;
            if(friendsSet.add(profile.getId()) || debugDisableValidatePumpReaction) {

                friendProfile.setReactionRate(friendProfile.getReactionRate() + 1);
                byte pumpedToday = (byte) (todayPumpedFriends + 1);
                firePumpReactionEvent(profile, friendProfile, pumpedToday);
                dailyRegistry.setHowManyPumped(profile.getId(), pumpedToday);
                //сохроняем в БД инфу о том, что скорость реакции прокачена если игрока нету в онлайн
                if(!friendProfile.isOnline()) {
                    daoService.doInTransactionWithoutResult(() -> daoService.getUserProfileDao().setReactionRate(friendProfile.getId(), friendProfile.getReactionRate()));
                }
            } else {
                // теоритически такого быть не должно, но на всякий случай
                result = PumpReactionRateResult.ResultEnum.TODAY_ALREADY_PUMPED;
            }
        }
        return result;
    }

    public List<List<Long>> getWhoPumped(UserProfile profile, boolean todayOnly) {
        List<List<Long>> result = new ArrayList<>();
        for(Map<Long, Object> whoPumpedMap : whoPumpedRateByDays) {
            Object whoPumpedInDay = whoPumpedMap.get(profile.getId());
            if(whoPumpedInDay != null) {
                if(whoPumpedInDay instanceof Set) {
                    result.add(new ArrayList<>((Set<Long>) whoPumpedInDay));
                } else if(whoPumpedInDay instanceof int[]) {
                    int[] arr = (int[]) whoPumpedInDay;
                    ArrayList<Long> list = new ArrayList<>();
                    for(int id : arr) {
                        list.add((long) id);
                    }
                    result.add(list);
                } else {
                    logger.error("unexpected class of whoPumpedInDay [{}]", whoPumpedInDay);
                    result.add(new ArrayList<>());
                }
            } else {
                result.add(new ArrayList<Long>());
            }
            if(todayOnly) {
                break;
            }
        }
        return result;
    }

    public ShopResultEnum buyReactionRate(int level, UserProfile profile) {
        if(profile.getReactionRate() >= valueForLevel(maxConfiguredReactionLevel)) {
            logger.error("игрок уже имеет максимальное количество реакции которое можно купить за рубины! reaction={}; level={}",
                    profile.getReactionRate(), getReactionLevel(profile.getReactionRate()));
            return ShopResultEnum.ERROR;
        }
        int currentReactionLevel = getReactionLevel(profile.getReactionRate());
        if(currentReactionLevel + 1 != level) {
            logger.error("можно купить только следующий уровень реакции! reaction={}; level={}", profile.getReactionRate(), currentReactionLevel);
            return ShopResultEnum.ERROR;
        }
        int newReactionValue = valueForLevel(level);
        float factor = (float) (newReactionValue - profile.getReactionRate()) / (float) (newReactionValue - valueForLevel(currentReactionLevel));
        int needRealMoney = Math.max(1, (int) Math.ceil(priceForLevel(level) * factor));
        if(profile.getRealMoney() < needRealMoney) {
            logger.error("не хватает рубинов! factor={}; needRealMoney={}", factor, needRealMoney);
            return ShopResultEnum.NOT_ENOUGH_MONEY;
        }
        profile.setRealMoney(profile.getRealMoney() - needRealMoney);
        int oldReactionValue = profile.getReactionRate();
        profile.setReactionRate(newReactionValue);

        profileEventsService.fireProfileEventAsync(ProfileEventEnum.PURCHASE, profile,
                Param.eventType, ItemType.REACTION_LEVEL,
                Param.profile_reaction, profile.getReactionRate(),
                Param.profile_reactionLevel, level,
                Param.realMoney, -needRealMoney,
                Param.reaction, newReactionValue - oldReactionValue
        );
        statisticService.buyItemStatistic(profile.getId(), MoneyType.REAL_MONEY.getType(), needRealMoney, ItemType.REACTION_LEVEL, newReactionValue - oldReactionValue, level, profile.getLevel());
        return ShopResultEnum.SUCCESS;
    }

    public int getReactionLevel(int reactionRate) {
        if(reactionRate >= valueForLevel(maxReactionLevel)) {
            return maxReactionLevel;
        } else {
            int currentReactionLevel = 0;
            for(Map.Entry<Integer, ReactionConf> levelValueEntry : reactionLevelsConf.entrySet()) {
                int valueForLevel = levelValueEntry.getValue().getValue();
                int valueForNextLevel = valueForLevel(levelValueEntry.getKey() + 1);
                if(reactionRate >= valueForLevel && reactionRate < valueForNextLevel) {
                    currentReactionLevel = levelValueEntry.getKey();
                    break;
                }
            }
            return currentReactionLevel;
        }
    }

    private int priceForLevel(int level) {
        return reactionLevelsConf.get(level).price;
    }

    private int valueForLevel(int level) {
        return reactionLevelsConf.get(level).value;
    }

    public int[] getReactionLevel(UserProfileStructure profileStructure) {
        int baseLevel = getReactionLevel(profileStructure.reactionRate);
        int[] extraLevel = getStuffsReaction(profileStructure.wormsGroup());
        int reactionBoostValue = getReactionBoostValue(profileStructure);
        if(reactionBoostValue == 1) {
            baseLevel += Math.max(12 - baseLevel, 1);
        } else if(reactionBoostValue == 2) {
            baseLevel += Math.max(18 - baseLevel, 2);
        }
        return new int[]{baseLevel + extraLevel[0], extraLevel[1]};
    }

    private int getReactionBoostValue(UserProfileStructure profileStructure) {
        return Math.max(stuffService.getBoostValue(profileStructure, BoostFamily.AddReaction), stuffService.getBoostValue(profileStructure, BoostFamily.AddReactionHight));
    }

    /**
     * вернет доп. реакцию предметов
     */
    public int[] getStuffsReaction(WormStructure wormStructure) {
        return ReactionRateService.sumReaction(getStuffReaction(wormStructure.hat), getStuffReaction(wormStructure.kit));
    }

    public int[] getStuffsReaction(WormStructure[] wormStructures) {
        int[] result = {0, 0};
        for(WormStructure wormStructure : wormStructures) {
            if(WormStructure.isActive(wormStructure)){
                result = sumReaction(result, getStuffsReaction(wormStructure));
            }
        }
        return result;
    }

    private int[] getStuffReaction(short stuffId) {
        if(stuffId > 0) {
            Stuff item = stuffService.getStuff(stuffId);
            if(item != null) {
                return item.getReaction() > 0 ? new int[]{item.getReaction(), 0} : new int[]{0, item.getReaction()};
            }
        }
        return new int[]{0, 0};
    }

    public static int[] sumReaction(int[] r1, int[] r2) {
        return new int[]{r1[0] + r2[0], r1[1] + r2[1]};
    }

    public static int compareReaction(int[] r1, int[] r2) {
        //Бонусы реакции на предметах http://jira.pragmatix-corp.com/browse/WORMIX-4137
        int r1_value = Math.max(1, r1[0] + r2[1]);
        int r2_value = Math.max(1, r2[0] + r1[1]);
        return r2_value - r1_value;
    }

//====================== Getters and Setters =================================================================================================================================================

    public void setSoftCache(SoftCache softCache) {
        this.softCache = softCache;
    }

    public List<Map<Long, Object>> getWhoPumpedRateByDays() {
        return whoPumpedRateByDays;
    }

    public void setPersistenceService(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public void setDailyRegistry(DailyRegistry dailyRegistry) {
        this.dailyRegistry = dailyRegistry;
    }

    public Map<Integer, ReactionConf> getReactionLevelsConf() {
        return reactionLevelsConf;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
