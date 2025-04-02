package com.pragmatix.app.services.rating;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.common.RatingType;
import com.pragmatix.app.messages.server.AwardGranted;
import com.pragmatix.app.messages.server.GetRatingResult;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.app.model.ProfileDailyStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.DailyRegistry;
import com.pragmatix.app.services.ProfileBonusService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.persist.DailyRatingMapKeeper;
import com.pragmatix.app.services.persist.RatingMapKeeper;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.server.Server;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

import static com.pragmatix.app.services.rating.OldRatingService.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.05.2016 10:20
 */
public class DailyRatingService {

    public static final int PROGRESS_DEEP = 6;
    public static final int PROGRESS_MAX = 999;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private PersistenceService persistenceService;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private ProfileService profileService;

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private SoftCache softCache;

    @Resource
    private com.pragmatix.app.services.BanService banService;

    private Map<BattleWager, GenericAward> firstPlaceDailyTopAward = new HashMap<>();

    private Map<BattleWager, GenericAward> secondPlaceDailyTopAward = new HashMap<>();

    private Map<BattleWager, GenericAward> thirdPlaceDailyTopAward = new HashMap<>();

    //== Вчерашний ТОП ==
    private Map<BattleWager, YesterdayRatingData> yesterdayRatingByWager = new EnumMap<>(BattleWager.class);

    //== Текущий дневной ТОП ==
    private Map<BattleWager, DailyRatingData> dailyRatingByWager = new EnumMap<>(BattleWager.class);

    public static final String dailyRatingKeepFileName = "RatingService.dailyRating";
    public static final String dailyProgressKeepFileName = "RatingService.dailyProgress";
    public static final String yesterdayRatingKeepFileName = "RatingService.yesterdayRating";
    public static final String dailyTopKeepFileName = "RatingService.dailyTop";

    private boolean initialized = false;

    private BattleWager[] battleWagerValues;

    private boolean needAwardForTop = false;

    //Игроки, которые вошли в дневной топ50, но имеют рейтинг ниже порога не должны получать наградную шапку
    @Value("${DailyRatingService.minAwardedRatingValue:500}")
    private int minAwardedRatingValue = 500;

    public void init(BattleWager[] battleWagerValues) {
        this.battleWagerValues = battleWagerValues;

        needAwardForTop = (firstPlaceDailyTopAward.size() + secondPlaceDailyTopAward.size() + thirdPlaceDailyTopAward.size()) > 0;

        for(BattleWager battleWager : this.battleWagerValues) {
            dailyRatingByWager.put(battleWager, new DailyRatingData(battleWager));
            yesterdayRatingByWager.put(battleWager, new YesterdayRatingData(battleWager));
        }
        // пытаемся восстановить текущий прогресс (история позиции игрока в дневном топе) с диска
        for(BattleWager battleWager : this.battleWagerValues) {
            DailyRatingData ratingData = dailyRatingByWager.get(battleWager);
            try {
                Map<Long, short[]> dailyProgressMap = persistenceService.restoreObjectFromFile(Map.class, dailyProgressKeepFileName + "_" + battleWager.getId());
                if(dailyProgressMap != null && dailyProgressMap.size() > 0) {
                    Server.sysLog.info("{}: Loaded dailyProgress from file for {} profiles", battleWager, dailyProgressMap.size());
                    ratingData.dailyProgressMap = dailyProgressMap;
                }
            } catch (Exception e) {
                Server.sysLog.error(battleWager + ": Error during restore dailyProgressMap of RatingService: " + e.toString(), e);
            }
            // пытаемся восстановить текущий топ с диска
            try {
                Map<Long, Integer> restoredDailyRatingMap = persistenceService.restoreObjectFromFile(Map.class, dailyRatingKeepFileName + "_" + battleWager.getId(), new DailyRatingMapKeeper());
                if(restoredDailyRatingMap != null && restoredDailyRatingMap.size() > 0) {
                    for(Map.Entry<Long, Integer> profileRatingEntry : restoredDailyRatingMap.entrySet()) {
                        dailyRegistry.setDailyRating(profileRatingEntry.getKey(), profileRatingEntry.getValue(), battleWager);
                    }
                }

                ConcurrentSkipListSet<RatingProfileStructure> list = persistenceService.restoreObjectFromFile(ConcurrentSkipListSet.class, dailyTopKeepFileName + "_" + battleWager.getId());
                if(list != null && list.size() > 0) {
                    Server.sysLog.info("Loaded  dailyTop from file for {} profiles", list.size());
                    for(RatingProfileStructure ratingProfileStructure : list) {
                        ratingProfileStructure.clanMember = profileService.newClanMemberStructure(ratingProfileStructure.id);
                        ratingData.dailyTopMap.put(ratingProfileStructure.id, ratingProfileStructure);
                        ratingProfileStructure.oldPlace = getOldPlace(ratingProfileStructure.id, ratingData);
                    }
                    reinitDailyTop(ratingData);
                    ratingData.lastDailyRating = ratingData.dailyTop.last().ratingPoints;
                } else if(restoredDailyRatingMap != null && restoredDailyRatingMap.size() > 0) {
                    // пересчитываем ежедневный ТОП
                    reconstructDailyTop(restoredDailyRatingMap, ratingData);
                }
            } catch (Exception e) {
                Server.sysLog.error(battleWager + ": Error during restore dailyTop of RatingService: " + e.toString(), e);
                ratingData.dailyTop = new ConcurrentSkipListSet<>(new UserProfileByRatingPointsComparator());
            }
        }
        // пытаемся восстановить вчерашний топ с диска
        Map<BattleWager, Map<Long, Integer>> yesterdayRatings = new EnumMap<>(BattleWager.class);
        for(BattleWager battleWager : this.battleWagerValues) {
            try {
                Map<Long, Integer> map = persistenceService.restoreObjectFromFile(Map.class, yesterdayRatingKeepFileName + "_" + battleWager.getId(), new RatingMapKeeper("yesterday"));
                if(map == null) {
                    map = new HashMap<>();
                }
                yesterdayRatings.put(battleWager, map);
            } catch (Exception e) {
                Server.sysLog.error(battleWager + ": Error during restore yesterdayTop of RatingService: " + e.toString(), e);

                yesterdayRatings.put(battleWager, new HashMap<>());
            }
        }
        fillYesterdayPositionsAndTop(yesterdayRatings);
        initialized = true;
    }

    public void persistToDisk() {
        if(!initialized)
            return;
        for(BattleWager battleWager : this.battleWagerValues) {
            YesterdayRatingData yesterdayRatingData = yesterdayRatingByWager.get(battleWager);
            DailyRatingData dailyRatingData = dailyRatingByWager.get(battleWager);

            persistenceService.persistObjectToFile(new Object[]{dailyRegistry.getStore(), battleWager}, dailyRatingKeepFileName + "_" + battleWager.getId(), new DailyRatingMapKeeper());

            persistenceService.persistObjectToFile(yesterdayRatingData.yesterdayRating, yesterdayRatingKeepFileName + "_" + battleWager.getId(), new RatingMapKeeper("yesterday"));

            persistenceService.persistObjectToFile(dailyRatingData.dailyTop, dailyTopKeepFileName + "_" + battleWager.getId());
            Server.sysLog.info("{}: Persisted dailyTop for {} profiles", battleWager, dailyRatingData.dailyTop.size());

            persistenceService.persistObjectToFile(dailyRatingData.dailyProgressMap, dailyProgressKeepFileName + "_" + battleWager.getId());
            Server.sysLog.info("{}: Persisted dailyProgress for {} profiles", battleWager, dailyRatingData.dailyProgressMap.size());
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void storeTopPositions() {
        if(!initialized)
            return;
        for(BattleWager battleWager : this.battleWagerValues) {
            DailyRatingData ratingData = dailyRatingByWager.get(battleWager);

            List<ImmutablePair<Long, Integer>> profilesByRating = new ArrayList<>();
            for(Map.Entry<Long, ProfileDailyStructure> profileDailyStructureEntry : dailyRegistry.getStore()) {
                int dailyRating = profileDailyStructureEntry.getValue().getDailyRating(battleWager);
                profilesByRating.add(new ImmutablePair<>(profileDailyStructureEntry.getKey(), dailyRating));
            }
            Collections.sort(profilesByRating, (o1, o2) -> o2.right - o1.right);

            for(int i = 0; i < profilesByRating.size(); i++) {
                Long profileId = profilesByRating.get(i).left;
                int rating = profilesByRating.get(i).right;
                if(i < PROGRESS_MAX && rating > 0) {
                    updateTopProgresFor(profileId, i, ratingData);
                } else {
                    setEmptyPositionAndRemoveEmptiedProgress(profileId, ratingData);
                }
            }
            // обновляем позицию в топе 30 мин. назад
            for(RatingProfileStructure ratingProfileStructure : ratingData.dailyTopMap.values()) {
                ratingProfileStructure.oldPlace = getOldPlace(ratingProfileStructure.id, ratingData);
            }
        }
    }

    private void setEmptyPositionAndRemoveEmptiedProgress(Long profileId, DailyRatingData ratingData) {
        short[] progress = ratingData.dailyProgressMap.get(profileId);
        if(progress != null) {
            for(int i = 1; i < progress.length; i++) {
                if(progress[i] != 0) {
                    progress = Arrays.copyOf(progress, progress.length);
                    System.arraycopy(progress, 1, progress, 0, progress.length - 1);
                    progress[progress.length - 1] = 0;
                    ratingData.dailyProgressMap.put(profileId, progress);
                    return;
                }
            }
            ratingData.dailyProgressMap.remove(profileId);
        }
    }

    private void updateTopProgresFor(Long profileId, Integer position, DailyRatingData ratingData) {
        short[] progress = ratingData.dailyProgressMap.get(profileId);
        short positionAsShort = (short) ((position + 1) & 0x0000FFFF);
        if(progress != null) {
            progress = Arrays.copyOf(progress, progress.length);
            System.arraycopy(progress, 1, progress, 0, progress.length - 1);
            progress[progress.length - 1] = positionAsShort;
            ratingData.dailyProgressMap.put(profileId, progress);
        } else {
            progress = new short[PROGRESS_DEEP];
            Arrays.fill(progress, positionAsShort);
            ratingData.dailyProgressMap.put(profileId, progress);
        }
    }

    /**
     * @return позицию игрока в топе 30 мин. назад или 0 если он не входит в топ 1000
     */
    public int getOldPlace(Long profileId, DailyRatingData ratingByWager) {
        short[] progress = ratingByWager.dailyProgressMap.get(profileId);
        return progress == null ? 0 : progress[0] & 0x0000FFFF;
    }

    /**
     * @return позицию игрока в топе 5 мин. назад или 0 если он не входит в топ 1000
     */
    public int getCurrentPlace(Long profileId, DailyRatingData ratingByWager) {
        short[] progress = ratingByWager.dailyProgressMap.get(profileId);
        return progress == null ? 0 : progress[PROGRESS_DEEP - 1] & 0x0000FFFF;
    }

    /**
     * @return позицию игрока во вчерашнем топе или 0 если он не вошел в топ 1000
     */
    public int getYesterdayPlace(Long profileId, YesterdayRatingData ratingByWager) {
        Integer position = ratingByWager.yesterdayPositions.get(profileId);
        return position == null ? 0 : position;
    }

    private void reconstructDailyTop(Map<Long, Integer> map, DailyRatingData ratingData) {
        Server.sysLog.info(ratingData.battleWager + ": Reconstruct today TOP ...");
        TreeSet<Map.Entry<Long, Integer>> set = new TreeSet<>((o1, o2) -> {
            if(o1.getValue() > o2.getValue()) {
                return -1;
            } else if(o1.getValue() < o2.getValue()) {
                return 1;
            } else if(o1.getKey() > o2.getKey()) {
                return -1;
            } else if(o1.getKey() < o2.getKey()) {
                return 1;
            } else {
                return 0;
            }
        });
        for(Map.Entry<Long, Integer> entry : map.entrySet()) {
            set.add(entry);
        }
        int i = 0;
        for(Map.Entry<Long, Integer> entry : set) {
            Long profileId = entry.getKey();
            UserProfile profile = softCache.get(UserProfile.class, profileId);
            RatingProfileStructure profileStructure = new RatingProfileStructure(profile, profileService.clanMember_rank_skin(profile), entry.getValue(), getOldPlace(profileId, ratingData));
            ratingData.dailyTopMap.put(profileStructure.id, profileStructure);
            i++;
            if(i >= MAX_TOP) {
                break;
            }
        }
        reinitDailyTop(ratingData);
        ratingData.lastDailyRating = ratingData.dailyTop.last().ratingPoints;
    }
    // очищаем ежедневный ТОП
    public void wipeDailyTop() {
        for(BattleWager battleWager : this.battleWagerValues) {
            DailyRatingData dailyRatingData = dailyRatingByWager.get(battleWager);
            dailyRatingData.dailyTopMap = new ConcurrentHashMap<>();
            reinitDailyTop(dailyRatingData);
            dailyRatingData.lastDailyRating = 0;

            dailyRatingData.dailyProgressMap = new ConcurrentHashMap<>();
        }
    }

    public void fillYesterdayPositionsAndTop(Map<BattleWager, Map<Long, Integer>> yesterdayRatings) {
        Server.sysLog.info("fillYesterdayPositionsAndTop...");
        for(BattleWager battleWager : this.battleWagerValues) {
            YesterdayRatingData ratingData = yesterdayRatingByWager.get(battleWager);
            ratingData.yesterdayRating = yesterdayRatings.get(battleWager);

            Server.sysLog.info("{} yesterdayRatingMap.size={}", battleWager, ratingData.yesterdayRating.size());

            Map<Long, Integer> yesterdayPosition = new HashMap<>();

            List<ImmutablePair<Long, Integer>> profilesByRating = new ArrayList<>();
            for(Map.Entry<Long, Integer> profileRatingEntry : ratingData.yesterdayRating.entrySet()) {
                int dailyRating = profileRatingEntry.getValue();
                if(dailyRating > 0 && !banService.isBanned(profileRatingEntry.getKey())) {
                    profilesByRating.add(new ImmutablePair<>(profileRatingEntry.getKey(), dailyRating));
                }
            }
            profilesByRating.sort((o1, o2) -> o2.right - o1.right);

            Server.sysLog.info("{} профилей с положительным вчерашним рейтингом {}", battleWager, profilesByRating.size());
            // вчерашний ТОП
            NavigableSet<RatingProfileStructure> navigableSet = new ConcurrentSkipListSet<>(new UserProfileByRatingPointsComparator());

            List<ImmutablePair<Long, Integer>> yesterdayTop = new ArrayList<>(MAX_TOP);
            Set<Long> yesterdayTopers = new HashSet<>(MAX_TOP);
            for(int i = 0; i < profilesByRating.size() && i < PROGRESS_MAX; i++) {
                ImmutablePair<Long, Integer> profileIdDailyRatingPair = profilesByRating.get(i);
                Long profileId = profileIdDailyRatingPair.left;
                yesterdayPosition.put(profileId, i + 1);
                if(i < MAX_TOP) {
                    yesterdayTop.add(profileIdDailyRatingPair);
                    yesterdayTopers.add(profileId);
                }
            }

            Server.sysLog.info("{} грузим профили топеров [{}] ...", battleWager, yesterdayTopers.size());
            profileService.loadProfiles(yesterdayTopers, true);

            Server.sysLog.info("{} заполняем ТОП [{}] ...", battleWager, yesterdayTop.size());
            for(ImmutablePair<Long, Integer> profileIdDailyRatingPair : yesterdayTop) {
                Long profileId = profileIdDailyRatingPair.left;
                int dailyRating = profileIdDailyRatingPair.right;
                final UserProfile profile = profileService.getUserProfile(profileId);
                if(profile != null) {
                    RatingProfileStructure profileStructure = new RatingProfileStructure(profile, profileService.clanMember_rank_skin(profile), dailyRating, 0);
                    navigableSet.add(profileStructure);
                }
            }

            ratingData.yesterdayPositions = yesterdayPosition;
            ratingData.yesterdayTop = navigableSet;

            Server.sysLog.info("{} done.", battleWager);
        }
    }
    /**
     * вернет список рейтинга за вчера или за сегодня
     */
    public GetRatingResult getDailySoloRating(RatingType ratingType, BattleWager battleWager, UserProfile profile) {
        List<RatingProfileStructure> result;
        int dailyRating = 0;
        int oldPlace = 0;
        int place = 0;
        if(ratingType == RatingType.Yesterday) {
            YesterdayRatingData ratingByWager = yesterdayRatingByWager.get(battleWager);
            if(ratingByWager == null)
                return null;
            dailyRating = ratingByWager.yesterdayRating.containsKey(profile.getId()) ? ratingByWager.yesterdayRating.get(profile.getId()) : 0;
            place = getYesterdayPlace(profile.getId(), ratingByWager);
            result = new ArrayList<>(ratingByWager.yesterdayTop);
        } else if(ratingType == RatingType.Daily) {
            DailyRatingData ratingByWager = dailyRatingByWager.get(battleWager);
            if(ratingByWager == null)
                return null;
            dailyRating = dailyRegistry.getDailyRating(profile.getId(), battleWager);
            oldPlace = getOldPlace(profile.getId(), ratingByWager);
            place = getCurrentPlace(profile.getId(), ratingByWager);
            result = new ArrayList<>(ratingByWager.dailyTop);
        } else {
            throw new IllegalArgumentException("illegal ratingType param " + ratingType);
        }
        return new GetRatingResult(ratingType, battleWager, result, dailyRating, oldPlace, place);
    }
    /**
     * перестраивает топ игроков в одну операцию
     * необходимо если у игрока в топе сменился рейтинг либо в рейтинг попал новый игрок
     */
    public void reinitDailyTop(DailyRatingData ratingData) {
        NavigableSet<RatingProfileStructure> navigableSet = new ConcurrentSkipListSet<>(new UserProfileByRatingPointsComparator());
        navigableSet.addAll(ratingData.dailyTopMap.values());
        ratingData.dailyTop = navigableSet;
    }

    private RatingProfileStructure getYesterdayProfileStructure(NavigableSet<RatingProfileStructure> topList, Long profileId) {
        for(RatingProfileStructure ratingProfileStructure : topList) {
            if(ratingProfileStructure.id == profileId) {
                return ratingProfileStructure;
            }
        }
        return null;
    }

    /**
     * Добавить игрока в ежедневный ТОП или обновить в нём информацию о рейтинге
     *
     * @param profile   игрок
     * @param incRating величина на которую прибавился рейтинг
     */
    public void updateDailyTop(UserProfile profile, int incRating, BattleWager battleWager) {
        if(!initialized || battleWager == null)
            return;
        // обновляем ежедневный рейтинг игрока
        int profilesDailyRating = dailyRegistry.getDailyRating(profile.getId(), battleWager);
        // рейниг может быть отрицательным
        profilesDailyRating += incRating;
        dailyRegistry.setDailyRating(profile.getId(), profilesDailyRating, battleWager);

        DailyRatingData ratingData = dailyRatingByWager.get(battleWager);
        if(ratingData == null) //если ТОП не по всем ставкам
            return;
        // топ заполнен не полностью, добавляем с положительным рейтом, обновляем если уже в топе
        if(ratingData.dailyTop.size() < MAX_TOP) {
            if(profilesDailyRating > 0 || ratingData.dailyTopMap.get(profile.getId()) != null) {
                ratingData.dailyTopMap.put(profile.getId(), new RatingProfileStructure(profile, profileService.clanMember_rank_skin(profile), profilesDailyRating, getOldPlace(profile.getId(), ratingData)));
                reinitDailyTop(ratingData);
                ratingData.lastDailyRating = ratingData.dailyTop.last().ratingPoints;
            }
        } else {
            // добавляем или обновляем игрока в ежедневный топ
            if(profilesDailyRating > ratingData.lastDailyRating || ratingData.dailyTopMap.get(profile.getId()) != null) {
                ratingData.dailyTopMap.put(profile.getId(), new RatingProfileStructure(profile, profileService.clanMember_rank_skin(profile), profilesDailyRating, getOldPlace(profile.getId(), ratingData)));
                reinitDailyTop(ratingData);

                while (ratingData.dailyTopMap.size() > MAX_TOP) {
                    RatingProfileStructure ratingProfileStructure = ratingData.dailyTop.pollLast();
                    ratingData.dailyTopMap.remove(ratingProfileStructure.id);
                    reinitDailyTop(ratingData);
                }

                ratingData.lastDailyRating = ratingData.dailyTop.last().ratingPoints;
            }
        }
    }
    // обнулить игроку ежедневные рейтинги (сегодня, вчера)
    public void wipeDailyRatings(Long profileId) {
        if(!initialized)
            return;
        for(BattleWager battleWager : this.battleWagerValues) {
            DailyRatingData dailyRatingData = dailyRatingByWager.get(battleWager);
            int profilesDailyRating = dailyRegistry.getDailyRating(profileId, battleWager);
            if(profilesDailyRating > 0) {
                RatingProfileStructure ratingProfileStructure = dailyRatingData.dailyTopMap.get(profileId);
                // игрок входит в сегодняшний топ
                if(ratingProfileStructure != null) {
                    dailyRatingData.dailyTopMap.remove(profileId);
                    reinitDailyTop(dailyRatingData);
                    dailyRatingData.lastDailyRating = dailyRatingData.dailyTop.size() > 0 ? dailyRatingData.dailyTop.last().ratingPoints : 0;
                }
                dailyRegistry.setDailyRating(profileId, 0, battleWager);
            }
            wipeYesterdayRating(profileId);
        }

    }

    public void wipeRating(UserProfile profile) {
        if(!initialized)
            return;
        wipeDailyRating(profile);
        wipeYesterdayRating(profile.getId());
    }

    private void wipeDailyRating(UserProfile profile) {
        for(BattleWager battleWager : this.battleWagerValues) {
            int dailyRating = dailyRegistry.getDailyRating(profile.getProfileId(), battleWager);
            updateDailyTop(profile, -dailyRating, battleWager);
        }
    }

    public void wipeYesterdayRating(Long profileId) {
        for(BattleWager battleWager : this.battleWagerValues) {
            YesterdayRatingData yesterdayRatingData = yesterdayRatingByWager.get(battleWager);
            // у игрока есть рейтинг за вчера
            if(yesterdayRatingData.yesterdayRating.containsKey(profileId)) {
                // игрок входит во вчерашний топ
                RatingProfileStructure ratingProfileStructure = getYesterdayProfileStructure(yesterdayRatingData.yesterdayTop, profileId);
                if(ratingProfileStructure != null) {
                    Server.sysLog.info(battleWager + ": Wipe yesterday RatingProfileStructure {}", ratingProfileStructure);
                    ratingProfileStructure.wipe();
                }
            }
        }
    }

    public void dailyTask() {
        if(!initialized)
            return;
        Map<BattleWager, Map<Long, Integer>> yesterdayRatings = new EnumMap<>(BattleWager.class);
        for(BattleWager battleWager : this.battleWagerValues) {
            yesterdayRatings.put(battleWager, new HashMap<>());
        }
        Server.sysLog.info("сохраняем текущие рейтинги, снимаем временные шапки ...");
        int i = 0;
        for(Map.Entry<Long, ProfileDailyStructure> entry : dailyRegistry.getStore()) {
            Long profileId = entry.getKey();
            ProfileDailyStructure dailyStructure = entry.getValue();
            dailyRegistry.clearFor(profileId);

            if(dailyStructure != null) {
                i++;
                try {
                    // сохраняем текущие рейтинги
                    int[] dailyRatings = dailyStructure.getDailyRatings();
                    if(dailyRatings != null) {
                        for(BattleWager battleWager : this.battleWagerValues) {
                            int dailyRating = dailyRatings[battleWager.ordinal()];

                            if(dailyRating != 0) {
                                yesterdayRatings.get(battleWager).put(profileId, dailyRating);
                            }
                        }
                    }
                } catch (Exception e) {
                    Server.sysLog.error(e.toString(), e);
                }
            }
        }
        Server.sysLog.info("обработано {} профилей", i);
        Server.sysLog.info("заполняем вчерашний ТОП");
        // сетим текущий рейтинг как вчерашний, заполняем вчерашний ТОП
        fillYesterdayPositionsAndTop(yesterdayRatings);

        Server.sysLog.info("обнуляем текущий дневной топ");
        // обнуляем текущий дневной топ
        wipeDailyTop();
        if(needAwardForTop) {
            Server.sysLog.info("раздаём награды");
            Set<UserProfile> profilesNeedUpdate = new HashSet<>();
            // раздаём награды тем кто в топе (индивидуальный рейтинг)
            for(BattleWager battleWager : this.battleWagerValues) {
                NavigableSet<RatingProfileStructure> yesterdayTop = yesterdayRatingByWager.get(battleWager).yesterdayTop;
                int topPosition = 0;
                for(RatingProfileStructure ratingProfileStructure : yesterdayTop) {
                    topPosition++;
                    if(ratingProfileStructure.ratingPoints >= minAwardedRatingValue) {
                        UserProfile profile = profileService.getUserProfile(ratingProfileStructure.getProfileId());
                        awardForTopPosition(profile, topPosition, battleWager);
                        profilesNeedUpdate.add(profile);
                    }
                }
            }
            Server.sysLog.info("обновляем {} профилей ТОП-еров", profilesNeedUpdate.size());
            for(final UserProfile profile : profilesNeedUpdate) {
                profile.setDirty(true);
                profileService.updateSync(profile);
            }
        }
        // сохряняем вчерашние рейтинги на диск
        persistToDisk();
    }

    private void awardForTopPosition(@NotNull UserProfile profile, int topPosition, BattleWager battleWager) {
        GenericAward awardByDailyTopPosition = getAwardByDailyTopPosition(topPosition, battleWager);
        if(awardByDailyTopPosition != null) {
            AwardTypeEnum awardType = AwardTypeEnum.DAILY_TOP;
            List<GenericAwardStructure> awardStructures = profileBonusService.awardProfile(awardByDailyTopPosition, profile, awardType, String.valueOf(topPosition));
            if(!awardStructures.isEmpty()) {
                AwardGranted message = new AwardGranted(awardType, Collections.singletonList(awardStructures.get(0)), "" + battleWager.getId() + "," + topPosition, Sessions.getKey(profile));
                boolean sendResult = Messages.toUser(message, profile);
                if(!sendResult) {
                    dailyRegistry.addOfflineAward(profile.getId(), new LoginAwardStructure(message));
                }
            }
        } else if(topPosition <= MAX_TOP) {
            log.error("не найдена награда за {} место в ТОП-е за бой {}", topPosition, battleWager);
        }
    }

    @Null
    private GenericAward getAwardByDailyTopPosition(int topPosition, BattleWager battleWager) {
        if(topPosition == 1) {
            return firstPlaceDailyTopAward.get(battleWager);
        } else if(topPosition > 1 && topPosition <= 10) {
            return secondPlaceDailyTopAward.get(battleWager);
        } else if(topPosition > 10 && topPosition <= MAX_TOP) {
            return thirdPlaceDailyTopAward.get(battleWager);
        } else {
            return null;
        }
    }

    public void onCloseClanSeason() {
        for(YesterdayRatingData yesterdayRatingData : yesterdayRatingByWager.values()) {
            for(RatingProfileStructure ratingProfileStructure : yesterdayRatingData.yesterdayTop) {
                UserProfile profile = profileService.getUserProfile(ratingProfileStructure.id);
                ratingProfileStructure.clanMember = profileService.newClanMemberStructure(profile);
            }
        }
    }

    public void onRename(final UserProfile profile) {
        // меняем имя в дневном ТОП-е
        for(DailyRatingData dailyRatingData : dailyRatingByWager.values()) {
            RatingProfileStructure ratingProfileStructure = dailyRatingData.dailyTopMap.get(profile.getId());
            if(ratingProfileStructure != null) {
                ratingProfileStructure.name = profile.getName();
            }
        }
    }

    public void onSetSeasonRating(final UserProfile profile, Function<UserProfile, Byte> getRank) {
        // меняем имя в дневном ТОП-е
        for(DailyRatingData dailyRatingData : dailyRatingByWager.values()) {
            RatingProfileStructure ratingProfileStructure = dailyRatingData.dailyTopMap.get(profile.getId());
            if(ratingProfileStructure != null) {
                ratingProfileStructure.rank = getRank.apply(profile);
            }
        }
    }

    public void setFirstPlaceDailyTopAward(Map<BattleWager, GenericAward> firstPlaceDailyTopAward) {
        this.firstPlaceDailyTopAward = firstPlaceDailyTopAward;
    }

    public void setSecondPlaceDailyTopAward(Map<BattleWager, GenericAward> secondPlaceDailyTopAward) {
        this.secondPlaceDailyTopAward = secondPlaceDailyTopAward;
    }

    public void setThirdPlaceDailyTopAward(Map<BattleWager, GenericAward> thirdPlaceDailyTopAward) {
        this.thirdPlaceDailyTopAward = thirdPlaceDailyTopAward;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Map<BattleWager, DailyRatingData> getDailyRatingByWager() {
        return dailyRatingByWager;
    }
}
