package com.pragmatix.app.services;

import com.pragmatix.app.common.BanType;
import com.pragmatix.app.common.BattleResultEnum;
import com.pragmatix.app.common.CheatTypeEnum;
import com.pragmatix.app.messages.client.DistributePoints;
import com.pragmatix.app.messages.client.EndBattle;
import com.pragmatix.app.messages.client.EndTurn;
import com.pragmatix.app.messages.structures.*;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.app.settings.BattleAwardSettings;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.gameapp.sessions.Connection;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.pvp.messages.battle.client.PvpActionEx;
import com.pragmatix.pvp.model.PvpBattleLog;
import com.pragmatix.pvp.services.matchmaking.GroupHpService;
import com.pragmatix.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.pragmatix.app.common.BattleResultEnum.WINNER;
import static java.lang.Math.abs;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * Проверка и бан читеров
 */
@Service
public class CheatersCheckerService {

    private static final Logger log = LoggerFactory.getLogger(CheatersCheckerService.class);
    /**
     * максимальное количество побед в подряд меньше чем за 10 сек
     */
    private static final byte MAX_CHEATS_INSTANT_WIN = 7;
    /**
     * максимальное количество обысков домика с несуществующим id
     */
    private static final byte MAX_SEARCH_NOT_EXISTENT_USER = 3;
    /**
     * кол-во быстрых обысков подряд
     */
    private static final byte MAX_INSTANT_SEARCH = 2;
    /**
     * допустимое время между обысками домиков друзей
     */
    private static final int HOUSE_SEARCH_DELAY = 1500;
    /**
     * допустимое время между платежами от одного id
     */
    private static final int PAYMENT_DELAY = 500;

    private static final int MAX_ITEMS_IN_PURCHASE = 300;
    /**
     * смело баним игрока, если число его ходов в два раза > ходов босса
     * (для честного игрока число ходов игрока должно быть равно или даже меньше)
     */
    private static double MAX_PLAYER_TO_BOSS_TURNS_RATIO = 2.0;

    /**
     * с этого id начинаются зарезервированные коды для урона от не-оружий (см. items-beans.xml)
     */
    private static int UNREAL_WEAPON_ID = 10000;
    private static int SINK_WEAPON_ID = UNREAL_WEAPON_ID + 0;
    private static int HEAL_WEAPON_ID = UNREAL_WEAPON_ID + 3;

    /**
     * лимит друзей на сети
     */
    @Value("${social.friendsLimit:10000}")
    private int socialFriendsLimit = 10000;

    @Resource
    private BanService banService;

    @Resource
    private PersistenceService persistenceService;

    @Resource
    private GroupHpService groupHPService;

    @Resource
    private ProfileService profileService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private BattleAwardSettings battleAwardSettings;

    @Value("${CheatersCheckerService.fillExcludedFromRubyAwardSet:true}")
    private boolean fillExcludedFromRubyAwardSet = true;
    /**
     * id игроков уличенных в "накрутке" рубинов
     */
    private volatile Set<Long> excludedFromRubyAward = new HashSet<>();

    public static final String keepFileName = "CheatersCheakerService.excludedFromRubyAward";

    private boolean initialized = false;

    @Value("${CheatersCheckerService.validateMissionLogTotals:true}")
    public boolean validateMissionLogTotals = true;

    @Value("${CheatersCheckerService.alwaysStoreDetailedHpInLog:true}")
    private boolean alwaysStoreDetailedHpInLog = true;

    @Value("${CheatersCheckerService.minPossibleBossHP:170}")
    private int minPossibleBossHP = 170;

    @Value("${CheatersCheckerService.maxPossibleDamagePerTurn:10000}")
    private int maxPossibleDamagePerTurn = 10000;

    @Value("${CheatersCheckerService.maxNormalDamagePerTurn:1000}")
    private int maxNormalDamagePerTurn = 1000;

    /**
     * допустимая погрешность расхождения HP / дамага
     */
    @Value("${CheatersCheckerService.hpTolerance:25}")
    private int hpTolerance = 25;

    /**
     * максимальное допустимое количество выстрелов из одного оружия за ход (среди всех оружий, кроме excludedWeapons)
     */
    @Value("${CheatersCheckerService.maxPossibleWeaponShotsPerTurn:6}")
    private int maxPossibleWeaponShotsPerTurn;

    /**
     * подозрительная длительность для хода в боссах (допустимая, если человек долго думал - но таким пользуются чтобы обходить {@link CheatTypeEnum#INSTANT_WIN})
     */
    @Value("${CheatersCheckerService.suspiciousTurnDuration:100000}")
    private int suspiciousTurnDuration = 100000; // NB: временно отключено

    /**
     * номер босса, начиная с которого проверяем на {@link CheatTypeEnum#FAST_WIN}
     */
    private int fastWinMinBossId = 21;

    /**
     * подозрительная длительность для победы над боссами начиная с {@link CheatersCheckerService#fastWinMinBossId}
     */
    private int fastWinDuration = 3 * 60;

    /**
     * оружия, для которых не осуществляется контроль за количеством выстрелов
     */
    @Value("#{unlimitedUseWeapons}")
    private Set<Integer> unlimitedUseWeapons = new HashSet<>();
    /**
     * Оружия, для которых один выстрел задается двумя или более одинаковыми экшнами
     */
    @Value("#{multiClickWeapons}")
    private Map<Integer, Integer> multiClickWeapons = new HashMap<>();

    enum Team {
        PLAYER(true),
        BOSS(false);

        public final boolean isPlayerTeam;

        Team(boolean isPlayerTeam) {
            this.isPlayerTeam = isPlayerTeam;
        }

        public static Team of(boolean isPlayerTeam) {
            return isPlayerTeam ? PLAYER : BOSS;
        }

        public static Team of(TurnStructure turn) {
            return Team.of(turn.isPlayerTurn);
        }

        public static Team of(BossWormStructure struct) {
            return Team.of(struct.isPlayerTeam);
        }
    }

    public void init() {
        if(fillExcludedFromRubyAwardSet) {
            excludedFromRubyAward = persistenceService.restoreObjectFromFile(Set.class, keepFileName);
            if(excludedFromRubyAward != null) {
                Server.sysLog.info("CheatersCheckerService (restore): will excluded from ruby award [{}] cheaters", excludedFromRubyAward.size());
            } else {
                fillExcludedFromRubyAwardSet();
            }
        }
        initialized = true;
    }

    /**
     * если подозрительно много друзей проверяем их у контакта
     *
     * @param profileId    профайл
     * @param friendsCount
     */
    public boolean checkFriendsCount(Long profileId, int friendsCount) {
        //если id больше лимита соцсети то это сразу читер
        if(friendsCount > socialFriendsLimit) {
            if(profileId != null) {
                //Закрываем соединение с читером
                BanType cheatReason = BanType.BAN_FOR_FRIENDS_LIMIT_EXCEED;
                banService.addToBanList(profileId, cheatReason, cheatReason.caption);
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * быстрая победа(менее 10сек)
     */
    public boolean checkInstantWin(UserProfile profile, long battleTime, BattleResultEnum battleResult) {
        // если бой длился меньше чем 10 секунд и при этом
        // он еще и победил, то не засчитываем такой бой
        if(battleTime < 10000 && battleResult == WINNER) {
            Optional<MissionLogStructure> mLog = Optional.ofNullable(profile.getMissionLog());

            byte newValue = (byte) (profile.getCheatInstantWin() + 1);
            profile.setCheatInstantWin(newValue);
            //если читерских действий больше 8 то обнуляем читера
            if(newValue >= MAX_CHEATS_INSTANT_WIN) {
                //добавляем читера в бан лист
                punish(Connections.get(), profile, BanType.BAN_FOR_INSTANT_WIN);
                mLog.ifPresent(m -> m.setReason(CheatTypeEnum.INSTANT_WIN));
                return false;
            } else {
                // запоминаем как подозрительного
                mLog.ifPresent(m -> m.setReason(CheatTypeEnum.INSTANT_WIN_MAYBE));
            }
        } else {
            //обнуляем счетчик чит боёв т.к. 1 раз противник мог сдаться или действительно была быстрая победа
            profile.setCheatInstantWin((byte) 0);
        }
        return true;
    }

    public enum ValidationResult {
        OK,
        IGNORE,
        FAIL,
        BANNED
    }

    public ValidationResult validateEndTurnMsg(UserProfile profile, EndTurn msg) {
        if(msg.battleId == 0) {
            log.error(String.format("hack detected! msg.battleId is empty, battleStartTime=%s", new Time(profile.getStartBattleTime())));
            return ValidationResult.IGNORE;
        } else if(profile.getBattleId() != msg.battleId) {
            log.error(String.format("hack detected! profile.battleId:%s != msg.battleId:%s, battleStartTime=%s", profile.getBattleId(), msg.battleId, new Time(profile.getStartBattleTime())));
//            profile.incrementCheatBattleIdCount();
//            if(profile.getCheatBattleIdCount() >= MAX_CHEATS_BATTLE_ID) {
//                //punish(Connections.get(), profile, BanType.BAN_FOR_BATTLE_ID);
//            }
            return ValidationResult.IGNORE;
        } else if(profile.getMissionId() != msg.missionId) {
            log.error(String.format("hack detected! profile.missionId:%s != msg.missionId:%s, battleStartTime=%s", profile.getMissionId(), msg.missionId, new Time(profile.getStartBattleTime())));
            return ValidationResult.IGNORE;
        } else if(msg.banType > 0) {
            BanType banType = BanType.valueOf(msg.banType);
            if(banType != null) {
                punish(Connections.get(), profile, banType, banType.toString() + ": " + msg.banNote);
            } else {
                log.error("wrong banType in command {}", msg);
                Connections.get().close();
            }
            return ValidationResult.BANNED;
        } else if(msg.turn.turnNum != BattleService.lastTurnNum(profile.getMissionLog()) + 1) {
            log.warn("[battleId={}] wrong turnNum [{}] expected [{}]", msg.battleId, msg.turn.turnNum, BattleService.lastTurnNum(profile.getMissionLog()) + 1);
            return ValidationResult.IGNORE;
        } else {
            return ValidationResult.OK;
        }
    }

    /**
     * Проверка id боя в команде которую прислал клиент
     *
     * @param profile профайл игрока
     * @param msg     команда окончания боя
     * @return false если читерил
     */
    public boolean validateEndBattleMsg(UserProfile profile, EndBattle msg) {
        // TODO : сверять что всё небесконечное оружие за лог действительно пришло в msg.items?
        int battleId = (int) profile.getBattleId();
        Optional<MissionLogStructure> mLog = Optional.ofNullable(profile.getMissionLog());
        if(msg.battleId == 0) {
            log.error(String.format("hack detected! msg.battleId is empty, battleStartTime=%s", new Time(profile.getStartBattleTime())));
            mLog.ifPresent(m -> m.setReason(CheatTypeEnum.PROTOCOL_HACK));
            return false;
        } else if(battleId != msg.battleId) {
            log.error(String.format("hack detected! profile.battleId:%s != msg.battleId:%s, battleStartTime=%s", profile.getBattleId(), msg.battleId, new Time(profile.getStartBattleTime())));
            mLog.ifPresent(m -> m.setReason(CheatTypeEnum.PROTOCOL_HACK));
            return false;
        } else if(profile.getMissionId() != msg.missionId) {
            log.error(String.format("hack detected! profile.missionId:%s != msg.missionId:%s, battleStartTime=%s", profile.getMissionId(), msg.missionId, new Time(profile.getStartBattleTime())));
            mLog.ifPresent(m -> m.setReason(CheatTypeEnum.PROTOCOL_HACK));
//            profile.incrementCheatBattleIdCount();
//            if(profile.getCheatBattleIdCount() >= MAX_CHEATS_BATTLE_ID) {
//                //punish(Connections.get(), profile, BanType.BAN_FOR_BATTLE_ID);
//            }
            return false;
        } else if(msg.banType > 0) {
            BanType banType = BanType.valueOf(msg.banType);
            if(banType != null) {
                punish(Connections.get(), profile, banType, banType.toString() + ": " + msg.banNote);
            } else {
                log.error("wrong banType in command {}", msg);
                Connections.get().close();
            }
            mLog.ifPresent(m -> m.setReason(CheatTypeEnum.PROTOCOL_HACK));
            return false;
        } else {
            int resultDecoded = (int) (msg.resultRaw - msg.battleId);
            BattleResultEnum battleResult = BattleResultEnum.valueOf(resultDecoded);
            if(battleResult == null) {
                log.error("hack detected! bad battleResult: {}", resultDecoded);
                mLog.ifPresent(m -> m.setReason(CheatTypeEnum.PROTOCOL_HACK));
                return false;
            } else {
                // ставим очищеное значение конца боя
                msg.result = battleResult;
                return true;
            }
        }
    }

    /**
     * Проверка на взлом магазина через переполнение инта посылкой большого количества предметов
     *
     * @param profile   профайл игрока
     * @param itemCount
     * @return true если читер
     */
    public boolean checkItemCount(UserProfile profile, int itemCount) {
        if(itemCount > MAX_ITEMS_IN_PURCHASE) {
            //добавляем читера в бан лист
            banService.addToBanList(profile.getId(), BanType.BAN_FOR_SHOP_HACK);

            Connections.get().close();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Проверка на взлом параметров червя через переполнение инта посылкой большого количества парамероы для прокачки
     *
     * @param profile          профайл игрока
     * @param distributePoints структура хронящая количество
     * @return true если читер
     */
    public boolean checkDistributePoints(UserProfile profile, DistributePoints distributePoints) {
        if(distributePoints.armor > 100 || distributePoints.attack > 100) {
            //добавляем читера в бан лист
            banService.addToBanList(profile.getId(), BanType.BAN_FOR_PARAM_HACK);
            Connections.get().close();
            return true;
        } else {
            return false;
        }
    }

    /**
     * проверяем профайл игрока на допустимый промежуок
     * между обысками домиков игроков и если время между обыска
     * мешьше допустимого, то заносит игрока в бан лист
     *
     * @param profile профайл игрока
     * @return true если игрок попался на читерстве
     */
    public boolean checkSearchHouseDelay(UserProfile profile) {
        if(profile.getLastSearchFriendTime() + HOUSE_SEARCH_DELAY >= System.currentTimeMillis()) {
            profile.setCheatInstantHouseSearch((byte) (profile.getCheatInstantHouseSearch() + 1));
            if(profile.getCheatHouseSearch() >= MAX_INSTANT_SEARCH) {
                punish(Connections.get(), profile, BanType.BAN_FOR_HOUSE_SEARCH);
            }
            return true;
        } else {
            profile.setCheatInstantHouseSearch((byte) 0);
            return false;
        }
    }

    /**
     * проверяем профайл игрока на допустимый промежуок
     * между платежами и если время между платежами
     * мешьше допустимого, то отот игрок спамит сервер
     *
     * @param profile профайл игрока
     * @return true если игрок попался на читерстве
     */
    public boolean checkPaymentDelay(UserProfile profile) {
        // отключаем проверку, вернем если будут преценденты
        // поле LastPaymentTime теперь содержит время последнего успешного платежа и сохраняется в базу
//        long curTime = System.currentTimeMillis();
//        boolean result = profile.getLastPaymentTime() + PAYMENT_DELAY >= curTime;
//        profile.setLastPaymentTime(curTime);
//        return result;
        return false;
    }

    /**
     * обыск несуществующего игрока
     *
     * @param profile читер
     */
    public void searchNonExistentUser(UserProfile profile) {
        byte newValue = (byte) (profile.getCheatHouseSearch() + 1);
        profile.setCheatHouseSearch(newValue);
        if(newValue >= MAX_SEARCH_NOT_EXISTENT_USER) {
            punish(Connections.get(), profile, BanType.BAN_FOR_SEARCH_NOT_EXISTENT_USER);
        }
    }

    /**
     * наказание за читерство. делим все пополам и баним
     *
     * @param connection  соединение с клиентом
     * @param profile     читер
     * @param cheatReason причина наказания
     */
    private void punish(Connection connection, UserProfile profile, BanType cheatReason) {
        punish(connection, profile, cheatReason, cheatReason.toString());
    }

    /**
     * @param connection  соединение с клиентом
     * @param profile     юзер
     * @param cheatReason как читерил
     * @param note        доп сведения
     */
    public void punish(Connection connection, UserProfile profile, BanType cheatReason, String note) {
        //добавляем читера в бан лист
        banService.addToBanList(profile.getId(), cheatReason, note);
        try {
            connection.close();
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    /**
     * Валидация журнала боя: 1) начинаем запись боя
     *
     * @param profile профиль игрока
     */
    public void logMissionStart(UserProfile profile) {
        int playerHP = groupHPService.calculateGroupHp(profileService.getUserProfileStructure(profile));
        profile.createMissionLog().start(playerHP);
    }

    /**
     * Валидация журнала боя: 2) записываем очередной ход
     *
     * @param profile профиль игрока
     * @param turn    итог хода
     */
    public void logMissionTurn(UserProfile profile, TurnStructure turn) {
        MissionLogStructure mLog = profile.getMissionLog();
        if(mLog != null) {
            if(turn.turnNum == 0 && !mLog.initedHP) {
                // если нулевой ход - запоминаем начальное здоровье босса и игрока и не запоминаем данную TurnStructure как ход
                mLog.playerHP = 0;
                mLog.bossHP = 0;
                applyNewBornHP(mLog, turn.births);
                mLog.initedHP = true;
            }
            mLog.add(turn);
        }
    }

    public void logMissionEnd(UserProfile profile, EndBattle msg) {
        MissionLogStructure mLog = profile.getMissionLog();
        if(mLog != null) {
            mLog.finish(msg.totalTurnsCount, msg.totalDamageToPlayer, msg.totalDamageToBoss, msg.totalUsedItems);
        }
    }

    /**
     * Валидация журнала боя: 3) валидируем итог журнал боя в конце боя
     * <p>
     * Проверка выполняется только для боёв с боссами И завершившихся победой
     *
     * @param profile      профиль игрока
     * @param battleResult результат боя (победа/поражение/...)
     * @return true - если клиентский и серверный итоги сошлись, false - если игрок читерил
     */
    public CheatTypeEnum validateMissionLogTotals(UserProfile profile, BattleResultEnum battleResult) {
        MissionLogStructure mLog = profile.getMissionLog();
        if(mLog != null && validateMissionLogTotals) {
            // валидируем только победы над боссами, для поражений и побед не над боссами тихо возвращаем "не проверялось", но valid=true
            if(battleResult == WINNER && battleAwardSettings.getAwardSettingsMap().get(profile.getMissionId()).isBossBattle()) {
                if(mLog.turns.isEmpty()) {
                    mLog.setReason(CheatTypeEnum.NO_TURNS);
                    log.error("BOSS hack detected: mission log contains 0 turns");
                } else {
                    mLog.setReason(CheatTypeEnum.OK); // изначально считаем OK, далее перетираем это значение
                    if(!mLog.initedHP || mLog.turns.get(0).turnNum != 0) {
                        mLog.setReason(CheatTypeEnum.INIT_WRONG);
                        log.error("BOSS hack detected: HP was not initialized, probably initialization 0'th turn was not sent");
                    }
                    int actualTurnsCount = mLog.turns.size() - 1; // не считая нулевой ход-инициализацию
                    if(mLog.supposedTotalTurnsCount != actualTurnsCount || actualTurnsCount < 1) {
                        mLog.setReason(CheatTypeEnum.TURN_COUNT_MISMATCH, mLog.supposedTotalTurnsCount - Math.max(actualTurnsCount, 1));
                        log.error("BOSS hack detected: supposedTotalTurnsCount:{} != actualTurnsCount:{} or < 1", mLog.supposedTotalTurnsCount, actualTurnsCount);
                    }
                    // сверяем изначальный суммарный HP, посчитанный в GroupHpService с суммой HP по появившимся червям команды на нулевом ходу
                    if(mLog.startPlayerHP != mLog.playerHP) {
                        int desynchValue = mLog.playerHP - mLog.startPlayerHP;
                        if(abs(desynchValue) > hpTolerance) {
                            // если расхождение превышает порог
                            mLog.setReason(CheatTypeEnum.PLAYER_HP_MISMATCH, desynchValue);
                            log.error("BOSS hack detected: wrong start HP: startPlayerHP(server):{} != playerHP(client):{}", mLog.startPlayerHP, mLog.playerHP);
                        } else {
                            mLog.setReason(CheatTypeEnum.PLAYER_HP_MISMATCH_MAYBE, desynchValue);
                            log.warn("BOSS hack maybe? wrong start HP: startPlayerHP(server):{} != playerHP(client):{}", mLog.startPlayerHP, mLog.playerHP);
                        }
                    }

                    int actualDamageToPlayer = 0; // сумма уронов игроку по всем ходам
                    int actualDamageToBoss = 0;   // сумма уронов игроку по всем ходам
                    int actualTurnNum = 0;
                    int actualBossTurnsCount = 0;
                    boolean lastTurnWasPlayerTurn = false;
                    Map<Integer, Integer> actualTotalItems = new HashMap<>();
                    Map<Integer, Integer> currentTurnItems = new HashMap<>();
                    int turnStartTime = (int) (profile.getStartBattleTime() / 1000L); // время начала текущего хода (timestamp в секундах)
                    for(TurnStructure turn : mLog.turns) {
                        if(turn.turnNum == 0) {
                            continue; // ход-инициализацию пропускаем
                        }
                        actualTurnNum++;
                        actualDamageToPlayer += turn.damageToPlayer;
                        actualDamageToBoss += turn.damageToBoss;
                        boolean bossSinkHappened = false;
                        if(turn.isPlayerTurn) {
                            currentTurnItems.clear();
                            backpackToMap(turn.items, actualTotalItems);
                            backpackToMap(turn.items, currentTurnItems);

                            if(lastTurnWasPlayerTurn) {
                                mLog.setReason(CheatTypeEnum.BOSS_TURN_SKIPPED);
                                log.error("BOSS hack detected: boss turn skipped at turn {}!", turn.turnNum);
                            }
                            lastTurnWasPlayerTurn = true;

                            currentTurnItems.forEach((itemId, shotsCount) -> {
                                if(shotsCount > maxPossibleWeaponShotsPerTurn && itemId < UNREAL_WEAPON_ID && !unlimitedUseWeapons.contains(itemId)) {
                                    mLog.setReason(CheatTypeEnum.WEAPON_USAGE_HIGH, shotsCount);
                                    log.error("BOSS hack detected: weapon {} used too many times per turn: ({} > maxPossibleWeaponShotsPerTurn:{}) all shots: {}",
                                            itemId, shotsCount, maxPossibleWeaponShotsPerTurn, currentTurnItems.entrySet().stream().map(e -> e.getKey() + "->" + e.getValue()).collect(joining(",")));
                                }
                            });

                            // проверка на утопление игроком босса без оружия на своему ходу - босс сам не может сместиться и упасть во время хода игрока, если не был применен, например, "чит на матрицу"
                            // TODO: может ли эта валидация вызывать false positive для таких боссов как Иллюзионист, Ромео и Джульетта и Ученый - которые всё же иногда движутся во время хода игрока?
                            bossSinkHappened = containsDeath(turn, Team.BOSS) && !containsDeath(turn, Team.PLAYER) && hasSinkHappened(turn);
                            if(bossSinkHappened && !isRealWeaponUsed(turn, Team.PLAYER)) {
                                mLog.setReason(CheatTypeEnum.SINK_WITHOUT_WEAPON);
                                log.error("BOSS hack detected: boss sink, but player did not use any weapon at turn {}!", turn.turnNum);
                            }
                        } else {
                            actualBossTurnsCount++;
                            lastTurnWasPlayerTurn = false;
                        }
                        if(turn.turnNum != actualTurnNum) {
                            mLog.setReason(CheatTypeEnum.TURN_NUMBER_MISMATCH);
                            log.error("BOSS hack detected: turn.turnNum:{} != actualTurnNum:{}", turn.turnNum, actualTurnNum);
                        }
                        // проверка на нереальный damage боссу за ход
                        int completeDamageToBoss = abs(turn.damageToBoss) + totalDeathDamageOverflow(turn, Team.BOSS);
                        if(completeDamageToBoss > maxPossibleDamagePerTurn) {
                            mLog.setReason(CheatTypeEnum.WEAPON_DAMAGE_HIGH, completeDamageToBoss - maxPossibleDamagePerTurn);
                            log.error("BOSS hack detected: completeDamageToBoss:{} > maxPossibleDamagePerTurn:{}", completeDamageToBoss, maxPossibleDamagePerTurn);
                        } else if(completeDamageToBoss > maxNormalDamagePerTurn && !bossSinkHappened) {
                            mLog.setReason(CheatTypeEnum.WEAPON_DAMAGE_HIGH_MAYBE, completeDamageToBoss - maxNormalDamagePerTurn);
                            log.error("BOSS hack detected: completeDamageToBoss:{} > maxNormalDamagePerTurn:{}", completeDamageToBoss, maxNormalDamagePerTurn);
                        }
                        applyNewBornHP(mLog, turn.births);

                        if(mLog.connectionEvents.isEmpty()) {
                            // QUESTION: точно ли?
                            int turnDuration = turn.endTime - turnStartTime;
                            if(turnDuration > suspiciousTurnDuration) {
                                mLog.setReason(CheatTypeEnum.LONG_TURN, turnDuration - suspiciousTurnDuration);
                                log.error("BOSS hack detected: very long turn: {} sec > {} sec", turnDuration, suspiciousTurnDuration);
                            }
                        } else {
                            mLog.setReason(CheatTypeEnum.WIN_IN_OFFLINE, mLog.offlineTime());
                        }
                        turnStartTime = turn.endTime;
                    }
                    Map<Integer, Integer> supposedTotalItems = backpackToMap(mLog.supposedTotalItems, new HashMap<Integer, Integer>());

                    int actualPlayerTurnsCount = actualTurnsCount - actualBossTurnsCount;
                    if(actualPlayerTurnsCount > actualBossTurnsCount * MAX_PLAYER_TO_BOSS_TURNS_RATIO) {
                        mLog.setReason(CheatTypeEnum.BOSS_TURN_NEVER);
                        log.error("BOSS hack detected: boss almost never had turn ({} turns) while player had {} turns!!", actualBossTurnsCount, actualPlayerTurnsCount);
                    }
                    if(actualPlayerTurnsCount < 1) {
                        mLog.setReason(CheatTypeEnum.INSTANT_WIN);
                        log.error("BOSS hack detected: player win while making {} turns! (out of {} total turns)", actualPlayerTurnsCount, actualTurnsCount);
                    }

                    if(mLog.bossHP < minPossibleBossHP) {
                        mLog.setReason(CheatTypeEnum.BOSS_HP_LOW, mLog.bossHP - minPossibleBossHP);
                        log.error("BOSS hack detected: bossHP:{} < {}", mLog.bossHP, minPossibleBossHP);
                    }
                    if(mLog.supposedTotalDamageToPlayer != actualDamageToPlayer || actualDamageToPlayer >= mLog.playerHP) {
                        int desynchValue = Math.max(actualDamageToPlayer - mLog.supposedTotalDamageToPlayer, actualDamageToPlayer - mLog.playerHP);
                        if((abs(mLog.supposedTotalDamageToPlayer - actualDamageToPlayer) > hpTolerance) || (actualDamageToPlayer >= (mLog.playerHP + hpTolerance))) {
                            // если расхождение превышает порог
                            log.error("BOSS hack detected: supposedTotalDamageToPlayer:{} != actualDamageToPlayer:{} or exceeds playerHP:{}", mLog.supposedTotalDamageToPlayer, actualDamageToPlayer, mLog.playerHP);
                            mLog.setReason(CheatTypeEnum.DAMAGE_TO_PLAYER_MISMATCH, desynchValue);
                        } else {
                            log.warn("BOSS hack maybe? supposedTotalDamageToPlayer:{} != actualDamageToPlayer:{} or exceeds playerHP:{}", mLog.supposedTotalDamageToPlayer, actualDamageToPlayer, mLog.playerHP);
                            mLog.setReason(CheatTypeEnum.DAMAGE_TO_PLAYER_MISMATCH_MAYBE, desynchValue);
                        }
                    }
                    if(mLog.supposedTotalDamageToBoss != actualDamageToBoss || actualDamageToBoss < mLog.bossHP) {
                        int desynchValue = Math.min(mLog.supposedTotalDamageToBoss - actualDamageToBoss, actualDamageToBoss - mLog.bossHP); // min - т.к. у читера обе величины будут отрицательными
                        if((abs(mLog.supposedTotalDamageToBoss - actualDamageToBoss) > hpTolerance) || (actualDamageToBoss < (mLog.bossHP - hpTolerance))) {
                            // если расхождение превышает порог
                            log.error("BOSS hack detected: supposedTotalDamageToBoss:{} != actualDamageToBoss:{} or doesn't reach bossHP:{}", mLog.supposedTotalDamageToBoss, actualDamageToBoss, mLog.bossHP);
                            mLog.setReason(CheatTypeEnum.DAMAGE_TO_BOSS_MISMATCH, desynchValue);
                        } else {
                            log.warn("BOSS hack maybe? supposedTotalDamageToBoss:{} != actualDamageToBoss:{} or doesn't reach bossHP:{}", mLog.supposedTotalDamageToBoss, actualDamageToBoss, mLog.bossHP);
                            mLog.setReason(CheatTypeEnum.DAMAGE_TO_BOSS_MISMATCH_MAYBE, desynchValue);
                        }
                    }
                    if(!supposedTotalItems.equals(actualTotalItems)) {
                        mLog.setReason(CheatTypeEnum.ITEMS_USED_MISMATCH, supposedTotalItems.size() - actualTotalItems.size());
                        log.error("BOSS hack detected: supposedTotalItems:[{}] != actualTotalItems:[{}]", supposedTotalItems.size(), actualTotalItems.size());
                    }

                    int battleDuration = (int) ((System.currentTimeMillis() - profile.getStartBattleTime()) / 1000L);
                    if(profile.getMissionId() >= fastWinMinBossId && battleDuration < fastWinDuration) {
                        mLog.setReason(CheatTypeEnum.FAST_WIN, battleDuration - fastWinDuration);
                        log.error("BOSS hack detected: fast win difficult boss #{} in only {} sec. < fastWinDuration:{} sec", profile.getMissionId(), battleDuration, fastWinDuration);
                    }
                }
            }
            mLog.valid = mLog.getReason().isValid();
            if(!mLog.valid || alwaysStoreDetailedHpInLog) {
                // добавляем в mLog ещё дополнительную отладочную информацию
                groupHPService.getDetailedGroupHp(profileService.getUserProfileStructure(profile), mLog.teamSnapshot);
            }
            return mLog.getReason();
        }
        return CheatTypeEnum.UNCHECKED;
    }

    private static void applyNewBornHP(MissionLogStructure mLog, BossWormStructure[] births) {
        for(BossWormStructure birth : births) {
            if(birth.isPlayerTeam) {
                mLog.playerHP += birth.HP;
            } else {
                mLog.bossHP += birth.HP;
            }
        }
    }

    private static int totalDeathDamageOverflow(TurnStructure turn, Team toWhom) {
        int result = 0;
        for(BossWormStructure death : turn.deaths) {
            if(Team.of(death) == toWhom) {
                // если полученный damage был > HP юнита, то избыток придет в death.HP в виде отрицательного значения
                result += Math.abs(death.HP);
            }
        }
        return result;
    }

    /**
     * @return был ли убит кто-то из членов команды ofWho за ход?
     */
    private static boolean containsDeath(TurnStructure turn, Team ofWho) {
        return stream(turn.deaths).anyMatch(death -> Team.of(death) == ofWho);
    }

    /**
     * @return было ли использовано настоящее оружие (кроме [Утопил], [Лечение] итп) за ход?
     */
    private static boolean isRealWeaponUsed(TurnStructure turn, Team byWho) {
        return Team.of(turn) == byWho && stream(turn.items).anyMatch(item -> item.weaponId != SINK_WEAPON_ID && item.weaponId != HEAL_WEAPON_ID);
    }

    /**
     * @return случилось ли утопление за ход?
     * TODO: 20.05.2016 как отличить, что утонул именно бос, а не игрок?
     */
    private static boolean hasSinkHappened(TurnStructure turn) {
        return stream(turn.items).anyMatch(item -> item.weaponId == SINK_WEAPON_ID); // утопление как раз имеет код UNREAL_WEAPON_ID + 0
    }

    private Map<Integer, Integer> backpackToMap(BackpackItemStructure[] items, Map<Integer, Integer> mapToFill) {
        for(BackpackItemStructure item : items) {
            Integer oldCount = mapToFill.get(item.weaponId);
            if(oldCount == null) {
                mapToFill.put(item.weaponId, item.count);
            } else {
                mapToFill.put(item.weaponId, oldCount + item.count);
            }
        }
        return mapToFill;
    }

    public CheatTypeEnum validatePvpClientAction(PvpBattleLog battleLog, long frame, PvpActionEx.ActionCmd actionCmd, long[] params) {
        PvpBattleLog.Turn turn = battleLog.getCurrentTurn();
        turn.setReason(CheatTypeEnum.OK, frame);
        switch (actionCmd) {
            /*
             * выбор оружия
             */
            case selectWeapon:
            case selectWeaponSrv:
                int newWeaponId = (int) params[0];
                PvpActionEx.ActionCmd expectingShootCommand = PvpActionEx.ActionCmd.valueOf(params[1]);
                if(expectingShootCommand == null || !expectingShootCommand.isShoot()) {
                    turn.setReason(CheatTypeEnum.WEAPON_SELECT_ILLEGAL, frame);
                    log.error("tn{}@fr{}| PVP hack detected: weapon selected {} and asking to wait for {} which is not a shooting command", turn.turnNum, frame, newWeaponId, (expectingShootCommand == null ? params[1] : expectingShootCommand));
                    break;
                }
                turn.currentWeaponId = newWeaponId;
                turn.currentShootCommand = expectingShootCommand;
                turn.consecutiveShotActions = 0;
                turn.shotMightBeCancelled = false;
                if(log.isDebugEnabled()) {
                    log.debug("tn{}@fr{}| Chosen weapon: {} by action {}, now waiting for shoot command {}", turn.turnNum, frame, turn.currentWeaponId, actionCmd, expectingShootCommand);
                }
                break;
            /*
             * выстрел одним из способов
             */
            case charge:
            case release:
            case point:
            case jumpShoot:
                int curWeaponId = turn.currentWeaponId;
                if(curWeaponId == 0) {
                    turn.setReason(CheatTypeEnum.WEAPON_SELECT_ILLEGAL, frame);
                    log.error("tn{}@fr{}| PVP hack detected: no weapon selected when shooting with {}", turn.turnNum, frame, actionCmd);
                    break;
                }
                turn.consecutiveShotActions++;

                // это последний выстрел (true) - или ещё подготовка и ждать другого (false)?
                boolean isExpectedShot = actionCmd == turn.currentShootCommand || actionCmd == PvpActionEx.ActionCmd.jumpShoot;
                // если у данного оружия выстрел задаётся несколькими экшнами: ждать ли дальше (true) - или это уже последний (false)
                Integer multiClickCount = multiClickWeapons.get(curWeaponId);
                boolean waitAnotherShoot = multiClickCount != null && turn.consecutiveShotActions < multiClickCount;

                if(isExpectedShot && !waitAnotherShoot) {
                    turn.shotsByWeapon.putIfAbsent(curWeaponId, new AtomicInteger(0));
                    int shotsCount = turn.shotsByWeapon.get(curWeaponId).incrementAndGet();
                    if(log.isDebugEnabled()) {
                        log.debug("tn{}@fr{}| Shot #{} with weapon: {} by action: {}", turn.turnNum, frame, shotsCount, curWeaponId, actionCmd);
                    }
                    turn.consecutiveShotActions = 0;
                    turn.shotMightBeCancelled = true;

                    if(shotsCount > maxPossibleWeaponShotsPerTurn && !unlimitedUseWeapons.contains(curWeaponId)) {
                        turn.setReason(CheatTypeEnum.PVP_WEAPON_USAGE_HIGH, frame);
                        log.error("tn{}@fr{}| PVP hack detected: weapon {} used too many times: ({} > maxPossibleWeaponShotsPerTurn:{}), all shots: {}",
                                turn.turnNum, frame, curWeaponId, shotsCount, maxPossibleWeaponShotsPerTurn, turn.shotsByWeapon.entrySet().stream().map(e -> e.getKey() + "->" + e.getValue()).collect(joining(",")));
                        break;
                    }
                } else {
                    // - это ещё не сам выстрел, ждем выстрел с помощью currentShootCommand
                    if(log.isDebugEnabled()) {
                        log.debug("tn{}@fr{}| Charging weapon {} by action: {} - not a shot, still waiting for shoot command {}...", turn.turnNum, frame, curWeaponId, actionCmd, turn.currentShootCommand);
                    }
                }
                break;
            /*
             * отмена выстрела
             */
            case cancelShot:
                AtomicInteger curWeaponShots = turn.shotsByWeapon.get(turn.currentWeaponId);
                if(turn.shotMightBeCancelled && curWeaponShots != null) {
                    curWeaponShots.decrementAndGet();
                    if(turn.consecutiveShotActions > 0) {
                        turn.consecutiveShotActions--;
                    }
                    if(log.isDebugEnabled()) {
                        log.debug("tn{}@fr{}| Cancelled last shot with {} by action: {}", turn.turnNum, frame, turn.currentWeaponId, actionCmd);
                    }
                }
                turn.shotMightBeCancelled = false;
                break;
            /*
             * окончание хода
             */
            case endTurn:
                if(turn.finished) {
                    turn.setReason(CheatTypeEnum.ACTION_END_TURN_REPEAT, frame);
                    log.error("tn{}@fr{}| PVP hack detected: command {} received again: turn already finished", turn.turnNum, frame, actionCmd);
                } else {
                    turn.finished = true;
                    if(log.isDebugEnabled()) {
                        log.debug("tn{}@fr{}| Turn finished by action: {}", turn.turnNum, frame, actionCmd);
                    }
                }
                break;
            /*
             * рюкзак - просто логгируется
             */
            case backpackOpen:
            case backpackClose:
                if(log.isDebugEnabled()) {
                    log.debug("tn{}@fr{}| Backpack used by action: {}", turn.turnNum, frame, actionCmd);
                }
                break;
            /*
             * прыжок - просто логгируется
             */
            case jump1:
            case jump2:
                if(log.isDebugEnabled()) {
                    log.debug("tn{}@fr{}| Jump by action: {}", turn.turnNum, frame, actionCmd);
                }
                break;
        }
        turn.valid = turn.getReason().isValid();
        if(!turn.valid) {
            battleLog.setNeedToBeSaved(true);
        }
        return turn.getReason();
    }

    /**
     * обновляем список игроков выбравших свой лимит рубинов за поиски домиков
     */
    private void fillExcludedFromRubyAwardSet() {
        try {
            Server.sysLog.info("CheatersCheckerService: select cheaters...");
            long cheatersCount = jdbcTemplate.queryForObject("SELECT fill_excluded_from_ruby_award()", Long.class);
            Server.sysLog.info("CheatersCheckerService: will excluded from ruby award [{}] cheaters", cheatersCount);
            if(cheatersCount > 0) {
                List<Long> cheatersList = jdbcTemplate.query("SELECT profile_id FROM _excluded_from_ruby_award", new RowMapper<Long>() {
                    @Override
                    public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return rs.getLong("profile_id");
                    }
                });
                excludedFromRubyAward = new HashSet<>(cheatersList);
            } else {
                excludedFromRubyAward = new HashSet<>();
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
            excludedFromRubyAward = new HashSet<>();
        }
    }

    public void persistToDisk() {
        if(initialized && fillExcludedFromRubyAwardSet)
            persistenceService.persistObjectToFile(excludedFromRubyAward, keepFileName);
    }

    public boolean isExcludedFromRubyAward(Long profileId) {
        return excludedFromRubyAward.contains(profileId);
    }

    public void runDailyTask() {
        if(initialized && fillExcludedFromRubyAwardSet)
            fillExcludedFromRubyAwardSet();
    }

    public Set<Long> getExcludedFromRubyAward() {
        return excludedFromRubyAward;
    }

    public boolean validateOfflineBattle(UserProfile profile, long finishLastBattle, EndBattleStructure endBattleResult) {
        // offline можно проходить только обычные миссии
        return endBattleResult.missionId == 0 && endBattleResult.startBattleTime * 1000L > finishLastBattle && endBattleResult.finishBattleTime > endBattleResult.startBattleTime;
    }

}
