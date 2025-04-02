package com.pragmatix.pvp.services;

import com.google.gson.Gson;
import com.pragmatix.app.common.Connection;
import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.app.services.ReactionRateService;
import com.pragmatix.app.services.TrueSkillService;
import com.pragmatix.app.services.rating.RankService;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.GameApp;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.intercom.messages.EndPvpBattleRequest;
import com.pragmatix.intercom.messages.IntercomResponseI;
import com.pragmatix.performance.statictics.Counter;
import com.pragmatix.performance.statictics.StatCollector;
import com.pragmatix.pvp.*;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.PvpProfileStructure;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.server.EndPvpBattleResultStructure;
import com.pragmatix.pvp.messages.battle.server.PvpEndBattle;
import com.pragmatix.pvp.messages.battle.server.PvpSystemMessage;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleStateEnum;
import com.pragmatix.pvp.services.matchmaking.BlackListService;
import com.pragmatix.pvp.services.matchmaking.TeamBattleProposal;
import com.pragmatix.pvp.services.matchmaking.lobby.ProposalStat;
import io.vavr.Tuple;
import jskills.IPlayer;
import jskills.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.pragmatix.pvp.model.BattleParticipant.State.droppedFromBattle;

/**
 * Класс содержит вспомогательные методы для Pvp боя
 */
@Service
public class PvpService {

    private static final Logger log = LoggerFactory.getLogger(PvpService.class);

    private final Logger pvpCdrlogger = LoggerFactory.getLogger("PVP_CDR_LOGGER");

    // новый JSON-логгер, должен заменить pvpCdrlogger. Содержит в себе также PvpBattleLog, в случае если были читы
    private final Logger pvpDetailsCdrLogger = LoggerFactory.getLogger("PVP_DETAILS_CDR_LOGGER");

    @Resource
    private SoftCache cache;

    /**
     * Счетчик pvp боёв
     */
    private AtomicLong battleCount = new AtomicLong(0);

    @Resource
    private CommandFactory commandFactory;

    @Resource
    private GameApp gameApp;

    @Resource
    private TrueSkillService trueSkillService;

    private final Map<String, Counter> battleCountersMap = new ConcurrentHashMap<>();

    @Value("${PvpService.logLeaveLobby:false}")
    private boolean logLeaveLobby = false;

    @Value("${PvpService.logLobbyStat:false}")
    private boolean logLobbyStat = false;

    @Value("${PvpService.logBattleLogAlways:true}")
    private boolean logBattleLogAlways = true; // если false - то полный лог ходов сохраняется только в случае ошибок

    // в сутки не должно сниматься больше, чем 35 приведенного мастерства (консервативного рейтинга)
    private int maxLeakTrueSkillRatingInDay = 35;

    public int reconnectTimeoutInSeconds = 15;

    public int maxPauseBattleInMinute = 1;

    @Resource
    private RatingFormula ratingFormula;

    @Autowired(required = false)
    private RankService rankService;

    @Resource
    private BlackListService blackListService;

    @Resource
    private ReactionRateService reactionRateService;

    @Resource
    private ReplayService replayService;

    @Resource
    private PvpDailyRegistry pvpDailyRegistry;

    private final Gson gson = new Gson();

    /**
     * соответствие id игрока и его имени
     */
    private final Map<Long, String> userIdToNameMap = new ConcurrentHashMap<>();

    public synchronized void initCounters() {
        StatCollector statCollector = gameApp.getStatCollector();

        //очищаем счетчики, если были
        for(String counterKey : battleCountersMap.keySet()) {
            Counter counter = battleCountersMap.get(counterKey);
            if(counter != null) {
                statCollector.noNeeded(counter);
            }
        }
        battleCountersMap.clear();

        // регистрируем бои на ставку (или дружеский без оной)
        for(BattleWager battleWager : BattleWager.values()) {
            String battleCounterName = getBattleCounterName(battleWager.battleType, battleWager);
            battleCountersMap.put(battleCounterName, statCollector.needCount("pvp", battleCounterName));
        }
    }

    public static Long getPvpUserId(long profileId, byte socialNetId) {
        return ((long) socialNetId) << 56 | profileId;
    }

    public static long getProfileId(long pvpUserId) {
        return (long) ((int) pvpUserId);
    }

    public static byte getSocialNetId(long pvpUserId) {
        return (byte) (pvpUserId >> 56);
    }

    public static Long getPvpUserId(IntercomResponseI intercomResponse) {
        return ((long) intercomResponse.getSocialNetId()) << 56 | intercomResponse.getProfileId();
    }

    public static String formatPvpUserId(long pvpUserId) {
        return String.format("%s:%s", getSocialNetId(pvpUserId), getProfileId(pvpUserId));
    }

    // доставка команды игроку с возвращением статуса операции
    public boolean sendCommand(BattleParticipant battleParticipant, Object command, boolean force, long battleId) {
        if(battleParticipant == null) {
            log.error("battleParticipant is null! failure send {}", command);
            return false;
        }

        if(!force && !battleParticipant.getState().canAccept()) {
            //игрок уже выбыл
            return false;
        }

        Long pvpUserId = battleParticipant.getPvpUserId();
        PvpUser pvpUser = getUser(pvpUserId);
        if(pvpUser != null && pvpUser.getBattleId() == battleId) {
            Session session = Sessions.get(pvpUser);
            if(session != null) {
                com.pragmatix.gameapp.sessions.Connection conn = session.getConnection(Connection.PVP);
                if(conn != null) {
                    try {
                        conn.send(command, gameApp.getSerializer());
                        return true;
                    } catch (Exception e) {
                        log.error(e.toString(), e);
                    }
                    if(log.isDebugEnabled()) {
                        log.debug("dispatch msg to user [{}] [{}]  ", PvpService.formatPvpUserId(pvpUserId), command);
                    }
                } else {
                    log.warn("Can't send message [{}] cause of battleParticipant for PvpUser [{}] not found", command, PvpService.formatPvpUserId(pvpUserId));
                }
            } else {
                log.warn("Can't send message [{}] cause of session for PvpUser with [{}] not found..", command, PvpService.formatPvpUserId(pvpUserId));
            }
        } else {
            log.warn("Can't send message [{}] cause  PvpUser not found in cache {}", command, PvpService.formatPvpUserId(pvpUserId));
        }
        return false;
    }

    /**
     * Получить пользователя зная id его соц. сети и его Id
     */
    public PvpUser getUser(byte socialNetId, Long profileId) {
        return getUser(getPvpUserId(profileId, socialNetId));
    }

    /**
     * Получить пользователя зная его уникальный id в Pvp
     */
    public PvpUser getUser(Long pvpUserId) {
        return cache.get(PvpUser.class, pvpUserId, false);
    }

    public boolean removeUser(Long pvpUserId) {
        PvpUser user = getUser(pvpUserId);
        if(user != null) {
            user.setBattleId(0);
            user.setCreateBattleRequest(null);
            user.setLastChatMessageTime(0);
        }
        return true;
    }

    public void addUser(PvpUser pvpUser) {
        cache.put(PvpUser.class, pvpUser.getId(), pvpUser);
    }

    /**
     * Разослать команду участникам боя
     *
     * @param pvpUserId    всем кроме id  будет разослана комманда
     * @param battleBuffer battleBuffer
     * @param command      command
     */
    public void dispatchToParticipants(long pvpUserId, final BattleBuffer battleBuffer, final PvpCommandI command) {
        if(log.isTraceEnabled()) {
            log.trace("dispatch msg except [{}] [{}]", formatPvpUserId(pvpUserId), command);
        }
        battleBuffer.visitExcept(pvpUserId, participant -> sendCommand(participant, command, false, battleBuffer.getBattleId()));
    }

    /**
     * Разослать команду тем, кто в данный момент имеет коннект к PVP серверу
     * (безусловнвя отсылка команды, вне зависимости от состояния участника боя)
     */
    public void dispatchSilentToAll(BattleBuffer battleBuffer, PvpCommandI command) {
        for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
            sendToUser(command, battleParticipant.getPvpUserId(), battleBuffer.getBattleId());
        }
    }

    /**
     * Разослать команду всем участникам боя
     *
     * @param battleBuffer battleBuffer
     * @param command      command
     * @param force        если истина рассылать комманду участникам без учёта их текущего состоянии
     */
    public void dispatchToAll(BattleBuffer battleBuffer, Object command, boolean force) {
        if(log.isDebugEnabled()) {
            log.debug("dispatch msg to all [{}]", command);
        }
        for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            sendCommand(battleParticipant, command, force, battleBuffer.getBattleId());
        }
    }

    public void unbindFromBattle(BattleParticipant battleParticipant, BattleBuffer battleBuffer, PvpSystemMessage.TypeEnum cause, BattleParticipant.State endBattleState) {
        unbindFromBattle(battleParticipant, battleBuffer, cause, endBattleState, 0, "");
    }

    public void unbindFromBattle(BattleParticipant battleParticipant, BattleBuffer battleBuffer, PvpSystemMessage.TypeEnum cause, BattleParticipant.State endBattleState, int banType, String banNote) {
        long pvpUserId = battleParticipant.getPvpUserId();
        if(log.isDebugEnabled()) {
            log.debug(String.format("unbind [%s] from battleId=%s by cause [%s]", battleParticipant, battleBuffer.getBattleId(), cause));
        }
        battleParticipant.battleResult = PvpBattleResult.NOT_WINNER;
        battleParticipant.setLeaveBattleTime(System.currentTimeMillis());
        battleParticipant.setState(endBattleState);
        dispatchToParticipants(pvpUserId, battleBuffer, new PvpSystemMessage(cause, battleParticipant.getPlayerNum(), battleBuffer.getBattleId()));

        calculateNewRatingForUnbindedParticipant(battleBuffer, battleParticipant);

        // отсылаем на Main сервер поражение
        EndPvpBattleRequest endBattleRequest = commandFactory.constructEndPvpBattleRequest(battleBuffer, battleParticipant, banType, banNote);
        // не ждем подтверждения на этот запрос
        endBattleRequest.needResponse = cause == PvpSystemMessage.TypeEnum.PlayerSurrendered;
        Messages.toServer(endBattleRequest, battleParticipant.getMainServer(), true);

        PvpUser user = getUser(pvpUserId);
        if(user != null && !endBattleRequest.needResponse) {
            // отсылаем персональную команду окончания боя, если участник в состоянии ещё её получить
            PvpEndBattle endBattle = new PvpEndBattle(battleBuffer, battleParticipant);
            sendToUser(endBattle, user, battleBuffer.getBattleId());
            if(log.isDebugEnabled()) {
                log.debug("battleId={}: закрываем соединение для [{}]", battleBuffer.getBattleId(), user);
            }
            Connections.closeConnectionDeferred(user, Connection.PVP);
        }
    }

    /**
     * разослать сообщение об окончании боя main серверам участников
     */
    public void dispatchToServerEndBattleRequest(final BattleBuffer battle, final boolean isRetry) {
        battle.visitAllInState(BattleParticipant.State.waitEndBattleRequestConfirm, participant -> {
            PvpUser user = getUser(participant.getPvpUserId());
            if(user != null) {
                EndPvpBattleRequest endBattleRequest = commandFactory.constructEndPvpBattleRequest(battle, participant);
                if(isRetry) {
                    log.error("retry for [{}] {}", user, endBattleRequest);
                }
                Messages.toServer(endBattleRequest, user.getMainServer(), true);
            }
        });
    }

    // отправить сообщение пользователю, если он ещё залогинен
    public void sendToUser(PvpCommandI message, long pvpUserId, long battleId) {
        PvpUser user = getUser(pvpUserId);
        sendToUser(message, user, battleId);
    }

    // отправить сообщение пользователю, если он ещё залогинен и принадлежит этому бою
    public void sendToUser(PvpCommandI message, PvpUser user, long battleId) {
        if(user != null && user.getBattleId() == battleId) {
            sendToUser(message, user);
        }
    }

    // отправить сообщение пользователю, если он ещё залогинен
    public void sendToUser(PvpCommandI message, PvpUser user) {
        Session session = Sessions.get(user);
        if(session != null) {
            com.pragmatix.gameapp.sessions.Connection conn = session.getConnection(Connection.PVP);
            if(conn != null) {
                conn.send(message, gameApp.getSerializer());
            }
        }
    }

    private void calculateNewRatingForUnbindedParticipant(BattleBuffer battleBuffer, BattleParticipant unbindedParticipant) {
        // результат боя по командам со времем выхода из боя
        Map<Byte, TeamBattleResult> resultByTeams = new HashMap<>(battleBuffer.getParticipants().size());
        for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            byte team = battleParticipant.getPlayerTeam();
            PvpBattleResult battleResult;
            long leaveBattleTime = battleParticipant.getLeaveBattleTime();
            if(battleParticipant.getState().canTurn()) {
                battleResult = PvpBattleResult.WINNER;
            } else if(battleParticipant.getState() == BattleParticipant.State.draw) {
                battleResult = PvpBattleResult.DRAW_GAME;
            } else if(battleParticipant.getState() == BattleParticipant.State.desync) {
                battleResult = PvpBattleResult.DRAW_DESYNC;
            } else if(battleParticipant.getPlayerTeam() == unbindedParticipant.getPlayerTeam()) {// команде выбывшего игрока для расчета рейтинга засчитываем поражение
                battleResult = PvpBattleResult.NOT_WINNER;
                leaveBattleTime = System.currentTimeMillis();
            } else {
                battleResult = PvpBattleResult.NOT_WINNER;
            }
            if(resultByTeams.containsKey(team)) {
                TeamBattleResult presentTeamBattleResult = resultByTeams.get(team);
                if(presentTeamBattleResult.compareTo(battleResult, leaveBattleTime) < 0) {
                    presentTeamBattleResult.setBattleResult(battleResult);
                    presentTeamBattleResult.setLeaveBattleTime(leaveBattleTime);
                }
                presentTeamBattleResult.addPlayer(battleParticipant);
            } else {
                resultByTeams.put(team, new TeamBattleResult(battleParticipant, battleResult, leaveBattleTime));
            }
        }

        Map<IPlayer, Rating> playerUpdatedRatingMap = trueSkillService.calculateNewRatings(battleBuffer, resultByTeams);
        fillNewTrueSkill(playerUpdatedRatingMap, unbindedParticipant);
    }

    public void calculateEndBattlePoints(BattleBuffer battleBuffer, BattleParticipant battleParticipant) {
        EndPvpBattleResultStructure winResult = new EndPvpBattleResultStructure(
                PvpBattleResult.WINNER,
                ratingFormula.getRatingPoints(battleBuffer, battleParticipant, PvpBattleResult.WINNER),
                rankService != null && rankService.isEnabled() ? rankService.getRankPoints(battleBuffer, battleParticipant, PvpBattleResult.WINNER) : 0
        );
        EndPvpBattleResultStructure defeatResult = new EndPvpBattleResultStructure(
                PvpBattleResult.NOT_WINNER,
                ratingFormula.getRatingPoints(battleBuffer, battleParticipant, PvpBattleResult.NOT_WINNER),
                rankService != null && rankService.isEnabled() ? rankService.getRankPoints(battleBuffer, battleParticipant, PvpBattleResult.NOT_WINNER) : 0
        );
        EndPvpBattleResultStructure drawResult = new EndPvpBattleResultStructure(
                PvpBattleResult.DRAW_GAME,
                ratingFormula.getRatingPoints(battleBuffer, battleParticipant, PvpBattleResult.DRAW_GAME),
                rankService != null && rankService.isEnabled() ? rankService.getRankPoints(battleBuffer, battleParticipant, PvpBattleResult.DRAW_GAME) : 0
        );
        battleParticipant.preCalculatedPoints = Tuple.of(winResult, defeatResult, drawResult);
    }

    public void finishBattle(BattleBuffer battleBuffer) {
        // вручную меняем состояние боя
        battleBuffer.setBattleState(PvpBattleStateEnum.WaitEndBattleConfirm);

        // результат боя по командам со времем выхода из боя
        Map<Byte, TeamBattleResult> resultByTeams = new HashMap<>(battleBuffer.getParticipants().size());
        int realPlayersCount = 0;
        for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            realPlayersCount++;
            if(log.isDebugEnabled()) {
                log.debug("battleId={}: finishing battle: {} ", battleBuffer.getBattleId(), battleParticipant);
            }
            byte team = battleParticipant.getPlayerTeam();
            PvpBattleResult battleResult;
            long leaveBattleTime = battleParticipant.getLeaveBattleTime();
            if(battleParticipant.getState().canTurn()) {
                battleResult = PvpBattleResult.WINNER;
            } else if(battleParticipant.getState() == BattleParticipant.State.draw) {
                battleResult = PvpBattleResult.DRAW_GAME;
            } else if(battleParticipant.getState() == BattleParticipant.State.desync) {
                battleResult = PvpBattleResult.DRAW_DESYNC;
            } else {
                battleResult = PvpBattleResult.NOT_WINNER;
            }
            if(resultByTeams.containsKey(team)) {
                TeamBattleResult presentTeamBattleResult = resultByTeams.get(team);
                // порядок сортировки обратный!
                if(presentTeamBattleResult.compareTo(battleResult, leaveBattleTime) > 0) {
                    presentTeamBattleResult.setBattleResult(battleResult);
                    presentTeamBattleResult.setLeaveBattleTime(leaveBattleTime);
                }
                presentTeamBattleResult.addPlayer(battleParticipant);
            } else {
                resultByTeams.put(team, new TeamBattleResult(battleParticipant, battleResult, leaveBattleTime));
            }
        }

        if(log.isDebugEnabled()) {
            log.debug("battleId={}: resultByTeams {}", battleBuffer.getBattleId(), resultByTeams);
        }

        Map<IPlayer, Rating> playerUpdatedRatingMap = trueSkillService.calculateNewRatings(battleBuffer, resultByTeams);

        //решаем кому выдать победу
        for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            // если на момент окончания боя игрок не имеет коннекта к pvp серверу, ему засчитывается поражение
            // иначе засчитывается результат команды
            if(!battleParticipant.getState().endBattle()) {
                PvpBattleResult battleResult = resultByTeams.get(battleParticipant.getPlayerTeam()).getBattleResult();
                battleParticipant.setState(BattleParticipant.State.waitEndBattleRequestConfirm);
                battleParticipant.battleResult = battleResult;
                fillNewTrueSkill(playerUpdatedRatingMap, battleParticipant);
            }
        }

        battleBuffer.getCurrentTurn().incrementAndGet();
        battleBuffer.setStartTurnTime(0);
        battleBuffer.clearCommandBuffer();
        battleBuffer.clearFutureCommandBuffer();
        battleBuffer.getCurrentCommandNum().set(1);

        battleBuffer.setFinishBattleTime(System.currentTimeMillis());

        dispatchToServerEndBattleRequest(battleBuffer, false);

        if(log.isDebugEnabled()) {
            Map<String, PvpBattleResult> resultMap = battleBuffer.getParticipants().stream().filter(p -> !p.isEnvParticipant())
                    .collect(Collectors.toMap(p -> PvpService.formatPvpUserId(p.getPvpUserId()), p -> p.battleResultStructure().battleResult));
            log.debug("battleId={}: finished {}", battleBuffer.getBattleId(), resultMap);
        }

        onFinishPvpBattle(battleBuffer);
    }

    private void fillNewTrueSkill(Map<IPlayer, Rating> playerUpdatedRatingMap, BattleParticipant battleParticipant) {
        if(playerUpdatedRatingMap != null) {
            // заполняем обновленный TrueSkill рейтинг
            for(Map.Entry<IPlayer, Rating> playerRatingEntry : playerUpdatedRatingMap.entrySet()) {
                PvpPlayer player = (PvpPlayer) playerRatingEntry.getKey();
                if(player.getId().equals(battleParticipant.getPvpUserId())) {

                    int ratingInStartOfDay = pvpDailyRegistry.getTrueSkillRating(player.getId(), battleParticipant.trueSkillMean, battleParticipant.trueSkillStandardDeviation);
                    Rating newRating = playerRatingEntry.getValue();
                    int newTrueSkillRating = TrueSkillService.trueSkillRating(newRating.getMean(), newRating.getStandardDeviation());

                    if(ratingInStartOfDay - newTrueSkillRating < maxLeakTrueSkillRatingInDay) {// боремся со сливами мастерства
                        battleParticipant.setNewTrueSkillMean(newRating.getMean());
                        battleParticipant.setNewTrueSkillStandardDeviation(newRating.getStandardDeviation());
                    }
                    break;
                }
            }
        }
    }

    private void onFinishPvpBattle(BattleBuffer battleBuffer) {
        // фиксируем результат боя для нужд сервиса подбора
        if(battleBuffer.getWager().getValue() > 0) {
            try {
                List<BattleParticipant> participants = battleBuffer.getParticipants();

                for(BattleParticipant participant1 : participants) {
                    for(BattleParticipant participant2 : participants) {
                        if(!participant2.getPvpUserId().equals(participant1.getPvpUserId())) {
                            // не учитываем результат боя между членами одной команды
                            if(participant1.getPlayerTeam() != participant2.getPlayerTeam()) {
                                EndPvpBattleResultStructure participantResult = participant2.battleResultStructure();
                                byte pvpBattleResult = participantResult == null ? (byte) -1 : participantResult.battleResult.byteType();
                                blackListService.registerBattleResult(participant1.getPvpUserId(), participant2.getPvpUserId(), pvpBattleResult);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }

        replayService.onFinishBattle(battleBuffer);
    }

    public Set<Byte> countLiveTeams(BattleBuffer battleBuffer) {
        List<BattleParticipant> participants = battleBuffer.getParticipants();
        Set<Byte> liveTeams = new HashSet<Byte>(participants.size());
        for(BattleParticipant battleParticipant : participants) {
            if(log.isDebugEnabled()) {
                log.debug("battleId={}, countingLiveTeams: {}", battleBuffer.getBattleId(), battleParticipant);
            }
            // считаем команды участников которые еще в бою
            BattleParticipant.State state = battleParticipant.getState();
            if(state.canTurn()) {
                liveTeams.add(battleParticipant.getPlayerTeam());
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("battleId={}, liveTeams: {}", battleBuffer.getBattleId(), liveTeams);
        }
        return liveTeams;
    }

    public Optional<PvpEndBattle> getPvpEndBattleCommand(BattleBuffer battleBuffer, BattleParticipant participant) {
        if(participant == null) {
            return Optional.empty();
        }
        try {
            participant.battleResultStructure();
        } catch (Exception e) {
            log.error("результат боя для игрока [{}] не найден! {}", PvpService.formatPvpUserId(participant.getPvpUserId()), e.toString());
            return Optional.empty();
        }
        return Optional.of(new PvpEndBattle(battleBuffer, participant));
    }

    public long getNextBattleId() {
        return battleCount.incrementAndGet();
    }

    public void addItems(BattleBuffer battle, BackpackItemStructure[] items) {
        List<BackpackItemStructure> itemsList = battle.getInTurn().getItems();
        Collections.addAll(itemsList, items);
    }

    public void dropPlayesFromBattle(BattleBuffer battle, PvpEndTurn endTurn) {
        // удаляям из боя тех кто почил с миров во время хода
        for(byte droppedPlayer : endTurn.droppedPlayers) {
            BattleParticipant droppedParticipant = battle.getParticipantByNum(droppedPlayer);
            if(droppedParticipant != null) {
                if(droppedParticipant.canTurn()) {
                    // в боях с ботом управление отрядами выбывших игроков не предусмотрено
                    BattleParticipant teamTroop = battle.getBattleType().isPveBattle() ? null : getTeamTroop(battle.getParticipants(), droppedParticipant);
                    if(teamTroop != null) {
                        teamTroop.setDroppedFromBattle(true);
                        // запоминаем благодоря какому отряду мы остаемся в бою
                        droppedParticipant.setTroopOnLoan(teamTroop.getPlayerNum());
                    } else {
                        droppedParticipant.setState(droppedFromBattle);
                        droppedParticipant.setLeaveBattleTime(System.currentTimeMillis());
                        droppedParticipant.setDroppedFromBattle(true);
                    }
                } else if(droppedParticipant.endBattle()) {
                    // "вынесли" отряд под внешним управлением
                    if(droppedParticipant.isDroppedFromBattle()) {
                        // "вынесли" повторно, значит поражения отряда было "взято в займы" ранее
                        repayTroop(battle, droppedParticipant);
                    } else {
                        droppedParticipant.setDroppedFromBattle(true);
                    }
                }
            } else {
                log.error("battleId={}, in command: {} connection with profile [{}] not exist!", new Object[]{battle.getBattleId(), endTurn, PvpService.formatPvpUserId(droppedPlayer)});
            }
        }
    }

    private void repayTroop(BattleBuffer battle, BattleParticipant droppedParticipant) {
        for(BattleParticipant battleParticipant : battle.getParticipants()) {
            if(battleParticipant.getPlayerTeam() == droppedParticipant.getPlayerTeam() && battleParticipant.getTroopOnLoan() == droppedParticipant.getPlayerNum()) {
                battleParticipant.setState(droppedFromBattle);
                battleParticipant.setLeaveBattleTime(System.currentTimeMillis());
                battleParticipant.setDroppedFromBattle(true);
                return;
            }
        }
        throw new IllegalStateException("не могу отдать долг: " + battle.getParticipants() + "; " + droppedParticipant);
    }

    /**
     * @return отряд выбывшего члена команды, которым управляет участник боя, если есть
     */
    private BattleParticipant getTeamTroop(List<BattleParticipant> participants, BattleParticipant droppedParticipant) {
        int num = droppedParticipant.getPlayerNum() - 1;
        if(num < 0) {
            num = participants.size() - 1;
        }
        int cycles = 0;
        while (num != droppedParticipant.getPlayerNum()) {
            BattleParticipant participant = participants.get(num);
            if(participant.getPlayerTeam() == droppedParticipant.getPlayerTeam()) {
                if(participant.endBattle() && !participant.isDroppedFromBattle()) {
                    // нашли члена команды, который отвалился от боя (unbind), но отряд его цел
                    return participant;
                } else if(participant.canTurn()) {
                    // дошли до другого участника в бою, отрядов в управлении нет
                    return null;
                }
            }
            num--;
            if(num < 0) {
                num = participants.size() - 1;
            }

            cycles++;
            if(cycles > participants.size()) {
                throw new IllegalStateException("бесконечный цикл: " + participants + "; " + droppedParticipant);
            }
        }
        return null;
    }


    public void addReagents(BattleBuffer battle, byte[] collectedReagents) {
        if(collectedReagents.length < 2) {
            return;
        }
        byte[] reagentsForBattle = battle.getReagentsForBattle();
        if(reagentsForBattle == null) {
            log.error("для боя battleId={} отсутствуют заготовленные реагенты", battle.getBattleId());
            return;
        }
        for(int n = 0; n < collectedReagents.length; n += 2) {
            byte playerNum = collectedReagents[n];
            byte reagentId = collectedReagents[n + 1];
            if(reagentId >= 0) {
                // проверям реагенты посланные и возвращенные
                for(int i = 0; i < reagentsForBattle.length; i++) {
                    byte sentReagentId = reagentsForBattle[i];
                    if(reagentId == sentReagentId) {
                        battle.getReagents().add(playerNum);
                        battle.getReagents().add(reagentId);
                        reagentsForBattle[i] = -1;
                    }
                }
            }
        }
    }

    public void storeDroppedUnits(BattleBuffer battle, byte[] droppedUnits) {
        if(droppedUnits == null || droppedUnits.length < 2) {
            return;
        }
        for(int n = 0; n < droppedUnits.length; n += 2) {
            byte playerNum = droppedUnits[n];
            byte droppedUnit = droppedUnits[n + 1];
            if(droppedUnit > 0) {
                Byte prevValue = battle.droppedUnits.get(playerNum);
                battle.droppedUnits.put(playerNum, prevValue != null ? (byte) (droppedUnit + prevValue) : droppedUnit);
            }
        }
    }

    public int[] getReactionLevel(PvpProfileStructure pvpProfileStructure) {
        return reactionRateService.getReactionLevel(pvpProfileStructure);
    }

    public void logLeaveLobbyStatistic(BattleBuffer battle) {
        if(logLeaveLobby) {
            long battleId = battle.getBattleId();
            int wager = battle.getWager().getValue();
            PvpBattleType battleType = battle.getBattleType();
            if(battle.getParticipants().size() > 0) {
                TeamBattleProposal battleProposal = battle.getParticipants().get(0).getBattleProposal();
                if(battleProposal != null) {
                    ProposalStat stat = battleProposal.getStat();
                    if(stat != null) {
                        StringBuilder sb = new StringBuilder();
                        long lobbyTime = System.currentTimeMillis() - battleProposal.getLobbyTime();
                        String lobbyStat = formatLobbyStat(battleProposal.getGridQuality(), stat, lobbyTime);
                        int groupHp = battleProposal.getGroupHp();
                        int level = battleProposal.getLevel();
                        String reactionRate = "" + battleProposal.getReactionLevel()[0] + ";" + battleProposal.getReactionLevel()[1];
                        byte[] units = battleProposal.getUnits();
                        int rating = battleProposal.getRating();
                        String mass = formatMass(units, groupHp, level/*, battleProposal.getColiseumProgress()*/, battleProposal.getGridQuality());
                        sb.append(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", battleId, "LEAVE_LOBBY", lobbyStat, battleType, wager, rating, mass, reactionRate));
                        for(BattleParticipant participant : battle.getParticipants()) {
                            if(participant.isEnvParticipant()) {
                                continue;
                            }
                            int battles = battleProposal.getBattlesCount();
                            double trueRatingMean = participant.getTrueSkillMean();
                            double trueRatingSpread = participant.getTrueSkillStandardDeviation();
                            sb.append(String.format("\t%s\t(%s)%.2f/%.2f", participant.formatUserId(), battles, trueRatingMean, trueRatingSpread));
                        }

                        pvpCdrlogger.info(sb.toString());
                    }
                }
            }
        }
    }

    private String formatLobbyStat(int gridQuality, ProposalStat stat, long lobbyTime) {
        if(logLobbyStat) {
            return String.format("%s\t%s(%s)%s:%s", formatTime(lobbyTime, true), gridQuality,
                    stat.matchedCandidats,
                    stat.printNotMatchByFor(),
                    stat.printNotMatchByThis());
        } else {
            return String.format("%s\t%s", formatTime(lobbyTime, true), gridQuality);
        }
    }

    private String formatMass(byte[] units, int groupHp, int level, int gridQuality) {
        return level + "/" + Arrays.toString(units).replaceAll(" ", "") + "/" + groupHp;
    }

    public void logEndBattleStatistic(BattleBuffer battle) {
        long battleId = battle.getBattleId();
        int wager = battle.getWager().getValue();
        long startBattleTime = battle.getStartBattleTime();
        long finishBattleTime = battle.getFinishBattleTime();
        String battleTime = formatTime(finishBattleTime - startBattleTime, false);
        PvpBattleType battleType = battle.getBattleType();
        int turnCount = battle.getCurrentTurn().get();

        StringBuilder sb = new StringBuilder();
        Serializable battleKey = wager != 0 ? wager : Arrays.toString(battle.getMissionIds()).replaceFirst(" ", "");
        if(battle.getQuestId() > 0)
            battleKey = "quest_" + battle.getQuestId();
        else if(battle.getWager() == BattleWager.GLADIATOR_DUEL)
            battleKey = "coliseum";
        else if(battle.getWager() == BattleWager.MERCENARIES_DUEL)
            battleKey = "merc";
        sb.append(String.format("%s\t%s\t%s/%s\t%s\t%s", battleId, battleType.getType(), battleKey, battle.getMapId(), battleTime, turnCount));

        for(BattleParticipant participant : battle.getParticipants()) {
            if(participant.isEnvParticipant()) {
                continue;
            }
            String setBattleResultAgo = participant.getLeaveBattleTime() > 0 ? formatTime(finishBattleTime - participant.getLeaveBattleTime(), true) : "0";
            int battleResult = participant.battleResult.getType();
            byte team = participant.getPlayerTeam();
            int lastState = participant.stateBeforeFinate != null ?  participant.stateBeforeFinate.type : participant.getState().type;
            TeamBattleProposal battleProposal = participant.getBattleProposal();
            String lobbyStat = "";
            double trueRatingMean = participant.trueSkillMean;
            double trueRatingSpread = participant.trueSkillStandardDeviation;
            int battles = participant.battlesCount;
            double matchQuality = 0;
            byte[] units = new byte[0];
            int groupHp = 0;
            String reactionView = "0/0";
            int rating = 0;
            int dailyRating = 0;
            int level = 0;
            PvpProfileStructure profileStructure = participant.getPvpProfileStructure();
            if(profileStructure != null) {
                level = profileStructure.getLevel();
                rating = profileStructure.rating;
                dailyRating = profileStructure.dailyRating;
                units = getUnits(profileStructure);
            }
            int gridQuality = 0;
            if(battleProposal != null) {
                ProposalStat stat = battleProposal.getStat();
                gridQuality = battleProposal.getGridQuality();
                if(stat != null) {
                    long lobbyTime = battleProposal.getLobbyTime();
                    lobbyStat = formatLobbyStat(gridQuality, stat, lobbyTime);
                }
                matchQuality = battleProposal.getMatchQuality();
                groupHp = battleProposal.getGroupHp();
                int[] reactionLevel = battleProposal.getReactionLevel();
                if(reactionLevel != null && reactionLevel.length > 1)
                    reactionView = reactionLevel[0] + "/" + reactionLevel[1];
            }
            String mass = formatMass(units, groupHp, level/*, coliseumProgress*/, gridQuality);
            EndPvpBattleResultStructure battleResultStructure = participant.battleResultStructure();
            int ratingPoints = battleResultStructure.ratingPoints;
            int rankPoints = battleResultStructure.rankPoints;
            int profileRankPoints = participant.profileRankPoints;
            sb.append(String.format("\t%s\t%s\t%s\t%s\t%s\t%s\t(%s)%.2f/%.2f\t%.2f\t%s/%s/%s/%s/%s\t%s\t%s",
                    participant.formatUserId(), battleResult, setBattleResultAgo, team, lastState, lobbyStat, battles, trueRatingMean, trueRatingSpread, matchQuality, rating, dailyRating, ratingPoints, profileRankPoints, rankPoints, mass, reactionView));
        }

        sb.append(String.format("\t%s", battle.getBattlePenaltyTime() / (turnCount - 1)));

        pvpCdrlogger.info(sb.toString());
    }

    // улучшенная версия logEndBattleStatistic: содержит ещё лог боя, хранится в более гибком JSON-формате
    public void logEndBattleDetails(BattleBuffer battle) {
        Map<String, Object> statRow = new LinkedHashMap<>();
        // -- battle info --
        statRow.put("battleId", battle.getBattleId());
        long startBattleTime = battle.getStartBattleTime();
        long finishBattleTime = battle.getFinishBattleTime();
        statRow.put("start", AppUtils.formatDate(new Date(startBattleTime)));
        statRow.put("finish", AppUtils.formatDate(new Date(finishBattleTime)));
        if(battle.totalSuspendTime > 0)
            statRow.put("suspendTime", formatTime(battle.totalSuspendTime));
        PvpBattleType battleType = battle.getBattleType();
        statRow.put("battleType", battleType.getType());
        statRow.put("wager_raw", battle.getWager().name());
        int wager = battle.getWager().getValue();
        statRow.put("wager", wager);
        if(wager == 0) {
            statRow.put("missionIds", battle.getMissionIds());
        }
        statRow.put("mapId", battle.getMapId());

        if(battle.getQuestId() > 0)
            statRow.put("special", "quest_" + battle.getQuestId());
        else if(battle.getWager() == BattleWager.GLADIATOR_DUEL)
            statRow.put("special", "coliseum");
        else if(battle.getWager() == BattleWager.MERCENARIES_DUEL)
            statRow.put("special", "merc");

        statRow.put("duration", formatTime(finishBattleTime - startBattleTime, false));
        int turnCount = battle.getCurrentTurn().get() - 1;
        statRow.put("turnCount", turnCount);

        // for each participant:
        statRow.put("participants", battle.getParticipants().stream().filter(p -> !p.isEnvParticipant()).map(participant -> {
            Map<String, Object> p = new LinkedHashMap<>();
            // -- participant's profile info --
            int rating = 0;
            int dailyRating = 0;
            int level = 0;
            List<Map<String, Object>> units = null;
            PvpProfileStructure profileStructure = participant.getPvpProfileStructure();
            if(profileStructure != null) {
                level = profileStructure.getLevel();
                rating = profileStructure.rating;
                dailyRating = profileStructure.dailyRating;
                units = getUnitsDetails(profileStructure);
            }
            Map<String, Object> profile = new LinkedHashMap<>();
            Long pvpUserId = participant.getPvpUserId();
            profile.put("profileId", getProfileId(pvpUserId));
            profile.put("socialNetId", getSocialNetId(pvpUserId));
            profile.put("rating", rating);
            profile.put("dailyRating", dailyRating);
            profile.put("level", level);
            profile.put("rankPoints", participant.profileRankPoints);
            profile.put("units", units);
            p.put("profile", profile);

            // -- participant's this battle info --
            p.put("battleResult", participant.battleResult.getType());
            if(participant.offlineTime > 0)
                p.put("offlineTime", participant.offlineTime);
            p.put("setBattleResultAgo", participant.getLeaveBattleTime() > 0 ? formatTime(finishBattleTime - participant.getLeaveBattleTime(), true) : "0");
            p.put("team", participant.getPlayerTeam());
            p.put("playerNum", participant.getPlayerNum());
            p.put("lastState", participant.stateBeforeFinate != null ? participant.stateBeforeFinate.type : participant.getState().type);
            p.put("battles", participant.battlesCount);
            p.put("trueSkillMean", gsonRound(participant.trueSkillMean, 2));
            p.put("trueSkillSpread", gsonRound(participant.trueSkillStandardDeviation, 2));

            // -- participant's rating result info --
            EndPvpBattleResultStructure battleResultStructure = participant.battleResultStructure();
            Map<String, Object> awarded = new LinkedHashMap<>();
            awarded.put("rating", battleResultStructure.ratingPoints);
            awarded.put("rankPoints", battleResultStructure.rankPoints);
            p.put("awarded", awarded);

            // -- participant's battle proposal info --
            TeamBattleProposal battleProposal = participant.getBattleProposal();
            if(battleProposal != null) {
                ProposalStat stat = battleProposal.getStat();
                if(stat != null) {
                    p.put("lobbyTime", formatTime(battleProposal.getLobbyTime(), true));
                    p.put("gridQuality", battleProposal.getGridQuality());
                    p.put("groupHp", battleProposal.getGroupHp());
                    if(logLobbyStat) {
                        p.put("matchedCandidats", stat.matchedCandidats);
                        p.put("notMatchByFor", stat.printNotMatchByFor());
                        p.put("notMatchByThis", stat.printNotMatchByThis());
                    }
                }
                p.put("matchQuality", gsonRound(battleProposal.getMatchQuality(), 2));
                p.put("reactionLevel", battleProposal.getReactionLevel());
            }

            return p;
        }).toArray());

        statRow.put("penalty", (turnCount > 0) ? (battle.getBattlePenaltyTime() / turnCount) : 0);

        // -- battle log, if was recorded --
        battle.getBattleLog().ifPresent(battleLog -> {
            if(battleLog.isNeedToBeSaved() || logBattleLogAlways) {
                statRow.put("battleLog", battleLog);
            }
        });
        
        statRow.put("chatLog", battle.chatLog);
        
        pvpDetailsCdrLogger.info(gson.toJson(statRow));
    }

    private static BigDecimal gsonRound(double value, int precision) {
        return new BigDecimal(value).setScale(precision, BigDecimal.ROUND_HALF_UP);
    }

    public byte[] getUnits(PvpProfileStructure userProfileStructure) {
        WormStructure[] wormsGroup = userProfileStructure.wormsGroup();
        byte[] units = new byte[wormsGroup.length];
        for(int i = 0; i < wormsGroup.length; i++) {
            units[i] = wormsGroup[i].level;
        }
        return units;
    }

    public List<Map<String, Object>> getUnitsDetails(PvpProfileStructure userProfileStructure) {
        return Arrays.stream(userProfileStructure.wormsGroup()).map(worm -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", worm.ownerId);
            map.put("level", worm.level);
            map.put("armor", worm.armor);
            map.put("attack", worm.attack);
            map.put("exp", worm.experience);
            map.put("race", worm.race);
            map.put("hat", worm.hat);
            map.put("kit", worm.kit);
            map.put("type", worm.teamMemberType.name());
            return map;
        }).collect(Collectors.toList());
    }

    public static String formatTime(long timeInterval) {
        return formatTime(timeInterval, false);
    }

    public static String formatTime(Duration duration) {
        return formatTime(duration.toMillis(), false);
    }

    public static String formatTimeInSeconds(int timeInterval) {
        return formatTime(timeInterval * 1000, false);
    }

    public static String formatTime(long timeInterval, boolean printMillis) {
        int millisecond = (int) (timeInterval % 1000);
        int second = (int) (timeInterval / 1000 % 60);
        int minute = (int) (timeInterval / 1000 / 60 % 60);
        int hour = (int) (timeInterval / 1000 / 60 / 60);
        String hourString;
        String minuteString;
        String secondString;
        String millisecondString;

        if(hour < 10) {
            hourString = "0" + hour;
        } else {
            hourString = Integer.toString(hour);
        }
        if(minute < 10) {
            minuteString = "0" + minute;
        } else {
            minuteString = Integer.toString(minute);
        }
        if(second < 10) {
            secondString = "0" + second;
        } else {
            secondString = Integer.toString(second);
        }
        millisecondString = String.valueOf(millisecond);

        StringBuilder sb = new StringBuilder();
        if(printMillis) {
            if(hour > 0) {
                sb.append(hourString).append(':');
                sb.append(minuteString).append(':');
                sb.append(secondString);
            } else if(minute > 0) {
                sb.append(minuteString).append(':');
                sb.append(secondString);
            } else if(second > 0) {
                sb.append(secondString);
            }

            sb.append('.').append(millisecondString);
        } else {
            sb.append(hourString).append(':');
            sb.append(minuteString).append(':');
            sb.append(secondString);
        }
        return sb.toString();
    }

    public void onStartBattle(BattleBuffer battle) {
        Counter counter = battleCountersMap.get(getBattleCounterName(battle.getBattleType(), battle.getWager()));
        if(counter != null) {
            counter.incriment();
        }
    }

    public void onFinalizeBattle(BattleBuffer battle) {
        if(battle.getStartBattleTime() > 0) {
            Counter counter = battleCountersMap.get(getBattleCounterName(battle.getBattleType(), battle.getWager()));
            if(counter != null) {
                counter.decriment();
            }
        }
    }

    private String getBattleCounterName(PvpBattleType battleType, BattleWager wager) {
        return PvpBattleKey.valueOf(battleType, wager).name();
    }

//====================== Getters and Setters =================================================================================================================================================

    public boolean isLogLeaveLobby() {
        return logLeaveLobby;
    }

    public void setLogLeaveLobby(boolean logLeaveLobby) {
        this.logLeaveLobby = logLeaveLobby;
    }

    public boolean isLogLobbyStat() {
        return logLobbyStat;
    }

    public void setLogLobbyStat(boolean logLobbyStat) {
        this.logLobbyStat = logLobbyStat;
    }

    public Map<Long, String> getUserIdToNameMap() {
        return userIdToNameMap;
    }

    public RankService getRankService() {
        return rankService;
    }

}
