package com.pragmatix.app.services;

import com.pragmatix.app.messages.server.AwardGranted;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.persist.HeroicMissionDailyProgressKeeper;
import com.pragmatix.app.settings.*;
import com.pragmatix.gameapp.GameApp;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.services.DailyTaskAvailable;
import com.pragmatix.gameapp.services.IServiceTask;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.threads.Execution;
import com.pragmatix.gameapp.threads.ExecutionContext;
import com.pragmatix.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.pragmatix.app.common.AwardTypeEnum.HEROIC_BOSS_EXTRA_AWARD;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.07.13 14:47
 */
@Service
public class HeroicMissionService implements DailyTaskAvailable {

    private static final Logger log = LoggerFactory.getLogger(HeroicMissionService.class);

    @Resource
    private BattleAwardSettings battleAwardSettings;

    @Resource
    private BattleService battleService;

    @Resource
    private Store store;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private PersistenceService persistenceService;

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private ProfileService profileService;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private GameApp gameApp;

    private static final String stateKeepFileName = "HeroicMissionService.heroicMissionDailyProgresses";

    public static final int HeroicMissionDifficultyLevels = 4;

    private HeroicMissionState[] heroicMissionStates = new HeroicMissionState[HeroicMissionDifficultyLevels];

    private HeroicMissionDailyProgress[] heroicMissionDailyProgresses;

    //для каждой сложности супербоссов держим отдельный фонд, неудачные попытки добавляют в фонд по 40 фузов
    @Value("${HeroicMissionService.defeatContributionMoney:20}")
    private int defeatContributionMoney = 20;

    // ограничиваем награду одному игроку. Не более 5000 фузов
    @Value("${HeroicMissionService.maxSingleAwardMoney:5000}")
    private int maxSingleAwardMoney = 5000;

    //в конце дня, если процент прохождения супербосса меньше 10%, то каждый прошедший игрок получает награду (призовой фонд делится поровну между всеми прошедшими)
    @Value("${HeroicMissionService.minWinThreshold:17}")
    private int minWinThreshold = 17;

    @Value("${HeroicMissionService.enableExtraAward:true}")
    private boolean enableExtraAward = true;

    private boolean initialized = false;

    public void init() {
        boolean needSave = false;
        for(int level = 0; level < HeroicMissionDifficultyLevels; level++) {
            HeroicMissionState state = store.load(getStoreKey(level), HeroicMissionState.class);
            if(state == null) {
                state = getNextState(level, null);
                needSave = true;
            }
            heroicMissionStates[level] = state;
        }
        if(needSave) {
            saveState();
        }

        HeroicMissionDailyProgress[] heroicMissionDailyProgresses = persistenceService.restoreObjectFromFile(HeroicMissionDailyProgress[].class, stateKeepFileName, new HeroicMissionDailyProgressKeeper());
        if(heroicMissionDailyProgresses != null
                && heroicMissionDailyProgresses.length == HeroicMissionDifficultyLevels
                && Arrays.stream(heroicMissionDailyProgresses).allMatch(Objects::nonNull)
                ) {
            this.heroicMissionDailyProgresses = heroicMissionDailyProgresses;
        } else {
            this.heroicMissionDailyProgresses = newHeroicMissionDailyProgresses.get();
        }

        initialized = true;
    }

    private Supplier<HeroicMissionDailyProgress[]> newHeroicMissionDailyProgresses = () ->
            IntStream.range(0, HeroicMissionDifficultyLevels).mapToObj(i -> new HeroicMissionDailyProgress()).toArray(HeroicMissionDailyProgress[]::new);

    public void persistToDisk() {
        persistenceService.persistObjectToFile(heroicMissionDailyProgresses, stateKeepFileName, new HeroicMissionDailyProgressKeeper());
    }

    private String getStoreKey(int level) {
        return "HeroicMissionLevel.Level_" + level;
    }

    @Override
    public IServiceTask getDailyTask() {
        return new IServiceTask() {
            @Override
            public void runServiceTask() {
                ExecutionContext context = new ExecutionContext(gameApp);
                Execution.EXECUTION.set(context);

                if(enableExtraAward) {
                    for(int i = 0; i < heroicMissionDailyProgresses.length; i++) {
                        HeroicMissionDailyProgress progress = heroicMissionDailyProgresses[i];
                        int defeatCount = progress.defeatCount.get();
                        int winCount = progress.getWinCount();
                        if(winCount > 0 && defeatCount > 0) {
                            int winPercent = winCount * 100 / (defeatCount + winCount);
                            if(winPercent < minWinThreshold) {
                                int awardMoney = Math.min(maxSingleAwardMoney, defeatCount * defeatContributionMoney / winCount);
                                int superBossLevel = i + 1;
                                Server.sysLog.info("heroic mission level:{} defeatCount={}, winCount={}, winPercent={}, awardMoney={}",
                                        superBossLevel, defeatCount, winCount, winPercent, awardMoney);

                                GenericAward award = GenericAward.builder().addMoney(awardMoney).build();
                                for(Long winner : progress.winners) {
                                    UserProfile profile = profileService.getUserProfile(winner);
                                    if(profile != null) {
                                        List<GenericAwardStructure> genericAwardStructures = profileBonusService.awardProfile(award, profile, HEROIC_BOSS_EXTRA_AWARD,
                                                "superBossLevel", superBossLevel,
                                                "winPercent", winPercent
                                        );
                                        profileService.updateSync(profile);

                                        if(profile.isOnline()) {
                                            Sessions.getOpt(profile).ifPresent(session ->
                                                    Messages.toUser(new AwardGranted(HEROIC_BOSS_EXTRA_AWARD, genericAwardStructures, "" + (superBossLevel - 1), session.getKey()), profile)
                                            );
                                        } else {
                                            dailyRegistry.addOfflineAward(profile.getId(), new LoginAwardStructure(HEROIC_BOSS_EXTRA_AWARD, genericAwardStructures, "" + (superBossLevel - 1)));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HeroicMissionState[] stateByLevel = new HeroicMissionState[HeroicMissionDifficultyLevels];
                for(int level = 0; level < HeroicMissionDifficultyLevels; level++) {
                    HeroicMissionState prevState = HeroicMissionService.this.heroicMissionStates[level];
                    stateByLevel[level] = getNextState(level, prevState);
                }
                HeroicMissionService.this.heroicMissionStates = stateByLevel;
                heroicMissionDailyProgresses = newHeroicMissionDailyProgresses.get();

                saveState();
                persistToDisk();

                Server.sysLog.info("Shift heroic missions state:");
                for(int level = 0; level < HeroicMissionDifficultyLevels; level++) {
                    HeroicMissionState state = stateByLevel[level];
                    Server.sysLog.info(String.format("\tLevel_%s: missions=%s map=%s", level, Arrays.toString(state.getCurrentMissionIds()), state.getMapId()));
                }
            }

            @Override
            public boolean isInitialized() {
                return initialized;
            }
        };
    }

    @Override
    public DailyTaskAvailable dependsOn() {
        // запускаем DailyTask после того, как очистит DailyStore
        return dailyRegistry;
    }

    public static String missionIdsAsString(short[] missionIds) {
        try {
            return missionIds[0] + "_" + missionIds[1];
        } catch (Exception e) {
            log.error(e.toString(), e);
            return "";
        }
    }

    public static short[] missionIdsAsArray(String missionIds) {
        try {
            String[] ss = missionIds.split("_");
            return new short[]{Short.valueOf(ss[0]), Short.valueOf(ss[1])};
        } catch (NumberFormatException e) {
            log.error(e.toString(), e);
            return new short[0];
        }
    }

    public int getHeroicMissionLevel(short[] missionIds) {
        String key = missionIdsAsString(missionIds);
        Integer level = battleAwardSettings.getHeroicMissionLevels().get(key);
        if(level == null){
            log.error("Configured difficulty level not found by heroic boss key [" + key + "]");
            level = 0;
        }
        return level;
    }

    private HeroicMissionState getNextState(int heroicMissionLevel, @Null HeroicMissionState prevState) {
        HeroicMissionState result = new HeroicMissionState();
        if(prevState == null) {
            String nextMissionKey = getNextMissionKey(heroicMissionLevel, null);
            result.setCurrentMission(nextMissionKey);
            HeroicMission heroicMission = battleAwardSettings.getHeroicMissions().get(nextMissionKey);
            result.setCurrentMissionMap(getNextMissionsMap(heroicMission, -1));
            result.setLastMissionsMap(new HashMap<String, Integer>());
            ArrayList<String> missionsHistory = new ArrayList<>();
            missionsHistory.add(nextMissionKey);
            result.setMissionsHistory(missionsHistory);
        } else {
            List<String> missionsHistory = prevState.getMissionsHistory();
            missionsHistory.add(prevState.getCurrentMission());
            if(missionsHistory.size() > battleAwardSettings.getHeroicMissionHistoryDeep().get(heroicMissionLevel)) {
                missionsHistory.remove(0);
            }
            String nextMissionKey = getNextMissionKey(heroicMissionLevel, prevState);
            result.setCurrentMission(nextMissionKey);
            HeroicMission heroicMission = battleAwardSettings.getHeroicMissions().get(nextMissionKey);
            Integer prevMapId = prevState.getLastMissionsMap().get(nextMissionKey);
            result.setCurrentMissionMap(getNextMissionsMap(heroicMission, prevMapId != null ? prevMapId : -1));
            result.setLastMissionsMap(new HashMap<>(prevState.getLastMissionsMap()));
            result.setMissionsHistory(new ArrayList<>(missionsHistory));
        }
        result.getLastMissionsMap().put(result.getCurrentMission(), result.getCurrentMissionMap());
        return result;
    }

    private int getNextMissionsMap(HeroicMission heroicMission, Integer prevMapId) {
        List<Integer> maps = new ArrayList<>(heroicMission.getMaps());
        if(prevMapId > 0) {
            maps.remove(prevMapId);
        }
        return maps.get(new Random().nextInt(maps.size()));
    }

    private String getNextMissionKey(int heroicMissionLevel, @Null HeroicMissionState prevState) {
        List<String> heroicMissionKeySet = new ArrayList<>();
        for(Map.Entry<String, HeroicMission> entry : battleAwardSettings.getHeroicMissions().entrySet()) {
            int level = battleAwardSettings.getLevel(entry.getValue().getKey());
            if(level == heroicMissionLevel) {
                heroicMissionKeySet.add(entry.getKey());
            }
        }
        if(prevState != null) {
            heroicMissionKeySet.removeAll(prevState.getMissionsHistory());
        }
        return heroicMissionKeySet.get(new Random().nextInt(heroicMissionKeySet.size()));
    }

    public void saveState() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                for(int i = 0; i < heroicMissionStates.length; i++) {
                    HeroicMissionState state = heroicMissionStates[i];
                    store.save(getStoreKey(i), state);
                }
            }
        });
    }

    public HeroicMissionState[] getHeroicMissionStates() {
        return heroicMissionStates;
    }

    public HeroicMissionDailyProgress[] getHeroicMissionDailyProgresses() {
        return heroicMissionDailyProgresses;
    }

    public int getDefeatContributionMoney() {
        return defeatContributionMoney;
    }

    public boolean validateSuperBossMission(short[] missionIds, long mapId) {
        if(battleService.isDisableValidateMission()) {
            return true;
        }
        boolean validState = false;
        for(HeroicMissionState state : heroicMissionStates) {
            if(Arrays.equals(missionIds, state.getMissionIds()) && mapId == state.getMapId()) {
                validState = true;
                break;
            }
        }
        return validState;
    }

    public void onDefeatBattle(short[] missionIds, long mapId, Long profileId) {
        try {
            if(validateSuperBossMission(missionIds, mapId)) {
                heroicMissionDailyProgresses[getHeroicMissionLevel(missionIds)].defeatCount.incrementAndGet();
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    public void onWinBattle(short[] missionIds, long mapId, Long profileId) {
        try {
            if(validateSuperBossMission(missionIds, mapId)) {
                heroicMissionDailyProgresses[getHeroicMissionLevel(missionIds)].winners.add(profileId);
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    public void onWipeProfile(UserProfile profile) {
        for(HeroicMissionDailyProgress progress : heroicMissionDailyProgresses) {
            if(progress.winners.remove(profile.getId())) {
                progress.defeatCount.decrementAndGet();
            }
        }
    }

}
