package com.pragmatix.app.services;

import com.google.gson.Gson;
import com.pragmatix.app.common.*;
import com.pragmatix.app.domain.TrueSkillEntity;
import com.pragmatix.app.init.LevelCreator;
import com.pragmatix.app.messages.client.EndBattle;
import com.pragmatix.app.messages.client.EndTurn;
import com.pragmatix.app.messages.server.EndBattleResult;
import com.pragmatix.app.messages.structures.*;
import com.pragmatix.app.model.ProfileDailyStructure;
import com.pragmatix.app.model.SimpleBattleStateStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.WagerDef;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.services.persist.MapIntegerByteKeeper;
import com.pragmatix.app.services.rating.RatingService;
import com.pragmatix.app.settings.*;
import com.pragmatix.arena.coliseum.ColiseumService;
import com.pragmatix.arena.mercenaries.MercenariesService;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.common.utils.VarInt;
import com.pragmatix.common.utils.VarObject;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.gameapp.GameApp;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.gameapp.sessions.Connection;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.intercom.messages.EndPvpBattleRequest;
import com.pragmatix.notify.NotifyEvent;
import com.pragmatix.notify.NotifyEventType;
import com.pragmatix.notify.NotifyService;
import com.pragmatix.performance.statictics.StatCollector;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.quest.QuestService;
import io.vavr.Tuple2;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.pragmatix.app.common.PvpBattleResult.WINNER;
import static com.pragmatix.app.common.ServerRole.ALL;
import static com.pragmatix.app.common.ServerRole.MAIN;
import static com.pragmatix.app.common.WhichLevelEnum.MY_LEVEL;
import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.*;
import static com.pragmatix.app.settings.AppParams.IS_NOT_MOBILE;
import static com.pragmatix.app.settings.AppParams.versionToString;
import static com.pragmatix.common.utils.AppUtils.currentTimeSeconds;

/**
 * сервис со вспомогаельными методами для боёвки
 * User: denis
 * Date: 10.12.2009
 * Time: 3:13:25
 */
@Service
public class BattleService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Logger battlesCdrlogger = LoggerFactory.getLogger("BATTLES_CDR_LOGGER");

    private final Gson gson = new Gson();

    /**
     * Счетчик боёв
     */
    private AtomicLong battleCount = new AtomicLong(0);

    public static final int MAX_BATTLE_COUNT = 5;

    @Resource
    private DailyRegistry dailyRegistry;

    @Value("${debug.disableValidateMission:false}")
    private boolean disableValidateMission;

    @Resource
    private RatingService ratingService;

    @Resource
    private WeaponService weaponService;

    @Resource
    private CraftService craftService;

    @Resource
    private BattleAwardSettings battleAwardSettings;

    @Resource
    private HeroicMissionService heroicMissionService;

    @Resource
    private ProfileExperienceService profileExperienceService;

    @Resource
    private StatCollector statCollector;

    @Resource
    private StatisticService statisticService;

    @Resource
    private TrueSkillService trueSkillService;

    @Resource
    private ProfileService profileService;

    @Resource
    private AppParams appParams;

    @Resource
    private PersistenceService persistenceService;

    @Resource
    private CheatersCheckerService cheatersCheckerService;

    @Autowired(required = false)
    private ColiseumService coliseumService;

    @Autowired(required = false)
    private MercenariesService mercenariesService;

    @Resource
    private QuestService questService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private LevelCreator levelCreator;

    public long simpleBattleReconnectTimeoutInSeconds = 60;

    @Value("${BattleService.simpleBattleReconnectEnabled:true}")
    public boolean simpleBattleReconnectEnabled = true;

    public final ConcurrentMap<Long, SimpleBattleStateStructure> disconnectedInSimpleBattle = new ConcurrentHashMap<>();

    @Value("#{pvpExperience}")
    public Map<BattleWager, Integer> pvpExperience;

    @Value("#{battleAwardSettings.awardSettingsMap}")
    private Map<Short, SimpleBattleSettings> awardSettingsMap;

    private MapIntegerByteKeeper flagsKeeper = new MapIntegerByteKeeper("DailyRegistry.nonEmptyFlags");
    private MapIntegerByteKeeper bossTokensKeeper = new MapIntegerByteKeeper("DailyRegistry.bossTokens");
    private MapIntegerByteKeeper wagerTokensKeeper = new MapIntegerByteKeeper("DailyRegistry.wagerTokens");

    private Map<BattleType, AtomicInteger> battlesCounters = new EnumMap<>(BattleType.class);

    @Resource
    private NotifyService notifyService;

    @Resource
    private GameApp gameApp;

    @Resource
    private CheatersCheckerService cheatersCheakerService;

    @Resource
    private BossBattleExtraRewardService bossBattleExtraRewardService;

    private int simpleBattleMaxAwardedStars = 5;
    private int simpleBattleMoneyPerStar = 4;

    private Map<BattleWager, Map<Integer, WagerDef>> battleWagerDefinitions;

    @Value("#{awardForDroppedUnits}")
    private List<Integer> awardForDroppedUnits;

    private int firstWagerWinAwardMoney = 100;
    private int firstWagerWinAwardExp = 5;

    @Value("${server.role:ALL}")
    private ServerRole serverRole;

    private boolean initialized = false;

    @Value("#{battleWagerDefinitions}")
    public void setBattleWagerDefinitions(List<WagerDef> wagerDefsList) {
        Map<BattleWager, Map<Integer, WagerDef>> wagerDefs = new EnumMap<>(BattleWager.class);
        for (WagerDef wagerDef : wagerDefsList) {
            if (!wagerDefs.containsKey(wagerDef.wager)) {
                wagerDefs.put(wagerDef.wager, new HashMap<>(4));
            }
            if (wagerDef.team > 0) {
                wagerDefs.get(wagerDef.wager).put(wagerDef.team, wagerDef);
            } else {
                for (int i = 1; i <= 4; i++) {
                    wagerDefs.get(wagerDef.wager).put(i, wagerDef.setTeam(i));
                }
            }
        }
        this.battleWagerDefinitions = wagerDefs;
    }

    public void init() {
        for (BattleType battleType : BattleType.values()) {
            AtomicInteger value = new AtomicInteger();
            battlesCounters.put(battleType, value);
            statCollector.needCollect("battles", battleType.name(), value::get, false);
        }
        // пытаемся восстановить состояние с диска
        try {
            ((Map<Integer, Byte>) persistenceService.restoreObjectFromFile(Map.class, flagsKeeper.name, flagsKeeper, Collections.emptyMap()))
                    .forEach((profileId, value) -> dailyRegistry.setFlags((long) profileId, value));
        } catch (Exception e) {
            log.error("Error during restore state of BattleService: " + e.toString(), e);
        }
        persistenceService.rename(flagsKeeper.name);

        try {
            ((Map<Integer, Byte>) persistenceService.restoreObjectFromFile(Map.class, bossTokensKeeper.name, bossTokensKeeper, Collections.emptyMap()))
                    .forEach((profileId, value) -> dailyRegistry.setBossWinAwardToken((long) profileId, value));
        } catch (Exception e) {
            log.error("Error during restore state of BattleService: " + e.toString(), e);
        }
        persistenceService.rename(bossTokensKeeper.name);

        try {
            ((Map<Integer, Byte>) persistenceService.restoreObjectFromFile(Map.class, wagerTokensKeeper.name, wagerTokensKeeper, Collections.emptyMap()))
                    .forEach((profileId, value) -> dailyRegistry.setWagerWinAwardToken((long) profileId, value));
        } catch (Exception e) {
            log.error("Error during restore state of BattleService: " + e.toString(), e);
        }
        persistenceService.rename(wagerTokensKeeper.name);

        initialized = true;
    }

    @Scheduled(cron = "*/5 * * * * *")
    public void cronTask() {
        if (!initialized) {
            return;
        }
        if (serverRole == MAIN || serverRole == ALL) {
            trackDisconnectedProfiles();

            Map<BattleType, VarInt> counters = Arrays.stream(BattleType.values()).collect(Collectors.toMap(type -> type, type -> new VarInt()));
            gameApp.getSessions().forEach(session -> {
                Connection connection = session.getConnection();
                if (connection != null && connection.getIndex() == 0) {
                    UserProfile profile = (UserProfile) session.getUser();
                    if (profile.isOnline()) {
                        BattleState battleState = profile.getBattleState();
                        if (battleState == BattleState.SIMPLE) {
                            counters.get(BattleType.valueOfMissionId(profile.getMissionId())).value++;
                        } else if (battleState == BattleState.IN_BATTLE_PVP) {
                            counters.get(BattleType.valueOfPvpBattleType(profile.pvpBattleType)).value++;
                        } else if (battleState == BattleState.WAIT_START_BATTLE) {
                            counters.get(BattleType.WAIT_START_BATTLE).value++;
                        } else {
                            counters.get(BattleType.NOT_IN_BATTLE).value++;
                        }
                    }
                }
            });
            for (BattleType battleType : BattleType.values()) {
                battlesCounters.get(battleType).set(counters.get(battleType).value);
            }
        }
    }

    public void persistToDisk() {
        Map<Integer, Byte> nonEmptyFlags = new HashMap<>();
        Map<Integer, Byte> bossTokens = new HashMap<>();
        Map<Integer, Byte> wagerTokens = new HashMap<>();

        dailyRegistry.store.forEach((profileId, profileDailyStructure) -> {
            if (profileDailyStructure.getFlags() > 0) {
                nonEmptyFlags.put(profileId.intValue(), profileDailyStructure.getFlags());
            }
            if (profileDailyStructure.getBossWinAwardToken() != ProfileDailyStructure.BossWinAwardTokenDefault) {
                bossTokens.put(profileId.intValue(), (byte) profileDailyStructure.getBossWinAwardToken());
            }
            if (profileDailyStructure.getWagerWinAwardToken() != ProfileDailyStructure.WagerWinAwardTokenDefault) {
                wagerTokens.put(profileId.intValue(), (byte) profileDailyStructure.getWagerWinAwardToken());
            }
        });

        persistenceService.persistObjectToFile(nonEmptyFlags, flagsKeeper.name, flagsKeeper);
        persistenceService.persistObjectToFile(bossTokens, bossTokensKeeper.name, bossTokensKeeper);
        persistenceService.persistObjectToFile(wagerTokens, wagerTokensKeeper.name, wagerTokensKeeper);
    }

    public WagerDef getBattleWagerDef(BattleWager wager, int teamSize) {
        if (!battleWagerDefinitions.containsKey(wager))
            throw new IllegalStateException("определение для ставки " + wager + " не найдено!");
        Map<Integer, WagerDef> wagersByTeam = battleWagerDefinitions.get(wager);
        if (!wagersByTeam.containsKey(teamSize))
            throw new IllegalStateException("определение для ставки " + wager + " и размера команды " + teamSize + " не найдено!");

        return wagersByTeam.get(teamSize);
    }

    public void onEndPvpBattle(EndPvpBattleRequest msg, final UserProfile profile, List<GenericAwardStructure> award) {
        profile.pvpBattleType = null;
        if (msg.battleId > 0 && msg.battleId == profile.getLastProcessedPvpBattleId()) {
            log.error("команда прислана повторно {}", msg);
            return;
        }
        boolean cheater = msg.banType > 0;
        if (cheater) {
            msg.collectedReagents = new byte[0];
        }
        String battleTime = PvpService.formatTimeInSeconds(AppUtils.currentTimeSeconds() - profile.pvpChangeStateTime);

        if (msg.battleType.isRewardBattle()) {

            byte[] collectedReagents = new byte[0];

            // PVP бой на ставку
            if (msg.wager.getValue() > 0) {
                endPvpBattleGrantAward(profile, msg, award);

                // корректность реагентов была проверена pvp сервером
                collectedReagents = msg.collectedReagents;
                msg.battleAward.collectedReagents = collectedReagents;

                profileEventsService.fireProfileEventAsync(END_PVP_BATTLE, profile, Param.endPvpBattleMessage, msg, Param.battleTime, battleTime);
                if (IS_NOT_MOBILE()) {
                    //За каждую победу на ставках игрок получает сундук, содержащий фузы и случайные ресурсы http://jira.pragmatix-corp.com/browse/WORMIX-4387
                    questService.consumeBattleResult(profile, (short) 1, msg.result, battleTime);
                }
            } else if (ArrayUtils.isNotEmpty(msg.missionIds)) {
                // совместное прохождение боссов

                // отнимаем один доступный бой у игрока
                decBattleCount(profile);

                endPveBattleGrantAward(profile, msg, award);

                if (msg.result == WINNER) {
                    collectedReagents = msg.battleAward.collectedReagents;

                    if (msg.missionIds.length == 2)
                        heroicMissionService.onWinBattle(msg.missionIds, msg.mapId, profile.getId());
                } else if (msg.missionIds.length == 2) {
                    heroicMissionService.onDefeatBattle(msg.missionIds, msg.mapId, profile.getId());
                }

                profileEventsService.fireProfileEventAsync(END_PVE_BATTLE, profile, Param.endPvpBattleMessage, msg, Param.battleTime, battleTime);
            } else if (msg.wager == BattleWager.GLADIATOR_DUEL) {
                // корректность реагентов была проверена pvp сервером
                collectedReagents = msg.collectedReagents;
                coliseumService.consumeBattleResult(profile, msg.result, collectedReagents, battleTime);
            } else if (msg.wager.questId > 0) {
                // корректность реагентов была проверена pvp сервером
                collectedReagents = msg.collectedReagents;
                questService.consumeBattleResult(profile, msg.questId, msg.result, collectedReagents, msg.battleType, battleTime);
            } else if (msg.wager == BattleWager.MERCENARIES_DUEL) {
                // корректность реагентов была проверена pvp сервером
                collectedReagents = msg.collectedReagents;
                mercenariesService.consumeBattleResult(profile, msg.result, collectedReagents, battleTime);
            }

            // удаляем предметы из рюкзака
            removeOrUpdateWeapon(profile, msg.items);

            // выдаем реагенты
            craftService.incrementReagents(profile, collectedReagents);

            // если игрок покинул игру сохроняем профайл в БД
            if (!profile.isOnline()) {
                profileService.updateSync(profile);
            }
        } else {
            // дружеский PVP бой
            profileEventsService.fireProfileEventAsync(END_FRIEND_BATTLE, profile, Param.battleTime, battleTime);
        }

        if (cheater) {
            BanType banType = BanType.valueOf(msg.banType);
            if (banType != null) {
                cheatersCheckerService.punish(Connections.get(), profile, banType, banType.toString() + ": " + msg.banNote);
            } else {
                log.error("wrong banType in command {}", msg);
                Connections.get().close();
            }
        }

        // фиксируем факт обработки команды
        profile.setLastProcessedPvpBattleId(msg.battleId);
    }

    public void decBattleCount(UserProfile profile, int value) {
        // только так и не иначе!!! Нам важно коррекное выставление поля lastBattleTime
        for (int i = 0; i < value; i++) {
            decBattleCount(profile);
        }
    }

    public void decBattleCount(UserProfile profile) {
        if (profile.getBattlesCount() == BattleService.MAX_BATTLE_COUNT) {
            //сохроняем время начала боя
            profile.setLastBattleTime(System.currentTimeMillis());
        }
        // отнимаем один доступный бой у игрока
        profile.setBattlesCount(profile.getBattlesCount() - 1);

        if (profile.getBattlesCount() < MAX_BATTLE_COUNT && profile.getLevel() > 1) {
            int delay = (int) getDelay(profile.getLevel()) * (MAX_BATTLE_COUNT - profile.getBattlesCount());
            NotifyEvent missionRestoredNotifyEvent = notifyService.send(profile.getId(), profile.getLocale(), delay, 0, NotifyEventType.MISSIONS_RESTORED);
            if (missionRestoredNotifyEvent != null) {
                cancelNotify(profile.missionRestoredNotifyEvent);
                if (log.isDebugEnabled()) log.debug("{}.setMissionRestoredNotifyEvent({})", profile.toStringAsObject(), missionRestoredNotifyEvent);
                profile.missionRestoredNotifyEvent = missionRestoredNotifyEvent;
            }

            NotifyEvent missionRestoredNotifyEventFirst = null;
            if (profile.getBattlesCount() <= 0) {
                missionRestoredNotifyEventFirst = notifyService.send(profile.getId(), profile.getLocale(), (int) getDelay(profile.getLevel()), 0, NotifyEventType.MISSION_RESTORED);
            }
            if (missionRestoredNotifyEventFirst != null) {
                cancelNotify(profile.missionRestoredNotifyEventFirst);
                if (log.isDebugEnabled()) log.debug("{}.setMissionRestoredNotifyEventFirst({})", profile.toStringAsObject(), missionRestoredNotifyEventFirst);
                profile.missionRestoredNotifyEventFirst = missionRestoredNotifyEventFirst;
            }
        }
    }

    private void cancelNotify(NotifyEvent notifyEvent) {
        if (notifyEvent != null) {
            notifyEvent.needSend = false;
            if (log.isDebugEnabled()) log.debug("{}.needSend -> false", notifyEvent);
        }
    }

    private void endPveBattleGrantAward(UserProfile profile, EndPvpBattleRequest msg, List<GenericAwardStructure> award) {
        if (msg.result != WINNER) {
            if (BattleService.isSingleBossBattle(msg.missionIds)) {
                BossBattleResultType bossBattleResultType;
                if (msg.getMissionId() > profile.getCurrentNewMission()) {
                    bossBattleResultType = BossBattleResultType.FIRST_DEFEAT;
                } else {
                    int bossWinAwardTokenValue = dailyRegistry.getBossWinAwardToken(profile.getId());
                    bossBattleResultType = bossWinAwardTokenValue > 0 ? BossBattleResultType.NEXT_DEFEAT_WITH_TOKEN : BossBattleResultType.NEXT_DEFEAT_WITHOUT_TOKEN;
                }
                msg.battleAward.bossBattleResultType = bossBattleResultType;
            }
            return;
        }

        Map<Short, SimpleBattleSettings> awardSettingsMap = battleAwardSettings.getAwardSettingsMap();
        int money = 0;
        int realMoney = 0;
        int exp = 0;
        int bossWinAwardToken = 0;
        BossBattleWinAward bossBattleWinAward;
        BossBattleWinAward nextWinBattleAward;
        BossBattleResultType bossBattleResultType = null;

        int bossWinAwardTokenValue = dailyRegistry.getBossWinAwardToken(profile.getId());

        if (BattleService.isSingleBossBattle(msg.missionIds)) {
            // одиночный босс
            SimpleBattleSettings battleSettings = awardSettingsMap.get(msg.getMissionId());
            if (battleSettings == null || !battleSettings.isNewBossBattle()) {
                log.error("AwardSettings not found or wrong for PVE battle {} {}", msg, battleSettings);
                return;
            }

            BossBattleSettings bossBattleSettings = (BossBattleSettings) battleSettings;
            // первое прохождение босса
            if (msg.getMissionId() > profile.getCurrentNewMission()) {
                // увеличиваем прогресс по совместным миссиям
                profile.setCurrentNewMission(msg.getMissionId());

                bossBattleWinAward = bossBattleSettings.getFirstWinBattleAward();
                bossBattleResultType = BossBattleResultType.FIRST_WIN;
            } else {
                bossBattleWinAward = bossBattleSettings.getNextWinBattleAward();
                bossBattleResultType = bossWinAwardTokenValue > 0 ? BossBattleResultType.NEXT_WIN_WITH_TOKEN : BossBattleResultType.NEXT_WIN_WITHOUT_TOKEN;
            }
            nextWinBattleAward = bossBattleSettings.getNextWinBattleAward();

            bossBattleExtraRewardService.onEndBattleGrandReward(profile, msg.getMissionId(), bossBattleSettings, award);
        } else if (BattleService.isSuperBossBattle(msg.missionIds)) {
            // героик боссы
            int level = heroicMissionService.getHeroicMissionLevel(msg.missionIds);
            bossBattleWinAward = battleAwardSettings.getAward(level);
            nextWinBattleAward = bossBattleWinAward;
        } else {
            log.error("wrong missionIds field in cmd: " + msg);
            return;
        }

        int heroicMissionLevel = heroicMissionService.getHeroicMissionLevel(msg.missionIds);
        VarObject<byte[]> reagentsForBossBattle = new VarObject<>();
        reagentsForBossBattle.value = craftService.getReagentsForBossBattle(msg.missionIds, heroicMissionLevel);

        if (BattleService.isSingleBossBattle(msg.missionIds)) {
            reagentsForBossBattle.value = trimReagentsForBossBattle(bossWinAwardTokenValue, reagentsForBossBattle.value);
        }

        if (bossWinAwardTokenValue > 0 || BattleService.isSuperBossBattle(msg.missionIds)) {
            // выдаем награду за прохождение мисии с боссами
            exp = bossBattleWinAward.getExperience();
            money = bossBattleWinAward.getMoney();
            realMoney = bossBattleWinAward.getRealMoney();

            Tuple2<Short, Map<Byte, Integer>> rareItem_medals = profileBonusService.grantBossBattleItemsAward(profile, bossBattleWinAward, award);
            Short rareItemId = rareItem_medals._1;

            // фиксируем в БД рубиновую награду за босса
            if (bossBattleWinAward.getRealMoney() > 0 || rareItemId > 0) {
                statisticService.awardStatistic(profile.getId(), money, realMoney, rareItemId, AwardTypeEnum.BATTLE.getType(), Arrays.toString(msg.missionIds));
            }
            msg.battleAward.rareItem = rareItemId;
            rareItem_medals._2().forEach((reagentId, count) -> IntStream.range(0, count).forEach(i -> reagentsForBossBattle.value = ArrayUtils.add(reagentsForBossBattle.value, reagentId)));
            bossWinAwardToken = BattleService.isSingleBossBattle(msg.missionIds) ? -1 : 0;

            dailyRegistry.addBossWinAwardToken(profile.getId(), bossWinAwardToken);
            if (disableValidateMission) {
                dailyRegistry.addBossWinAwardToken(profile.getId(), 1);
            }
        } else {
            // выдаем награду за прохождение босса без наградного билета. Пройти можно только босса пройденного ранее
            exp = nextWinBattleAward.getExperience();
            money = nextWinBattleAward.getMoney() / 2;
        }
        int boostFactor = profileService.getBoostFactor(profile);
        money *= boostFactor;
        exp *= boostFactor;

        profile.setMoney(profile.getMoney() + money);
        profile.setRealMoney(profile.getRealMoney() + realMoney);
        profileExperienceService.addExperience(profile, exp);

        msg.battleAward.money = money;
        msg.battleAward.realMoney = realMoney;
        msg.battleAward.experience = exp;
        msg.battleAward.boostFactor = boostFactor;
        msg.battleAward.bossWinAwardToken = bossWinAwardToken;
        msg.battleAward.bossBattleResultType = bossBattleResultType;
        msg.battleAward.collectedReagents = reagentsForBossBattle.value;

        profileBonusService.fillBattleAward(msg.battleAward.money, msg.battleAward.realMoney, 0, msg.battleAward.experience, award);
        profileBonusService.fillBattleAward(msg.battleAward.collectedReagents, award);

        if (BattleService.isSingleBossBattle(msg.missionIds)) {
            // регистрируем прохождение мисии с боссами в "суточном реестре"
            dailyRegistry.setSuccessedMission(profile.getId());
        } else {
            // регистрируем прохождение мисии с супер боссами в "суточном реестре"
            dailyRegistry.setSuccessedSuperBosMission(profile.getId());
        }
    }

    private void removeOrUpdateWeapon(UserProfile profile, BackpackItemStructure[] items) {
        for (BackpackItemStructure itemStructure : items) {
            weaponService.removeOrUpdateWeapon(null, profile, itemStructure.weaponId, itemStructure.count, true);
        }
    }

    private void endPvpBattleGrantAward(UserProfile profile, EndPvpBattleRequest msg, List<GenericAwardStructure> award) {
        WagerDef wagerDef = getBattleWagerDef(msg.wager, msg.teamSize);
        if (msg.result == WINNER) {
            int winMoney = wagerDef.award;
            if (msg.wager == BattleWager.WAGER_20_DUEL) {
                /*
                При победе получаешь фузы в зависимости от твоего здоровья на момент окончания боя: чем меньше здоровья, тем больше награда.
                Больше 50% хп = 15 фузов
                Больше 15% хп = 30 фузов
                Меньше 15% хп = 60 фузов
                */
                if (msg.healthInPercent > 50) {
                    winMoney = 15;
                } else if (msg.healthInPercent > 15) {
                    winMoney = 30;
                } else {
                    winMoney = 60;
                }
            }
            int extraMoney = 0;
            int extraExp = 0;
            int wagerWinAwardToken = 0;
            if (dailyRegistry.getWagerWinAwardToken(profile.getId()) > 0) {
                extraExp = firstWagerWinAwardExp;
                extraMoney = firstWagerWinAwardMoney;
                wagerWinAwardToken = -1;
                winMoney += extraMoney;
                dailyRegistry.addWagerWinAwardToken(profile.getId(), wagerWinAwardToken);
            }
            profile.setMoney(profile.getMoney() + winMoney);
            msg.battleAward.money = winMoney;
            msg.battleAward.extraMoney = extraMoney;
            msg.battleAward.wagerWinAwardToken = wagerWinAwardToken;
            msg.battleAward.healthInPercent = msg.healthInPercent;

            // начисляем опыт (ускорители не учитываем)
            Integer exp = pvpExperience.get(wagerDef.wager);
            if (exp != null) {
                exp += extraExp;
                profileExperienceService.addExperience(profile, exp);
                msg.battleAward.experience = exp;
            } else {
                log.error("не указан опыт за победу на ставке {}", wagerDef.wager);
            }
        } else if (msg.result == PvpBattleResult.NOT_WINNER) {
            int looseMoney = wagerDef.value;
            int winMoney = getAwardForDroppedUnit(profile, msg);
            int awardMoney = winMoney - looseMoney;
            profile.setMoney(profile.getMoney() + awardMoney);
            msg.battleAward.money = awardMoney;
        }
        profileBonusService.fillBattleAward(msg.battleAward.money - msg.battleAward.extraMoney, 0, 0, msg.battleAward.experience, award);
        if (msg.battleAward.extraMoney > 0) {
            award.add(new GenericAwardStructure(AwardKindEnum.EXTRA_MONEY, msg.battleAward.extraMoney));
        }
        // начисляем очки рейтинга
        ratingService.onEndPvpBattle(profile, msg);

        // обновляем TrueSkill рейтинг если он был изменен PVP сервером
        if (msg.newTrueSkillMean > 0 && msg.newTrueSkillStandardDeviation > 0) {
            TrueSkillEntity trueSkillEntity = trueSkillService.getTrueSkillFor(profile);
            trueSkillEntity.setMean(msg.newTrueSkillMean);
            trueSkillEntity.setStandardDeviation(msg.newTrueSkillStandardDeviation);
            trueSkillEntity.incBattles();
        }
    }

    private int getAwardForDroppedUnit(UserProfile profile, EndPvpBattleRequest msg) {
        if (msg.wager == BattleWager.WAGER_15_DUEL) {
            if (msg.droppedUnits < 0 || msg.droppedUnits >= awardForDroppedUnits.size()) {
                log.error("[{}] некорректное зеначение поля droppedUnits в команде {}", profile, msg);
                return 0;
            } else {
                return awardForDroppedUnits.get(msg.droppedUnits);
            }
        } else {
            return 0;
        }
    }

    /**
     * отсчитает еще один бой
     *
     * @return id нового боя
     */
    public long incrementAndGetBattleCount() {
        return battleCount.incrementAndGet();
    }

    /**
     * вернет доступное количество боёв для игрока и
     * начислит нужное количество боёв если время пришло
     *
     * @param profile профайл игрока
     * @return доступное количество боёв для игрока
     */
    public int checkBattleCount(UserProfile profile) {
        if (profile.getBattlesCount() < MAX_BATTLE_COUNT) {
            synchronized (profile) {
                if (profile.getBattlesCount() < MAX_BATTLE_COUNT) {
                    // начисляем нужное количество битв если пришло время
                    long delay = getDelay(profile.getLevel());
                    long curTime = System.currentTimeMillis();
                    long lastTime = profile.getLastBattleTime();
                    long deltaT = curTime - lastTime;
                    long canIncrease = (long) Math.floor(deltaT / delay);
                    int battleCount = (int) Math.min(profile.getBattlesCount() + canIncrease, MAX_BATTLE_COUNT);
                    if (battleCount != MAX_BATTLE_COUNT) {
                        //если начислили не все бои, то нужно сдвинуть время боя
                        profile.setLastBattleTime(lastTime + delay * canIncrease);
                    }
                    if (battleCount > profile.getBattlesCount()) {
                        profileEventsService.fireProfileEventAsync(RESTORE_BATTLES, profile,
                                Param.battles, battleCount - profile.getBattlesCount()
                        );
                    }
                    profile.setBattlesCount(battleCount);
                }
            }
            return profile.getBattlesCount();
        } else {
            return profile.getBattlesCount();
        }
    }

    /**
     * Распределение времени восстановления по уровням
     * Уровень Время (m)
     *
     * @param level
     * @return вернет нужное количество в милисекундах ожидания боя
     */
    public long getDelay(int level) {
        int delay = levelCreator.getLevel(level).getDelay();
        return delay == 0
                ? 1
                : TimeUnit.MINUTES.toMillis(delay);
    }

    /**
     * Доступна ли игроку миссия с боссами
     *
     * @param profile профиль игрока
     * @return проходил ли игрок сегодня успешно миссию с Боссами
     */
    public boolean isBossTodayAvaliableFor(UserProfile profile) {
        return !dailyRegistry.isSuccessedMission(profile.getId());
    }

    /**
     * Доступна ли игроку миссия с супер боссами
     *
     * @param profile профиль игрока
     * @return проходил ли игрок сегодня успешно миссию с Боссами
     */
    public boolean isSuperBossTodayAvaliableFor(UserProfile profile) {
        return !dailyRegistry.isSuccessedSuperBossMission(profile.getId());
    }

    public boolean validateMission(UserProfile profile, short missionId, SimpleBattleSettings battleSettings) {
        return validateMission(profile, missionId, battleSettings, isBossTodayAvaliableFor(profile));
    }

    public boolean validateMission(UserProfile profile, short missionId, SimpleBattleSettings battleSettings, boolean isTodayAvaliable) {
        if (disableValidateMission) {
            return true;
        }
        if (profile.getLevel() < battleSettings.getMinLevel() || profile.getLevel() > battleSettings.getMaxLevel()) {
            return false;
        }
        // не отслеживаем прохождение мисии - бой с ботами
        if (!battleSettings.isTrackedBattle()) {
            return true;
        } else if (battleSettings.isLearningBattle() && missionId < profile.getCurrentMission() && profile.getCurrentMission() <= 0) {
            // обучающая миссия - запрещаем проходить миссии повторно
            // id обучающих миссий  отрицательные и уменьшаются
            // запрещаем проходить обучающую миссию после прохождения любой из миссий с босами
            return true;
        } else if (battleSettings.isBossBattle() && (missionId <= profile.getCurrentMission() + 1 || profile.getCurrentMission() < 0 && missionId == 1) && isTodayAvaliable) {
            // миссия с боссом - количество прохождений не ограничено
            // миссия или пройденная или последующая
            // id этих миссий положительные и увеличиваются
            if (missionId == profile.getCurrentMission() + 1) {
                // нового босса можно пройти только при наличии наградного билета
                return dailyRegistry.getBossWinAwardToken(profile.getId()) > 0;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean validateBattlesCount(UserProfile profile) {
        return profile.getBattlesCount() > 0 || disableValidateMission;
    }

    public boolean validateSuperBossMission(UserProfile profile, short missionId, SimpleBattleSettings battleSettings) {
        return battleSettings.isNewBossBattle() ?
                validateNewMission(profile, missionId, battleSettings, isSuperBossTodayAvaliableFor(profile)) :
                validateMission(profile, missionId, battleSettings, isSuperBossTodayAvaliableFor(profile));
    }

    public boolean validateNewMission(UserProfile profile, short missionId, SimpleBattleSettings battleSettings) {
        return validateNewMission(profile, missionId, battleSettings, isBossTodayAvaliableFor(profile));
    }

    public boolean validateNewMission(UserProfile profile, short missionId, SimpleBattleSettings battleSettings, boolean isTodayAvaliable) {
        if (disableValidateMission) {
            return true;
        }
        if (profile.getLevel() < battleSettings.getMinLevel() || profile.getLevel() > battleSettings.getMaxLevel()) {
            return false;
        }
        if (battleSettings.isNewBossBattle() && missionId <= (profile.getCurrentNewMission() == 0 ? 100 : profile.getCurrentNewMission()) + 1 && isTodayAvaliable) {
            // миссия на прохождение - в день можно проходить только одну мисию, или уже пройденную или последующую
            // id этих миссий начинаются со 101 и увеличивается
            if (missionId == (profile.getCurrentNewMission() == 0 ? 100 : profile.getCurrentNewMission()) + 1) {
                // нового босса можно пройти только при наличии наградного билета
                return dailyRegistry.getBossWinAwardToken(profile.getId()) > 0;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Подготавливает профайл к началу боя
     *
     * @param profile       профайл игрока который начинает бой
     * @param awardSettings настройки наград и опыта за бой
     * @param missionId     номер начинаемой миссии
     * @param award
     * @return {@code battleId} для нового начатого боя
     */
    public long startSimpleBattle(UserProfile profile, SimpleBattleSettings awardSettings, short missionId, List<GenericAwardStructure> award) {
        // говорим, что бой начался и ждем его конца с результатами
        profile.setBattleState(BattleState.SIMPLE);

        //отсчитываем id боя
        long battleId = incrementAndGetBattleCount();

        profile.setBattleId(battleId);
        profile.setMissionId(missionId);

        int boostFactor = profileService.getBoostFactor(profile);

        // начисляем игроку опыт за проигрышь на случай если соеденение с сервером оборвется
        int exp = awardSettings.getNotWinnerExp() * boostFactor;
        profileExperienceService.addExperience(profile, exp);

        // начисляем игроку денег за проигрышь
        int awardMoney = awardSettings.getNotWinnerMoney() * boostFactor;
        profile.setMoney(profile.getMoney() + awardMoney);

        //сохраняем время начала боя
        profile.setStartBattleTime(System.currentTimeMillis());
        if (awardSettings.isBossBattle()) {
            // начинаем запись журнала боя
            cheatersCheckerService.logMissionStart(profile);
        }

        int battles = 0;
        if (awardSettings.isDecrementBattleCount()) {
            decBattleCount(profile);
            battles = -1;
        }
        profileEventsService.fireProfileEventAsync(START_SIMPLE_BATTLE, profile,
                Param.battles, battles,
                Param.money, awardMoney,
                Param.experience, exp,
                Param.battleId, battleId,
                Param.missionId, missionId,
                Param.version, versionToString(profile.version)
        );

        int extraReagentCount = profileService.getExtraReagentCount(profile);
        byte[] reagentsForBattle = ArrayUtils.EMPTY_BYTE_ARRAY;
        if (!awardSettings.isBossBattle()) {
            reagentsForBattle = craftService.getReagentsForSimpleBattle(profile.getLevel(), extraReagentCount);
        }
        profile.setReagentsForBattle(reagentsForBattle);

        profileBonusService.fillBattleAward(awardMoney, 0, 0, exp, award);
        profile.defeatBattleAward = award;

        return battleId;
    }

    public CheatersCheckerService.ValidationResult onSimpleBattleTurn(UserProfile profile, EndTurn msg) {
        CheatersCheckerService.ValidationResult validationResult = cheatersCheckerService.validateEndTurnMsg(profile, msg);
        switch (validationResult) {
            case OK:
                msg.turn.endTime = currentTimeSeconds();
                cheatersCheckerService.logMissionTurn(profile, msg.turn);
                break;
            case BANNED:
                // клиент сообщил, что это читер. Он уже забанен.
                onBattleInterrupted(profile, BattleResultEnum.NOT_WINNER_CHEAT, AppUtils.currentTimeSeconds());
                break;
        }
        return validationResult;
    }

    public static short lastTurnNum(MissionLogStructure missionLog) {
        if (missionLog != null && missionLog.turns.size() > 0) {
            return missionLog.turns.get(missionLog.turns.size() - 1).turnNum;
        } else {
            return -1;
        }
    }

    /**
     * Помечает уже прерванный бой как прерванный (в с случае дисконнекта или бана)
     *
     * @param profile профиль игрока
     * @param result  результат завершения, который отобразится в статистике: NOT_WINNER_DISCONNECT или NOT_WINNER_CHEAT
     */
    public void onBattleInterrupted(UserProfile profile, BattleResultEnum result, int finishBattleTime) {
        // отражаем окончание боя c боссом в статистике боёв
        if (profile.inBattleState(BattleState.SIMPLE) && profile.getBattleId() > 0) {
            // списываем не бесконечное оружие, накопленное в логе
            MissionLogStructure mLog = profile.getMissionLog();
            if (mLog != null) {
                for (TurnStructure turn : mLog.turns) {
                    if (turn.isPlayerTurn) {
                        for (BackpackItemStructure item : turn.items) {
                            weaponService.removeOrUpdateWeaponSilent(profile, item.weaponId, item.count);
                        }
                    }
                }
            }
            EndBattle msg = new EndBattle(MY_LEVEL, result, (int) profile.getBattleId(), profile.getMissionId());
            SimpleBattleSettings battleSettings = awardSettingsMap.get(msg.missionId);
            if (battleSettings.isBossBattle()) {
                if (msg.missionId > profile.getCurrentMission()) {
                    msg.battleAward.bossBattleResultType = BossBattleResultType.FIRST_DEFEAT;
                } else {
                    int bossWinAwardTokenValue = dailyRegistry.getBossWinAwardToken(profile.getId());
                    msg.battleAward.bossBattleResultType = bossWinAwardTokenValue > 0 ? BossBattleResultType.NEXT_DEFEAT_WITH_TOKEN : BossBattleResultType.NEXT_DEFEAT_WITHOUT_TOKEN;
                }
            }
            onEndSimpleBattle(profile, msg, finishBattleTime);
        }
        profile.wipeSimpleBattleState();
        profile.setReagentsForBattle(null);
    }

    public EndBattleResult.EndBattleValidateResult endBattle(UserProfile profile, EndBattle msg, List<GenericAwardStructure> award) {
        EndBattleResult.EndBattleValidateResult endBattleValidateResult = EndBattleResult.EndBattleValidateResult.INVALID;
        // если был бой за который можно получить награду
        if (profile.getBattleState() == BattleState.SIMPLE) {
            SimpleBattleSettings battleSettings = awardSettingsMap.get(msg.missionId);
            if (battleSettings.isBossBattle()) {
                cheatersCheckerService.logMissionEnd(profile, msg);
            }

            boolean grantAwardAndReagents = true;
            if (!cheatersCheckerService.validateEndBattleMsg(profile, msg) ||
                    !cheatersCheckerService.checkInstantWin(profile, System.currentTimeMillis() - profile.getStartBattleTime(), msg.result)) {
                // если бой длился меньше чем 10 секунд и при этом
                // он еще и победил, то не засчитываем такой бой
                msg.result = BattleResultEnum.NOT_WINNER_CHEAT;
                endBattleValidateResult = EndBattleResult.EndBattleValidateResult.CHEAT;
                grantAwardAndReagents = false;
            } else {
                // иначе ещё проверяем, что лог миссии сходится:
                CheatTypeEnum cheatType = cheatersCheckerService.validateMissionLogTotals(profile, msg.result);
                if (cheatType.mustBeDiscarded()) {
                    // а) если бой должен быть не засчитан - отменяем выдачу награды и подправляем msg
                    log.error("Battle [{}]:{} DISCARDED: cheat type {}", msg.battleId, msg.result, cheatType);
                    msg.result = BattleResultEnum.NOT_WINNER_CHEAT;
                    endBattleValidateResult = EndBattleResult.EndBattleValidateResult.CHEAT;
                    grantAwardAndReagents = false;
                }
                if (cheatType.mustBeBanned()) {
                    // б) если же нужно ещё и забанить - баним
                    BanType banType = BanType.BAN_FOR_BOSS_HACK;
                    grantAwardAndReagents = false;
                    log.error("User [{}] BANNED for cheat type {} with ban type {} for {} days", profile, cheatType, banType, banType.durationInDays);
                    cheatersCheckerService.punish(Connections.get(), profile, banType, banType.toString() + ": " + cheatType.name());
                }
            }
            if (grantAwardAndReagents) {
                //выдаем награду за бой (деньги/опыт)
                onEndBattleGrantAward(msg, profile, award);
                endBattleValidateResult = EndBattleResult.EndBattleValidateResult.OK;
            } else if (battleSettings.isBossBattle()) {
                if (msg.missionId > profile.getCurrentMission()) {
                    msg.battleAward.bossBattleResultType = BossBattleResultType.FIRST_DEFEAT;
                } else {
                    int bossWinAwardTokenValue = dailyRegistry.getBossWinAwardToken(profile.getId());
                    msg.battleAward.bossBattleResultType = bossWinAwardTokenValue > 0 ? BossBattleResultType.NEXT_DEFEAT_WITH_TOKEN : BossBattleResultType.NEXT_DEFEAT_WITHOUT_TOKEN;
                }

            }
            //отражаем результат боя в статистике боев
            onEndSimpleBattle(profile, msg, AppUtils.currentTimeSeconds());
        }

        profile.wipeSimpleBattleState();

        //удаляем предметы с рюкзака
        for (BackpackItemStructure itemStructure : msg.items) {
            weaponService.removeOrUpdateWeapon(msg.missionId, profile, itemStructure.weaponId, itemStructure.count, true);
        }
        profile.setReagentsForBattle(null);
        profile.setLastProcessedBattleId(msg.battleId);

        return endBattleValidateResult;
    }

    //отражаем результат боя в статистике боев
    public void onEndSimpleBattle(UserProfile profile, EndBattle msg, int finishBattleTime) {
        String battleTime = "";
        int battleTurns = 0;
        try {
            battleTime = PvpService.formatTimeInSeconds((int) (finishBattleTime - profile.getStartBattleTime() / 1000L));
            if (msg.missionId > 0) {
                String clientVersion = versionToString(profile.version);
                String serverVersion = appParams.getVersionAsString();
                String resultStr = msg.result.name();
                long assignedBattleId = profile.getBattleId();
                long assignedMissionId = profile.getMissionId();
                String banNote = msg.banNote.replaceAll(" ", "_");
                banNote = banNote.isEmpty() ? "\'\'" : banNote;
                String bossBattleResultType = msg.battleAward.bossBattleResultTypeName();
                String currMissionId = "" + profile.getCurrentMission() + "/" + dailyRegistry.getBossWinAwardToken(profile.getId());
                String missionLog = profile.getMissionLog() != null ? gson.toJson(profile.getMissionLog()) : "";
                String record = String.format("EndBattle\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                        profile.getProfileId(), battleTime, resultStr, msg.missionId, assignedMissionId, msg.battleId, assignedBattleId, clientVersion, serverVersion, msg.banType, banNote, missionLog, currMissionId, bossBattleResultType);
                battlesCdrlogger.info(record);
                battleTurns = Optional.ofNullable(profile.getMissionLog()).map(mLog -> mLog.turns.size()).orElse(0);
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        profileEventsService.fireProfileEventAsync(END_SIMPLE_BATTLE, profile,
                Param.endSimpleBattleMessage, msg,
                Param.battleTime, battleTime,
                Param.battleTurns, battleTurns
        );
    }

    public static boolean isSingleBossBattle(short[] missionIds) {
        return missionIds != null && missionIds.length == 1;
    }

    public static boolean isSuperBossBattle(short[] missionIds) {
        return missionIds != null && missionIds.length > 1;
    }

    public void setOffBattleReagents(long battleId, byte[] collectedReagents, UserProfile profile, EndBattle.BattleAward battleAward) {
        byte[] reagentsForBattle = profile.getReagentsForBattle();
        if (reagentsForBattle == null) {
            log.error("для боя battleId={} отсутствуют заготовленные реагенты", battleId);
            return;
        }
        for (byte reagentId : collectedReagents) {
            if (reagentId >= 0) {
                // проверям реагенты посланные и возвращенные
                for (int i = 0; i < reagentsForBattle.length; i++) {
                    byte sentReagentId = reagentsForBattle[i];
                    if (reagentId == sentReagentId) {
                        craftService.incrementReagent(reagentId, profile.getId());
                        battleAward.collectedReagents.add(reagentId);
                        reagentsForBattle[i] = -1;
                        break;
                    }
                }
            }
        }
    }

    public void refundOfflineBattles(UserProfile profile, EndBattleStructure[] offlineBattles) {
        if (simpleBattleReconnectEnabled) {
            return;
        }
        int logoutBattlesCount = profile.getBattlesCount();

        //30 - таймаут http сессии
        long finishLastBattle = (profile.getLogoutTime() - 30) * 1000;

        for (int i = 0; i < offlineBattles.length; i++) {
            EndBattleStructure endBattleResult = offlineBattles[i];
            if (i >= logoutBattlesCount) {
                log.error("[{}] extra battle! {}", profile, endBattleResult);
                continue;
            }

            SimpleBattleSettings battleSettings = awardSettingsMap.get(endBattleResult.missionId);

            if (battleSettings == null) {
                log.error("[{}] offline battle AwardSettings not found for missionId {}", profile, endBattleResult.missionId);
                return;
            } else if (battleSettings.isBossBattle() || !validateMission(profile, endBattleResult.missionId, battleSettings)) {
                log.error("[{}] offline battle validate mission failed! EndBattleStructure{}", profile, endBattleResult);
                return;
            }

            if (cheatersCheakerService.validateOfflineBattle(profile, finishLastBattle, endBattleResult)) {
                //выдаем награду за бой (деньги/опыт)
                onEndOfflineBattleGrantAward(endBattleResult, profile);

                //отражаем результат боя в статистике боев
                onEndSimpleBattle(profile, newEndBattle(endBattleResult), endBattleResult.finishBattleTime);
            } else {
                log.error("[{}] offline battle result invalid! EndBattleStructure{}", profile, endBattleResult);

                // в статистику боев заносим поражение
                endBattleResult.result = BattleResultEnum.NOT_WINNER_CHEAT;
                onEndSimpleBattle(profile, newEndBattle(endBattleResult), endBattleResult.finishBattleTime);

                return;
            }
            // если бой длился меньше чем 10 секунд и при этом
            // он еще и победил, то не засчитываем такой бой
            boolean checkInstantWin = cheatersCheakerService.checkInstantWin(profile, (endBattleResult.finishBattleTime - endBattleResult.startBattleTime) * 1000L, endBattleResult.result);
            if (!checkInstantWin) {
                log.error("[{}] offline battle instant win! EndBattleStructure{}", profile, endBattleResult);
                return;
            }

            //удаляем предметы с рюкзака
            for (BackpackItemStructure itemStructure : endBattleResult.items) {
                weaponService.removeOrUpdateWeapon(endBattleResult.missionId, profile, itemStructure.weaponId, itemStructure.count, true);
            }
            finishLastBattle = endBattleResult.finishBattleTime * 1000L;
        }
    }

    private EndBattle newEndBattle(EndBattleStructure endBattleStruct) {
        EndBattle endBattle = new EndBattle();
        endBattle.battleId = 0;
        endBattle.result = endBattleStruct.result;
        endBattle.type = endBattleStruct.type;
        endBattle.expBonus = endBattleStruct.expBonus;
        endBattle.missionId = endBattleStruct.missionId;
        endBattle.items = endBattleStruct.items;
        endBattle.banType = endBattleStruct.banType;
        endBattle.banNote = endBattleStruct.banNote;
        return endBattle;
    }

    private void onEndOfflineBattleGrantAward(EndBattleStructure msg, UserProfile profile) {
        //todo бустеры и логгирование в events логе
        SimpleBattleSettings battleSettings = awardSettingsMap.get(msg.missionId);

        if (battleSettings == null) {
            log.error("AwardSettings not found for missionId={} in SIMPLE battle", msg.missionId);
            return;
        }

        //сначало начисляем бонусный опыт, если накрутили опыт, то не засчитываем
        int exp = Math.min(msg.expBonus, battleSettings.getMaxExpBonus());
        int money = 0;

        if (msg.result == BattleResultEnum.NOT_WINNER) {
            // начисляем игроку опыт за проигрышь
            exp += battleSettings.getNotWinnerExp();

            // начисляем игроку денег за проигрышь
            money = battleSettings.getNotWinnerMoney();
        } else {
            BattleAward battleAward = getMissionBattleAward(msg.type, battleSettings);

            if (battleAward == null) {
                log.error("BattleAward not found for missionId={} and type={} in SIMPLE battle", msg.missionId, msg.type);
                return;
            }

            if (msg.result == BattleResultEnum.WINNER) {
                // выдаем награду и регистрируем прохождение обучающей мисии
                if (battleSettings.isLearningBattle()) {
                    exp += battleAward.getWinnerExp();
                    money = battleAward.getWinnerMoney();

                    profile.setCurrentMission(msg.missionId);
                } else if (!battleSettings.isBossBattle()) {
                    // миссия с ботами
                    exp += battleAward.getWinnerExp();
                    money = battleAward.getWinnerMoney();
                }
            } else {
                exp += battleAward.getDrawGameExp();
                money = battleAward.getDrawGameMoney();
            }

        }
        if (log.isDebugEnabled()) {
            log.debug("[{}] refund offline battle: money={} exp={} {}", profile, money, exp, msg);
        }
        profile.setMoney(profile.getMoney() + money);
        profileExperienceService.addExperience(profile, exp, false);

        profile.setLastBattleTime(msg.startBattleTime);
    }

    public void onEndBattleGrantReagents(SimpleBattleSettings battleSettings, EndBattle msg, UserProfile profile, int bossWinAwardToken) {
        //учитываем собранные реагенты в бою
        if (battleSettings.isBossBattle()) {
            if (msg.result == BattleResultEnum.WINNER) {
                // если бой с боссом, только в случае победы
                int extraReagentCount = profileService.getExtraReagentCount(profile);
                byte[] reagentsForBossBattle = craftService.getReagentsForBossBattle(msg.missionId, extraReagentCount);
                byte[] reagentsForBattle = trimReagentsForBossBattle(bossWinAwardToken, reagentsForBossBattle);

                profile.setReagentsForBattle(reagentsForBattle);
                setOffBattleReagents(msg.battleId, reagentsForBattle, profile, msg.battleAward);
            }
        } else {
            // если бой с ботами, учитываем всегда
            setOffBattleReagents(msg.battleId, msg.collectedReagents, profile, msg.battleAward);
        }
    }

    public byte[] trimReagentsForBossBattle(int bossWinAwardToken, byte[] reagentsFofBattle) {
        if (reagentsFofBattle.length > 1 && bossWinAwardToken == 0) {
            reagentsFofBattle = new byte[]{reagentsFofBattle[ThreadLocalRandom.current().nextInt(0, reagentsFofBattle.length)]};
        }
        return reagentsFofBattle;
    }

    public void onEndBattleGrantAward(EndBattle msg, UserProfile profile, List<GenericAwardStructure> award) {
        SimpleBattleSettings battleSettings = battleAwardSettings.getAwardSettingsMap().get(msg.missionId);
        if (battleSettings == null) {
            log.error("AwardSettings not found for missionId={} in SIMPLE battle", msg.missionId);
            return;
        }
        //сначало начисляем бонусный опыт, если накрутили опыт, то не засчитываем
        int bonusExp = msg.expBonus <= battleSettings.getMaxExpBonus() ? msg.expBonus : 0;

        int bossWinAwardToken = 0;
        BossBattleResultType bossBattleResultType = null;

        int boostFactor = profileService.getBoostFactor(profile);
        if (msg.result != BattleResultEnum.WINNER) {
            if (battleSettings.isBossBattle()) {
                if (msg.missionId > profile.getCurrentMission()) {
                    bossBattleResultType = BossBattleResultType.FIRST_DEFEAT;
                } else {
                    int bossWinAwardTokenValue = dailyRegistry.getBossWinAwardToken(profile.getId());
                    bossBattleResultType = bossWinAwardTokenValue > 0 ? BossBattleResultType.NEXT_DEFEAT_WITH_TOKEN : BossBattleResultType.NEXT_DEFEAT_WITHOUT_TOKEN;
                }
            }
        }
        int bossWinAwardTokenValue = dailyRegistry.getBossWinAwardToken(profile.getId());

        // опыт за то, что игрок проиграл мы уже зачисляли
        if (msg.result == BattleResultEnum.WINNER || msg.result == BattleResultEnum.DRAW_GAME) {
            int exp = 0;

            BattleAward battleAward = getMissionBattleAward(msg.type, battleSettings);

            if (battleAward == null) {
                log.error("BattleAward not found for missionId={} and type={} in SIMPLE battle", msg.missionId, msg.type);
                return;
            }

            int money = 0;
            int realMoney = 0;

            if (msg.result == BattleResultEnum.WINNER) {
                // выдаем награду и регистрируем прохождение обучающей мисии
                if (battleSettings.isLearningBattle()) {
                    exp = battleAward.getWinnerExp() - battleSettings.getNotWinnerExp();
                    money = battleAward.getWinnerMoney() - battleSettings.getNotWinnerMoney();

                    profile.setCurrentMission(msg.missionId);
                } else if (battleSettings.isBossBattle()) {
                    // прохождение мисии с боссами
                    BossBattleSettings bossBattleSettings = (BossBattleSettings) battleSettings;
                    BossBattleWinAward bossBattleWinAward;
                    // первое прохождение босса
                    if (msg.missionId > profile.getCurrentMission()) {
                        // увеличиваем прогресс по миссиям
                        profile.setCurrentMission(msg.missionId);

                        bossBattleWinAward = bossBattleSettings.getFirstWinBattleAward();
                        bossBattleResultType = BossBattleResultType.FIRST_WIN;
                    } else {
                        bossBattleWinAward = bossBattleSettings.getNextWinBattleAward();
                        bossBattleResultType = bossWinAwardTokenValue > 0 ? BossBattleResultType.NEXT_WIN_WITH_TOKEN : BossBattleResultType.NEXT_WIN_WITHOUT_TOKEN;
                    }

                    if (bossWinAwardTokenValue > 0) {
                        // выдаем награду за прохождение мисии с боссами
                        exp = bossBattleWinAward.getExperience();
                        money = bossBattleWinAward.getMoney();
                        realMoney = bossBattleWinAward.getRealMoney();

                        Tuple2<Short, Map<Byte, Integer>> rareItem_medals = profileBonusService.grantBossBattleItemsAward(profile, bossBattleWinAward, award);
                        Short rareItemId = rareItem_medals._1;

                        // фиксируем в БД рубиновую награду за босса
                        if (bossBattleWinAward.getRealMoney() > 0 || rareItemId > 0) {
                            statisticService.awardStatistic(profile.getId(), money, realMoney, rareItemId, AwardTypeEnum.BATTLE.getType(), "[" + msg.missionId + "]");
                        }
                        msg.battleAward.rareItem = rareItemId;
                        rareItem_medals._2().forEach((reagentId, count) -> IntStream.range(0, count).forEach(i -> msg.battleAward.collectedReagents.add(reagentId)));

                        bossWinAwardToken = -1;
                        dailyRegistry.addBossWinAwardToken(profile.getId(), bossWinAwardToken);
                        if (disableValidateMission) {
                            dailyRegistry.addBossWinAwardToken(profile.getId(), 1);
                        }
                    } else {
                        // выдаем награду за прохождение босса без наградного билета. Пройти можно только босса пройденного ранее
                        BossBattleWinAward nextWinBattleAward = bossBattleSettings.getNextWinBattleAward();
                        exp = nextWinBattleAward.getExperience() / 2;
                        money = nextWinBattleAward.getMoney() / 2;
                    }

                    bossBattleExtraRewardService.onEndBattleGrandReward(profile, msg.missionId, bossBattleSettings, award);

                    // регистрируем прохождение мисии с боссами в "суточном реестре"
                    dailyRegistry.setSuccessedMission(profile.getId());
                } else {
                    // миссия с ботами
                    exp = battleAward.getWinnerExp() - battleSettings.getNotWinnerExp();
                    money = battleAward.getWinnerMoney() - battleSettings.getNotWinnerMoney();
                }
            } else {
                exp = battleAward.getDrawGameExp() - battleSettings.getNotWinnerExp();
                money = battleAward.getDrawGameMoney() - battleSettings.getNotWinnerMoney();
            }
            profile.setMoney(profile.getMoney() + money * boostFactor);
            msg.battleAward.money = money * boostFactor;
            // на рубины ускорители не распространяются
            profile.setRealMoney(profile.getRealMoney() + realMoney);
            msg.battleAward.realMoney = realMoney;

            profileExperienceService.addExperience(profile, bonusExp + exp * boostFactor);
            msg.battleAward.experience = bonusExp + exp * boostFactor;
        } else {
            profileExperienceService.addExperience(profile, bonusExp);
            msg.battleAward.experience = bonusExp;
        }
        // Если игрок подбирает в миссиях бонусные звезды, выдавать 4 фуза за каждую звезду
        // при этом ограничить макс кол-во звезд на миссию до 5
        int bonusMoney = Math.min(simpleBattleMaxAwardedStars, bonusExp) * simpleBattleMoneyPerStar;
        profile.setMoney(profile.getMoney() + bonusMoney);
        msg.battleAward.money += bonusMoney;

        msg.battleAward.boostFactor = boostFactor;
        msg.battleAward.bossWinAwardToken = bossWinAwardToken;
        msg.battleAward.bossBattleResultType = bossBattleResultType;

        //зачисляем собранные(заработанные) реагенты
        onEndBattleGrantReagents(battleSettings, msg, profile, bossWinAwardTokenValue);

        profileBonusService.fillBattleAward(msg.battleAward.money, msg.battleAward.realMoney, bonusExp, msg.battleAward.experience - bonusExp, award);
        profileBonusService.fillBattleAward(msg.battleAward.collectedReagents, award);
    }

    private BattleAward getMissionBattleAward(WhichLevelEnum type, SimpleBattleSettings battleSettings) {
        BattleAward battleAward;
        switch (type) {
            case LOW_LEVEL: {
                battleAward = battleSettings.getAwardLowLevel();
                break;
            }
            case HIGH_LEVEL: {
                battleAward = battleSettings.getAwardHighLevel();
                break;
            }
            default: {
                battleAward = battleSettings.getAwardMyLevel();
            }
        }
        return battleAward;
    }

    public boolean isDisableValidateMission() {
        return disableValidateMission;
    }

    public enum BattleType {
        NOT_IN_BATTLE,
        WAIT_START_BATTLE,

        LEARNING,
        BOT,
        BOSS,

        FRIEND,
        PvE,
        PvP,
        ;

        public static BattleType valueOfMissionId(short missionId) {
            if (missionId < 0) {
                return LEARNING;
            } else if (missionId == 0) {
                return BOT;
            } else {
                return BOSS;
            }
        }

        public static BattleType valueOfPvpBattleType(PvpBattleType pvpBattleType) {
            switch (pvpBattleType) {
                case PvE_FRIEND:
                case PvE_PARTNER:
                    return PvE;
                case FRIEND_PvP:
                    return FRIEND;
                default:
                    return PvP;
            }
        }
    }

    public List<Integer> getAwardForDroppedUnits() {
        return awardForDroppedUnits;
    }

    public void onDisconnect(UserProfile profile) {
        if (simpleBattleReconnectEnabled && profile.inBattleState(BattleState.SIMPLE) && profile.getBattleId() > 0) {
            if (simpleBattleReconnectTimeoutInSeconds == 0) {
                // говорим, что игрок полюбому уже не в игре
                onBattleInterrupted(profile, BattleResultEnum.NOT_WINNER_DISCONNECT, AppUtils.currentTimeSeconds());
            } else {
                SimpleBattleStateStructure battleState = new SimpleBattleStateStructure(profile, AppUtils.currentTimeSeconds());
                if (battleState.missionLog != null) {
                    battleState.missionLog.connectionEvents.add(new Time_ConnectionState(AppUtils.currentTimeSeconds(), ConnectionState.disconnect));
                }
                disconnectedInSimpleBattle.put(profile.getId(), battleState);
            }
        }
    }

    public void trackDisconnectedProfiles() {
        disconnectedInSimpleBattle.forEach((profileId, battleState) -> {
            if (AppUtils.currentTimeSeconds() > battleState.disconnectTime + simpleBattleReconnectTimeoutInSeconds) {
                disconnectedInSimpleBattle.remove(profileId);

                UserProfile profile = profileService.getUserProfile(profileId);
                if (profile != null) {// profile == null когда закрывается сезон
                    battleState.mergeTo(profile);
                    onBattleInterrupted(profile, BattleResultEnum.NOT_WINNER_RECONNECT_TIMEOUT, battleState.disconnectTime);
                    profileService.updateSync(profile);
                    log.info("[{}] battleId={} close battle by timeout", profileId, battleState.battleId);
                }
            }
        });
    }

    public ReconnectToSimpleBattleResultStructure onLogin(UserProfile profile, long battleId, int version, short missionId, short turnNum) {
        SimpleBattleStateStructure battleState = disconnectedInSimpleBattle.remove(profile.getId());
        if (battleState != null) {
            short lastTurnNum = lastTurnNum(battleState.missionLog);
            if (battleState.originalBattleId != battleId) {
                if (battleId > 0) {
                    log.error("reconnect failure [battleId={}] battleId mismatched {}", battleId, battleState);
                    reconnectFailure(profile, battleState);
                } else {
                    reconnectFailure(profile, battleState, BattleResultEnum.NOT_WINNER_DISCONNECT);
                }
            } else if (battleState.version != version) {
                log.error("reconnect failure [battleId={}] version [{}] mismatched {}", battleId, version, battleState);
                reconnectFailure(profile, battleState);
            } else if (battleState.missionId != missionId) {
                log.error("reconnect failure [battleId={}] missionId [{}] mismatched {}", battleId, missionId, battleState);
                reconnectFailure(profile, battleState);
            } else if (lastTurnNum > turnNum) {
                log.error("reconnect failure [battleId={}] turnNum [{}] incorrect {}", battleId, turnNum, battleState);
                reconnectFailure(profile, battleState);
            } else {
                String offlineTime = PvpService.formatTimeInSeconds(AppUtils.currentTimeSeconds() - battleState.disconnectTime);
                log.info("reconnect success [battleId={}] offline time {} turnNum={}, missionId={} {}", battleId, offlineTime, turnNum, missionId, battleState);
                battleState.mergeTo(profile);

                if (profile.getMissionLog() != null) {
                    profile.getMissionLog().connectionEvents.add(new Time_ConnectionState(AppUtils.currentTimeSeconds(), ConnectionState.connect));
                }
                profileEventsService.fireProfileEventAsync(RECONNECT, profile,
                        Param.battleId, battleId,
                        Param.missionId, missionId,
                        "turnNum", battleState.lastTurnNum(),
                        "offlineTime", offlineTime
                );
                return new ReconnectToSimpleBattleResultStructure(profile.getBattleId(), battleState.originalBattleId, profile.getMissionId(), lastTurnNum);
            }
        } else if (battleId > 0) {
            log.warn("reconnect failure [battleId={}] battleState is absent", battleId);
        }
        return null;
    }

    private void reconnectFailure(UserProfile profile, SimpleBattleStateStructure battleState) {
        reconnectFailure(profile, battleState, BattleResultEnum.NOT_WINNER_INCORRECT_RECONNECT);
    }

    private void reconnectFailure(UserProfile profile, SimpleBattleStateStructure battleState, BattleResultEnum cause) {
        battleState.mergeTo(profile);
        onBattleInterrupted(profile, cause, battleState.disconnectTime);
    }

}
