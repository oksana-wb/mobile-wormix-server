package com.pragmatix.pvp.services.battletracking;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.pragmatix.app.common.Connection;
import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.model.MainServers;
import com.pragmatix.app.model.RestrictionItem.BlockFlag;
import com.pragmatix.chat.ChatAction;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.GameApp;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.threads.service.IServiceThread;
import com.pragmatix.intercom.messages.EndPvpBattleRequest;
import com.pragmatix.intercom.messages.PvpServerStopped;
import com.pragmatix.performance.statictics.StatCollector;
import com.pragmatix.performance.statictics.ValueHolder;
import com.pragmatix.pvp.logging.BattleLogEvent;
import com.pragmatix.pvp.logging.BattleLogEventsAppender;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.client.*;
import com.pragmatix.pvp.messages.battle.server.PvpEndBattle;
import com.pragmatix.pvp.messages.battle.server.PvpRetryCommandRequestServer;
import com.pragmatix.pvp.messages.battle.server.PvpStartTurn;
import com.pragmatix.pvp.messages.battle.server.PvpSystemMessage;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.CommandFactory;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.ReplayService;
import com.pragmatix.pvp.services.matchmaking.WagerMatchmakingService;
import com.pragmatix.server.Server;
import com.pragmatix.sessions.AppServerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.pragmatix.pvp.model.BattleParticipant.State.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 28.06.11 17:55
 */
@Component
public class PvpBattleTrackerService implements IServiceThread {

    private static final Logger log = LoggerFactory.getLogger(PvpBattleTrackerService.class);

    private static final Logger DUMP_BATTLES_LOGGER = LoggerFactory.getLogger("DUMP_BATTLES_LOGGER");

    @Resource
    protected PvpService pvpService;

    protected ConcurrentHashMap<Long, BattleBuffer> battles = new ConcurrentHashMap<Long, BattleBuffer>();

    @Resource
    protected GameApp gameApp;

    @Resource
    private CommandFactory commandFactory;

    /**
     * подозрительное количество команд за ход
     */
    private int suspiciousCommandsCountByTurn = 120;

    /**
     * максимальное количество команд за ход
     */
    private int maxCommandsCountByTurn = 150;
    private Level pvpLogbackLoggerLevel;
    private List<Appender<ILoggingEvent>> pvpLogbackLoggerAppenders;

    @Resource
    private BattleLogEventsAppender battleLogEventsAppender;

    @Value("${debug.dumpAllBattles:false}")
    private boolean debugDumpAllBattles = false;

    @Resource
    private WagerMatchmakingService wagerMatchmakingService;

    @Resource
    private ReplayService replayService;

    @Autowired(required = false)
    private MainServers mainServers;

    public void init() {
        PvpBattleTrackerTimeoutTask pvpTurnTrackerTimoutTask = new PvpBattleTrackerTimeoutTask("PvpBattleTrackerTimoutTask", this, pvpService);
        gameApp.getManagedThreads().add(pvpTurnTrackerTimoutTask);
        pvpTurnTrackerTimoutTask.start();

        if(debugDumpAllBattles) {
            enableTrackBattleLogEvents();
        }

        pvpService.initCounters();
        StatCollector statCollector = gameApp.getStatCollector();
        if(mainServers != null) {
            for(final String mainServerAdress : mainServers.map.values()) {
                statCollector.needCollect("pvp#" + mainServerAdress, "in_battle", new InBattleValueHolder(mainServerAdress), false);
            }
        } else {
            statCollector.needCollect("pvp#main", "in_battle", new InBattleValueHolder("main"), false);
        }
    }

    private class InBattleValueHolder implements ValueHolder {

        final String mainServerAdress;

        InBattleValueHolder(String mainServerAdress) {
            this.mainServerAdress = mainServerAdress;
        }

        @Override
        public long getValue() {
            int inBattle = 0;
            for(BattleBuffer battleBuffer : battles.values()) {
                if(!battleBuffer.isFinished()) {
                    for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
                        if(!battleParticipant.isEnvParticipant() && !battleParticipant.isDroppedFromBattle() && battleParticipant.getMainServer().getDestAddress().equals(mainServerAdress)) {
                            inBattle++;
                        }
                    }
                }
            }
            return inBattle;
        }
    }

    public void finishAllBattles() {
        Server.sysLog.info("Останавливаем PvP сервер. Боёв: " + battles.size());
        int liveBattles = 0;
        for(Long battleId : battles.keySet()) {
            BattleBuffer battleBuffer = battles.remove(battleId);
            if(!battleBuffer.isFinished()) {
                if(battleBuffer.isStarted()) {
                    liveBattles++;
                }
                for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
                    // участник всё ещё в бою
                    if(!battleParticipant.endBattle() && !battleParticipant.isEnvParticipant()) {
                        // "прощаем" игроку потраченное одноразовое оружие
                        battleParticipant.getItems().clear();
                        // не изменяем рейтинг
                        battleParticipant.setNewTrueSkillMean(-1);
                        battleParticipant.setNewTrueSkillStandardDeviation(-1);
                        // отсылаем на Main сервер ничью
                        battleParticipant.battleResult = PvpBattleResult.DRAW_SHUTDOWN;

                        EndPvpBattleRequest endPvpBattleRequest = commandFactory.constructEndPvpBattleRequest(battleBuffer, battleParticipant);
                        // не ждем подтверждения на этот запрос
                        endPvpBattleRequest.needResponse = false;
                        Messages.toServer(endPvpBattleRequest, battleParticipant.getMainServer(), true);

                        Long userId = battleParticipant.getPvpUserId();
                        PvpUser user = pvpService.getUser(userId);
                        if(user != null) {
                            // отсылаем персональную команду окончания боя, если участник в состоянии ещё её получить
                            PvpEndBattle endBattle = new PvpEndBattle(battleBuffer, battleParticipant);
                            pvpService.sendToUser(endBattle, user, battleBuffer.getBattleId());
//                            if(log.isDebugEnabled()) {
//                                log.debug("battleId={}: закрываем соединение для [{}]", battleBuffer.getBattleId(), user);
//                            }
//                            Connections.closeConnectionDeferred(user, Connection.PVP);
                        }
                        pvpService.removeUser(userId);
                    }
                }
            }
        }
        Server.sysLog.info("liveBattles was: " + liveBattles);

        if(mainServers != null) {
            for(final String mainServer : mainServers.map.values()) {
                Messages.toServer(new PvpServerStopped(), new AppServerAddress(mainServer), true);
                Server.sysLog.info("send PvpServerStopped to [{}]", mainServer);
            }
        }
    }

    protected void dropBattle(BattleBuffer battleBuffer) {
        battles.remove(battleBuffer.getBattleId());
        // перестраховка, чтобы не подвисали заявки
        wagerMatchmakingService.leaveLobby(battleBuffer);
        replayService.onDropBattle(battleBuffer);
    }

    protected void closeBattle(BattleBuffer battleBuffer) {
        dropBattle(battleBuffer);

        ConcurrentMap<Long, List<BattleLogEvent>> logEventsByBattles = battleLogEventsAppender.getLogEventsByBattles();
        // удаляем из монитора сбора статистики
        List<BattleLogEvent> battleLogEvents = logEventsByBattles.remove(battleBuffer.getBattleId());
        // пишем дамп боя если надо
        if(battleLogEvents != null) {
            boolean needDump = false;
            for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
                if(battleParticipant.getState().isNeedDump()) {
                    needDump = true;
                    break;
                }
            }

            if(needDump || debugDumpAllBattles) {
                StringBuilder sb = new StringBuilder("timeouted battle:\n");
                sb.append("@@@@@ START DUMP [battleId=").append(battleBuffer.getBattleId()).append("] @@@@@\n");
                for(BattleLogEvent battleLogEvent : battleLogEvents) {
                    sb.append('@').append(battleLogEvent).append('\n');
                }
                sb.append("@@@@@ END DUMP [tracked battles: ").append(logEventsByBattles.size()).append("] @@@@@\n");
                DUMP_BATTLES_LOGGER.error(sb.toString());
            }
        }

        for(BattleParticipant enemy : battleBuffer.getParticipants()) {
            Long userId = enemy.getPvpUserId();
            PvpUser pvpUser = pvpService.getUser(userId);
            if(pvpUser != null && pvpUser.getBattleId() == battleBuffer.getBattleId()) {
                Session session = Sessions.get(pvpUser);
                if(session != null) {
                    com.pragmatix.gameapp.sessions.Connection connection = session.getConnection(Connection.PVP);
                    if(connection != null) {
                        connection.close();
                    }
                }

                pvpService.removeUser(userId);
            }
        }

        pvpService.onFinalizeBattle(battleBuffer);
    }

    public void disconnectFromBattle(PvpUser user) {
        if(log.isDebugEnabled()) {
            log.debug("disconnected");
        }
        BattleBuffer battleBuffer = battles.get(user.getBattleId());
        if(battleBuffer != null) {
            /**
             * {@link com.pragmatix.pvp.services.battletracking.handlers.DisconnectHandler}
             */
            battleBuffer.handleEvent(user, PvpBattleActionEnum.Disconnect);
        } else {
            if(log.isDebugEnabled()) {
                log.debug("battleId={}: battle not exist", user.getBattleId());
            }
        }
    }

    public void unbindFromPausedBattle(PvpUser user) {
        BattleBuffer battleBuffer = battles.get(user.getBattleId());
        if(battleBuffer != null && battleBuffer.paused) {
            BattleParticipant battleParticipant = battleBuffer.getParticipant(user.getId());
            if(battleParticipant != null) {
                battleParticipant.offlineTime += System.currentTimeMillis() - battleParticipant.getDisconnectTime();
                battleParticipant.setDisconnectTime(0);

                if(battleParticipant.getState().canTurn()) {
                    /**
                     * {@link com.pragmatix.pvp.services.battletracking.handlers.UnbindHandler}
                     */
                    battleBuffer.handleEvent(user, PvpBattleActionEnum.Unbind);
                }
                battleBuffer.resumeBattle(user.getId());
            }
        }
    }

    public BattleBuffer tryReConnectToBattle(PvpUser user, long battleId, short turnNum) {
        if(battleId == 0)
            return null;
        BattleBuffer battleBuffer = battles.get(battleId);
        BattleParticipant battleParticipant;
        if(battleBuffer != null && user.getBattleId() == battleId) {
            battleParticipant = battleBuffer.getParticipant(user.getId());
            if(battleParticipant != null) {
                if(battleBuffer.getCurrentTurn().get() == turnNum || /*Сервер уже переключил номер хода, но клиент не получил StartTurn*/ battleBuffer.getCurrentTurn().get() == turnNum + 1) {
                    if(battleParticipant.getState() != waitReconnect) {
                        battleParticipant.stateBeforeDisconnect = battleParticipant.getState();
                        battleParticipant.setState(waitReconnect);
                    }
                    battleParticipant.offlineTime += System.currentTimeMillis() + battleParticipant.getDisconnectTime();
                    battleParticipant.setDisconnectTime(0);

                    battleBuffer.resumeBattle(user.getId());

                    return battleBuffer;
                } else {
                    log.error("failure reconnect to battle! clientTurn:{} != currentTurn:{}, participantState:{}",turnNum, battleBuffer.getCurrentTurn().get(), battleParticipant.getState());
                }
            } else {
                log.error("failure reconnect to battle! battleParticipant not found in battle");
            }
        } else {
            if(user.disconnectTime > 0) {
                long offlineTime = (System.currentTimeMillis() - user.disconnectTime);
                log.error("failure reconnect to battle! offlineTime:{}, user.battleId:{}", PvpService.formatTime(offlineTime), user.getBattleId());
            } else {
                log.error("failure reconnect to battle! disconnectTime is empty");
            }
        }
        return null;
    }

    public void reconnectToBattle(PvpUser user, long battleId, short turnNum, short lastCommandNum) {
        BattleBuffer battleBuffer = battles.get(battleId);
        if(battleBuffer == null) return;
        if(battleBuffer.getCurrentTurn().get() == turnNum + 1) {
            // Сервер уже переключил номер хода, но клиент не получил StartTurn
            turnNum = (short) (turnNum + 1);
            lastCommandNum = 0;
        }
        BattleParticipant battleParticipant = battleBuffer.getParticipant(user.getId());

        if(battleBuffer.getTurningPvpId() == user.getId()) {
            battleParticipant.setState(battleParticipant.stateBeforeDisconnect);

            // реконнект игрока того чей сейчас ход
            // он утверждает что отправил больше команд чем пришло на сервер
            if(lastCommandNum > battleBuffer.getCurrentCommandNum().get()) {
                // просим его повторить команды начиная с battleBuffer.сurrentCommandNum + 1
                PvpRetryCommandRequestServer message = new PvpRetryCommandRequestServer(battleId, (short) battleBuffer.getCurrentTurn().get(), (short) (battleBuffer.getCurrentCommandNum().get() + 1));
                Messages.toUser(message);
            } else if(lastCommandNum < battleBuffer.getCurrentCommandNum().get()) {
                if(lastCommandNum == 0 && battleBuffer.getCurrentTurn().get() > 1
                        && battleBuffer.getCommandBuffer().size() == 1 && battleBuffer.getCommandBuffer().get(0) instanceof PvpStartTurn) {
                    // всё нормально, игрок не получил StartTurn
                    Messages.toUser(battleBuffer.getCommandBuffer().get(0));
                } else {
                    // сервер обработал больше комманд, чем в момент реконнекта сообщает ему клиент
                    // это ссука читер
                    pvpService.unbindFromBattle(battleBuffer.getInTurn(), battleBuffer, PvpSystemMessage.TypeEnum.PlayerCheater, cheat);
                    battleBuffer.handleAction(PvpBattleActionEnum.StateTimeout);
                }
            }
        } else {
            battleParticipant.setState(battleParticipant.stateBeforeDisconnect);

            for(CountedCommandI cmd : battleBuffer.getCommandBuffer()) {
                if(cmd.getCommandNum() > lastCommandNum) {
                    pvpService.sendToUser(cmd, user, battleBuffer.getBattleId());
                }
            }
        }

        // могли веть и в читеры записать
        if(battleParticipant.getState().canTurn()) {
            pvpService.dispatchToParticipants(user.getId(), battleBuffer, new PvpSystemMessage(PvpSystemMessage.TypeEnum.PlayerReconnected, battleParticipant.getPlayerNum(), user.getBattleId()));
        }
    }

    public void retryCommandsByRequest(PvpUser user, PvpRetryCommandRequestClient command) {
        BattleBuffer battleBuffer = battles.get(command.getBattleId());
        if(battleBuffer != null) {
            if(command.turnNum == battleBuffer.getCurrentTurn().get() && battleBuffer.getTurningPvpId() != user.getId()) {
                int i = 1;
                for(CountedCommandI cmd : battleBuffer.getCommandBuffer()) {
                    if(i >= command.fromCommandNum) {
                        if(cmd instanceof PvpEndBattle) {
                            // передоставляем команду  PvpEndBattle только если окончание боя было подтверждено main сервером игрока
                            // тот на чьём ходу бой завершился запросить передоставку пока не может
                            if(battleBuffer.getParticipant(user.getId()).getState() == finate) {
                                Messages.toUser(cmd);
                            }
                        } else {
                            Messages.toUser(cmd);
                        }
                    }
                    i++;
                }
            } else {
                // игрок читер
                log.error("Invalid command {} in {}", command, battleBuffer.dumpBattle());
            }
        } else {
            // бой не найден, рвем коннект
            PvpSystemMessage pvpSystemMessage = new PvpSystemMessage(PvpSystemMessage.TypeEnum.BattleNotExist, -1, command.getBattleId());
            Messages.toUser(pvpSystemMessage);
            Connections.closeConnectionDeferred();
        }
    }

    public void dispatchChatMessage(final PvpChatMessage chatMessage, PvpUser profile) {
        final BattleBuffer battleBuffer = battles.get(chatMessage.getBattleId());
        if(battleBuffer != null) {
            BattleParticipant participant = battleBuffer.getParticipant(profile.getId());
            if(participant != null) {
                short blocks = participant.getRestrictionBlocks();
                if(chatMessage.teamsMessage) {
                    final byte playerTeam = participant.getPlayerTeam();
                    if(!BlockFlag.TEAM_CHAT.isAppliedTo(blocks) || !chatMessage.action.canBeRestricted) {
                        battleBuffer.visitExcept(profile.getId(), participant_ -> {
                            if(participant_.getPlayerTeam() == playerTeam) {
                                pvpService.sendCommand(participant_, chatMessage, false, battleBuffer.getBattleId());
                            }
                        });
                        if (chatMessage.action == ChatAction.PostToChat) {
                            battleBuffer.chatLog.add(new BattleBuffer.ChatMessage(AppUtils.currentTimeSeconds(), (int) profile.getProfileId(), chatMessage.message, chatMessage.teamsMessage));
                        }
                    }
                } else {
                    if(!BlockFlag.CHAT.isAppliedTo(blocks) || !chatMessage.action.canBeRestricted) {
                        pvpService.dispatchToParticipants(profile.getId(), battleBuffer, chatMessage);

                        if (chatMessage.action == ChatAction.PostToChat) {
                            battleBuffer.chatLog.add(new BattleBuffer.ChatMessage(AppUtils.currentTimeSeconds(), (int) profile.getProfileId(), chatMessage.message, chatMessage.teamsMessage));
                        }
                    }
                }
            }
        }
    }

    public boolean checkCountedCommand(CountedCommandI command, PvpUser profile) {
        BattleBuffer battleBuffer = battles.get(command.getBattleId());
        if(battleBuffer != null) {
            if(command.getTurnNum() < battleBuffer.getCurrentTurn().get()) {
                log.warn("expected greater turn num [{}] in [{}]", battleBuffer.getCurrentTurn().get(), command);
                return false;
            }
            if(profile.getId() != battleBuffer.getTurningPvpId()) {
                log.warn("battleId={}: Command from invalid user: [{}] in turning [{}]", new Object[]{battleBuffer.getBattleId(), profile, PvpService.formatPvpUserId(battleBuffer.getTurningPvpId())});
                return false;
            }
            if(command.getCommandNum() > battleBuffer.getCurrentCommandNum().get() + 1) {
                log.warn("expected lower command num [{}] in [{}]", battleBuffer.getCurrentCommandNum().get() + 1, command);

                battleBuffer.addFutureCommand(command);

                // первое же некорректное сообщение полученное в состоянии ReadyToDispatch генерирует запрос на передоставку комманды и переводит бой в состояние WaitForReplayCommand
                if(battleBuffer.getBattleState() == PvpBattleStateEnum.ReadyToDispatch) {
                    if(log.isDebugEnabled()) {
                        log.debug("battleId={}: Invalid command num in ReadyToDispatch state, send IdleTimeout action...", battleBuffer.getBattleId());
                    }
                    PvpSystemMessage pvpSystemMessage = new PvpSystemMessage(PvpSystemMessage.TypeEnum.PlayerSkeepCommand, battleBuffer.getInTurn().getPlayerNum(), battleBuffer.getBattleId());
                    pvpService.dispatchToAll(battleBuffer, pvpSystemMessage, false);
                    /**
                     * {@link com.pragmatix.pvp.services.battletracking.handlers.RequestRetryCommandHandler}
                     */
                    battleBuffer.handleAction(PvpBattleActionEnum.IdleTimeout);
                }
                return false;
            } else if(command.getCommandNum() <= battleBuffer.getCurrentCommandNum().get()) {
                // игнорируем пришедшые команды с меньшим или текущим CurrentCommandNum
                if(log.isDebugEnabled()) {
                    log.debug("skip repeating command num in [{}], {}", command, battleBuffer.dumpBattle());
                }
                PvpSystemMessage pvpSystemMessage = new PvpSystemMessage(PvpSystemMessage.TypeEnum.PlayerRepeatYourself, battleBuffer.getInTurn().getPlayerNum(), battleBuffer.getBattleId());
                pvpService.dispatchToAll(battleBuffer, pvpSystemMessage, false);
                return false;
            } else if(command.getCommandNum() == suspiciousCommandsCountByTurn) {
                // подозрительно много команд за ход - рассылаем в чат предупреждение
                PvpSystemMessage pvpSystemMessage = new PvpSystemMessage(PvpSystemMessage.TypeEnum.PlayerLongTimeInTurn, battleBuffer.getInTurn().getPlayerNum(), battleBuffer.getBattleId());
                pvpService.dispatchToAll(battleBuffer, pvpSystemMessage, false);
                return true;
            } else if(command.getCommandNum() == maxCommandsCountByTurn) {
                // достигнут предел количества команд за ход
                log.error("battleId={}: commands per turn limit is reached: {}", command.getBattleId(), command);
                battleBuffer.handleAction(PvpBattleActionEnum.StateTimeout);
                return false;
            } else if(command.getCommandNum() > maxCommandsCountByTurn) {
                // превышен порог количества команд за ход
                if(log.isDebugEnabled()) {
                    log.debug("Exceeded the threshold of the number of commands per turn: {}, {}", command, battleBuffer.dumpBattle());
                }
                return false;
            }
        }
        // обработка на не верный battleId будет выполнена позже
        return true;
    }

    public void handleCommand(PvpUser user, PvpCommandI command) {
        handleCommand(user, command, false);
    }

    /**
     * @param forceAccept пропускать команду от игрока который уже dropped
     */
    public void handleCommand(PvpUser user, PvpCommandI command, boolean forceAccept) {
        long start = System.currentTimeMillis();

        BattleBuffer battleBuffer = battles.get(command.getBattleId());
        if(battleBuffer != null) {
            BattleParticipant battleParticipant = battleBuffer.getParticipant(user.getId());
            if(battleParticipant != null) {
                BattleParticipant.State state = battleParticipant.getState();
                // пропускаем команды только от игроков которые ещё в игре
                if(state.canTurn() || (state.canAccept() && forceAccept)) {
                    battleBuffer.handleEvent(user, command);
                } else {
                    if(!(command instanceof PvpDropPlayer) && !(command instanceof PvpEndTurnResponse)) {
                        log.error("battleId={}: Dropped player [{}] send command: {}", new Object[]{battleBuffer.getBattleId(), PvpService.formatPvpUserId(battleParticipant.getPvpUserId()), command});
                    }
                }
            } else {
                PvpSystemMessage pvpSystemMessage = new PvpSystemMessage(PvpSystemMessage.TypeEnum.PlayerNotFoundInBattle, -1, command.getBattleId());
                Messages.toUser(pvpSystemMessage);
                Connections.closeConnectionDeferred();
            }
        } else {
            PvpSystemMessage pvpSystemMessage = new PvpSystemMessage(PvpSystemMessage.TypeEnum.BattleNotExist, -1, command.getBattleId());
            Messages.toUser(pvpSystemMessage);
            Connections.closeConnectionDeferred();
        }

        if(log.isTraceEnabled()) {
            log.trace("## battleId={}: handle command in {} msec.", command.getBattleId(), System.currentTimeMillis() - start);
        }
    }

    public void unbindCheaterFromBattle(PvpUser profile, long battleId) {
        BattleBuffer battleBuffer = getBattle(battleId);
        if(battleBuffer != null) {
            BattleParticipant battleParticipant = battleBuffer.getParticipant(profile.getId());
            if(battleParticipant != null) {
                battleBuffer.handleEvent(profile, new PvpDropPlayer(battleParticipant.getPlayerNum(), battleId, DropReasonEnum.I_AM_CHEATER));
            }
        }
    }

    public BattleBuffer getBattle(long battleId) {
        return battles.get(battleId);
    }

    public BattleParticipant getBattleParticipant(long battleId, long userId) {
        BattleBuffer battleBuffer = battles.get(battleId);
        if(battleBuffer != null) {
            return battleBuffer.getParticipant(userId);
        } else {
            return null;
        }
    }

    /**
     * @param battleBuffer новый бой
     * @return null если боя с таким id не было, иначе бой добавлен не будет и вернется тот бой что присутствует в мапе
     */
    public BattleBuffer addBattleIfAbsent(BattleBuffer battleBuffer) {
        return battles.putIfAbsent(battleBuffer.getBattleId(), battleBuffer);
    }

    public void enableTrackBattleLogEvents() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger pvpLogbackLogger = loggerContext.getLogger("com.pragmatix.pvp");
        // сохряняем текущий уровень логгера "com.pragmatix.pvp"
        pvpLogbackLoggerLevel = pvpLogbackLogger.getLevel();
        // сохряняем текущие аппендеры логгера "com.pragmatix.pvp"
        Iterator<Appender<ILoggingEvent>> appenderIterator = pvpLogbackLogger.iteratorForAppenders();
        pvpLogbackLoggerAppenders = new ArrayList<>();
        while (appenderIterator.hasNext()) {
            Appender<ILoggingEvent> eventAppender = appenderIterator.next();
            pvpLogbackLogger.detachAppender(eventAppender);
            pvpLogbackLoggerAppenders.add(eventAppender);
        }

        // конфигурием аппендер сбора логов по боям
        battleLogEventsAppender.setContext(loggerContext);
        battleLogEventsAppender.setPvpLogbackLoggerAppenders(pvpLogbackLoggerAppenders);
        battleLogEventsAppender.setPvpLogbackLoggerLevel(pvpLogbackLoggerLevel);
        battleLogEventsAppender.start();

        // добавляем наш аппендер
        pvpLogbackLogger.setLevel(Level.DEBUG);
        pvpLogbackLogger.addAppender(battleLogEventsAppender);
    }

    public void disableTrackBattleLogEvents() {
        if(pvpLogbackLoggerLevel != null && pvpLogbackLoggerAppenders != null) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger pvpLogbackLogger = loggerContext.getLogger("com.pragmatix.pvp");
            // возвращаем как было
            pvpLogbackLogger.detachAppender(battleLogEventsAppender);
            battleLogEventsAppender.stop();

            pvpLogbackLogger.setLevel(pvpLogbackLoggerLevel);
            for(Appender<ILoggingEvent> appender : pvpLogbackLoggerAppenders) {
                pvpLogbackLogger.addAppender(appender);
            }
        } else {
            log.error("failure disableTrackBattleLogEvents: pvpLogbackLoggerLevel={}; pvpLogbackLoggerAppenders={}", pvpLogbackLoggerLevel, pvpLogbackLoggerAppenders);
        }
    }

    //====================== Getters and Setters =================================================================================================================================================

    public void setSuspiciousCommandsCountByTurn(int suspiciousCommandsCountByTurn) {
        this.suspiciousCommandsCountByTurn = suspiciousCommandsCountByTurn;
    }

    public void setMaxCommandsCountByTurn(int maxCommandsCountByTurn) {
        this.maxCommandsCountByTurn = maxCommandsCountByTurn;
    }

    public ConcurrentHashMap<Long, BattleBuffer> getBattles() {
        return battles;
    }

}

