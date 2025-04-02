package com.pragmatix.pvp.model;

import com.pragmatix.app.settings.AppParams;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.task.TaskLock;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.messages.battle.client.CountedCommandI;
import com.pragmatix.pvp.messages.handshake.client.CreateBattleRequest;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.battletracking.BattleStateTrackerI;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import com.pragmatix.pvp.services.battletracking.PvpBattleStateEnum;
import com.pragmatix.pvp.services.matchmaking.BattleProposal;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Time;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 24.06.11 18:03
 */
public class BattleBuffer {

    private static final int INIT_PARTICIPANS = 4;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final long battleId;

    private final PvpBattleType battleType;

    private final int mapId;

    private final BattleStateTrackerI battleStateTracker;

    private AtomicInteger currentTurn = new AtomicInteger(1);

    private AtomicInteger currentCommandNum = new AtomicInteger(0);

    private long turningPvpId;

    private final List<BattleParticipant> participants = new ArrayList<>(INIT_PARTICIPANS);

    /**
     * последний кто ходил в команде. Индекс в масиве соответствует инлексу команды
     */
    private byte[] turningNumByTeam;

    private List<CountedCommandI> commandBuffer = new CopyOnWriteArrayList<>();

    private ConcurrentNavigableMap<Short, CountedCommandI> futureCommandBuffer = new ConcurrentSkipListMap<Short, CountedCommandI>();

    private volatile PvpBattleStateEnum battleState;

    /**
     * время обработки последней action
     */
    private long lastActivityTime = System.currentTimeMillis();

    /**
     * время прихода последней PvpActionEx
     */
    private long lastPvpActionExTime;

    /**
     * суммарное время опоздания PvpActionEx в течении хода
     */
    private int turnPenaltyTime;

    /**
     * суммарное время опоздания PvpActionEx в течении всего боя
     */
    private int battlePenaltyTime;

    /**
     * время последнего изменения состояния
     */
    private long lastChangeStateTime = System.currentTimeMillis();

    /**
     * время начала хода
     */
    private long startTurnTime = 0;

    private boolean sendLongTimeInTurningWarning = false;

    private PvpUser creator;

    protected BattleWager wager;

    private short[] missionIds;

    private short questId;

    public byte[] reagentsForBattle;

    /**
     * пары собранныых реагентов - playerNun:reagentId
     */
    private final List<Byte> reagents = new CopyOnWriteArrayList<>();

    public final Map<Byte, Byte> droppedUnits = new ConcurrentHashMap<>();

    public short[] participantsHealthInPercent;

    private BattleProposal battleProposal;

    private long startBattleTime;

    private long finishBattleTime;

    private PvpBattleStateEnum wrappedState;

    private TaskLock lock;

    private Runnable onStartBattle;

    public int heroicBossLevel = -1;

    final public List<ChatMessage> chatLog = new ArrayList<>(10);
    
    private PvpBattleLog pvpBattleLog;

    // Лог боя для целей реплея
    public ByteBuf commandBuf;
    // Код боя для целей реплея
    public String commandBufKey;

    // время когда все активные участники боя одновременно теряют соединение с сервером
    public long pauseTime = 0;

    public volatile boolean paused = false;

    public int totalSuspendTime = 0;

    public static class ChatMessage {
        final public String date;
        final int profileId;
        final String message;
        final boolean teamsMessage;

        public ChatMessage(int date, int profileId, String message, boolean teamsMessage) {
            this.date = AppUtils.formatDateInSeconds(date);
            this.profileId = profileId;
            this.message = message;
            this.teamsMessage = teamsMessage;
        }
    }
    
    public void pauseBattle() {
        paused = true;
        pauseTime = System.currentTimeMillis();
        log.info("battleId={}: pause battle. all live participants is offline", battleId);
    }

    public void resumeBattle(long resumedBy) {
        if(paused) {
            if(resumedBy != 0) {
                visitExcept(resumedBy, participant -> {
                    // переставляем время диконнекта у остальных участников боя, иначе у них немедленно сработает таймаут на переподключение
                    participant.offlineTime += System.currentTimeMillis() + participant.getDisconnectTime();
                    participant.setDisconnectTime(System.currentTimeMillis());
                });
            }
            long suspendTime = System.currentTimeMillis() - pauseTime;

            lastActivityTime += suspendTime;
            lastChangeStateTime += suspendTime;
            lastPvpActionExTime += suspendTime;
            if(startTurnTime > 0)
                startTurnTime += suspendTime;

            totalSuspendTime += suspendTime;

            if(log.isInfoEnabled()) {
                log.info("battleId={}: resume battle. suspend time is {}, total suspend time is {}", battleId, PvpService.formatTime(suspendTime), PvpService.formatTime(totalSuspendTime));
                log.info("\n" +
                        "lastActivityTime=" + AppUtils.formatDate(new Date(lastActivityTime)) + "\n" +
                        "lastChangeStateTime=" + AppUtils.formatDate(new Date(lastChangeStateTime)) + "\n" +
                        "lastPvpActionExTime=" + AppUtils.formatDate(new Date(lastPvpActionExTime)) + "\n" +
                        "startTurnTime=" + AppUtils.formatDate(new Date(startTurnTime))
                );
            }
            pauseTime = 0;
            paused = false;
        }
    }

    public String dumpBattle() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BattleBuffer{");
        sb.append("battleId=").append(battleId);
        sb.append(", turn=").append(currentTurn);
        sb.append(", commandNum=").append(currentCommandNum);
        sb.append(", inTurn=").append(PvpService.formatPvpUserId(turningPvpId));
        sb.append(", state=").append(battleState);
        if(commandBuffer.size() > 0) {
            sb.append(", commandBuffer.size=").append(commandBuffer.size());
        }
        if(futureCommandBuffer.size() > 0) {
            sb.append(", futureCommandBuffer=").append(futureCommandBuffer.keySet());
        }
        sb.append(", activity=").append(lastActivityTime > 0 ? new Time(lastActivityTime) : "");
        sb.append(", changeState=").append(lastChangeStateTime > 0 ? new Time(lastChangeStateTime) : "");
        if(startTurnTime > 0) {
            sb.append(", startTurn=").append(new Time(startTurnTime));
        }
        sb.append(", participants=").append(participants);
        sb.append('}');
        return sb.toString();
    }

    public String dumpBattleInitInfo() {
        return "BattleBuffer{" +
                "battleId=" + battleId +
                ", wager=" + wager +
                ", mapId=" + mapId +
                ", missionIds=" + Arrays.toString(missionIds) +
                ", participants=" + participants +
                '}';
    }

    public BattleBuffer(long battleId, PvpBattleType battleType, int mapId, BattleStateTrackerI battleStateTracker) {
        this.battleId = battleId;
        this.battleType = battleType;
        this.mapId = mapId;
        this.battleStateTracker = battleStateTracker;
        this.battleState = battleStateTracker.getInitState();
    }

    public void init() {
    }


    public void handleEvent(PvpUser user, Object event) {
        battleStateTracker.handleEvent(user, event, this);
    }

    public void handleAction(PvpBattleActionEnum action) {
        battleStateTracker.handleAction(action, this);
    }

    public void visitExcept(long userId, Consumer<BattleParticipant> consumer) {
        for(BattleParticipant battleParticipant : participants) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            if(battleParticipant.getPvpUserId() != userId) {
                consumer.accept(battleParticipant);
            }
        }
    }

    public void visitExceptTurning(Consumer<BattleParticipant> consumer) {
        for(BattleParticipant battleParticipant : participants) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            if(battleParticipant.getPvpUserId() != turningPvpId) {
                consumer.accept(battleParticipant);
            }
        }
    }

    public void visitExceptTurningAndInState(BattleParticipant.State expectedState, Consumer<BattleParticipant> consumer) {
        for(BattleParticipant battleParticipant : participants) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            if(battleParticipant.getPvpUserId() != turningPvpId && battleParticipant.inState(expectedState)) {
                consumer.accept(battleParticipant);
            }
        }
    }

    public void visitExceptTurningAndInStates(BattleParticipant.State expectedState1, BattleParticipant.State expectedState2, Consumer<BattleParticipant> consumer) {
        for(BattleParticipant battleParticipant : participants) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            if(battleParticipant.getPvpUserId() != turningPvpId && (battleParticipant.inState(expectedState1) || battleParticipant.inState(expectedState2))) {
                consumer.accept(battleParticipant);
            }
        }
    }

    public void visitAllInState(BattleParticipant.State expectedState, Consumer<BattleParticipant> consumer) {
        for(BattleParticipant battleParticipant : participants) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            if(battleParticipant.inState(expectedState)) {
                consumer.accept(battleParticipant);
            }
        }
    }

    public List<BattleParticipant> getParticipants() {
        return participants;
    }

    public BattleParticipant getInTurn() {
        return getParticipant(turningPvpId);
    }

    public CountedCommandI getLastBufferedCommand() {
        if(commandBuffer.size() > 0) {
            return commandBuffer.get(commandBuffer.size() - 1);
        } else {
            return null;
        }
    }

    public BattleParticipant getParticipant(long pvpUserId) {
        for(BattleParticipant participant : participants) {
            if(participant.getPvpUserId() == pvpUserId) {
                return participant;
            }
        }
        return null;
    }

    public BattleParticipant getParticipantByNum(int playerNum) {
        try {
            return participants.get(playerNum);
        } catch (IndexOutOfBoundsException e) {
            log.error("battleId={}, на найден участник по playerNum {}", getBattleId(), playerNum);
            return null;
        }
    }

    public void setParticipantState(long pvpUserId, BattleParticipant.State newState) {
        BattleParticipant battleParticipant = getParticipant(pvpUserId);
        if(battleParticipant != null) {
            battleParticipant.setState(newState);
        } else {
            log.warn("failere set state [{}] for [{}], battleParticipant not found", newState, PvpService.formatPvpUserId(pvpUserId));
        }
    }

    public BattleParticipant getParticipant(byte socialNetId, long profileId) {
        return getParticipant(PvpService.getPvpUserId(profileId, socialNetId));
    }

    public void addParticipant(BattleParticipant participant) {
        participants.add(participant);
    }

    public void addFutureCommand(CountedCommandI command) {
        futureCommandBuffer.putIfAbsent(command.getCommandNum(), command);
    }

    public boolean allInState(BattleParticipant.State expectedState) {
        for(BattleParticipant battleParticipant : participants) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            if(battleParticipant.getState() != expectedState) {
                return false;
            }
        }
        return true;
    }

    public boolean allInStateOrEndBattle(BattleParticipant.State expectedState) {
        for(BattleParticipant battleParticipant : participants) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            if(battleParticipant.getState() != expectedState && !battleParticipant.getState().endBattle()) {
                return false;
            }
        }
        return true;
    }

    public boolean allInStates(BattleParticipant.State... expectedStates) {
        Set<BattleParticipant.State> expectedStatesSet = new HashSet<BattleParticipant.State>(expectedStates.length);
        Collections.addAll(expectedStatesSet, expectedStates);
        for(BattleParticipant battleParticipant : participants) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            if(!expectedStatesSet.contains(battleParticipant.getState())) {
                return false;
            }
        }
        return true;
    }

    public boolean hasInState(BattleParticipant.State expectedState) {
        for(BattleParticipant battleParticipant : participants) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            if(battleParticipant.getState() == expectedState) {
                return true;
            }
        }
        return false;
    }

    public boolean hasInStates(BattleParticipant.State expectedState1, BattleParticipant.State expectedState2) {
        for(BattleParticipant battleParticipant : participants) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            if(battleParticipant.getState() == expectedState1 || battleParticipant.getState() == expectedState2) {
                return true;
            }
        }
        return false;
    }

    public boolean hasInStates(BattleParticipant.State expectedState1, BattleParticipant.State expectedState2, BattleParticipant.State expectedState3) {
        for(BattleParticipant battleParticipant : participants) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            if(battleParticipant.getState() == expectedState1 || battleParticipant.getState() == expectedState2 || battleParticipant.getState() == expectedState3) {
                return true;
            }
        }
        return false;
    }

    public void setParticipantState(long pvpUserId, BattleParticipant.State state, BattleParticipant.State expectedState) {
        BattleParticipant participant = getParticipant(pvpUserId);
        if(participant != null) {
            BattleParticipant.State prevState = participant.getState();
            participant.setState(state);
            if(prevState != expectedState) {
                log.warn("предыдущее {} состояние не равно ожидаемому {}", prevState, expectedState);
            }
        } else {
            log.warn("не удалось выставить состояние {}, участник боя не найден", state);
        }
    }

    public boolean isFinished() {
        return inState(PvpBattleStateEnum.EndBattle) || inState(PvpBattleStateEnum.DropBattle);
    }

    public boolean isStarted() {
        return turningPvpId > 0;
    }

    public CreateBattleRequest getCreateBattleRequest() {
        if(creator != null) {
            return creator.getCreateBattleRequest();
        } else {
            return null;
        }
    }

    //====================== Getters and Setters =================================================================================================================================================

    public AtomicInteger getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(AtomicInteger currentTurn) {
        this.currentTurn = currentTurn;
    }

    public AtomicInteger getCurrentCommandNum() {
        return currentCommandNum;
    }

    public void setCurrentCommandNum(AtomicInteger currentCommandNum) {
        this.currentCommandNum = currentCommandNum;
    }

    public long getTurningPvpId() {
        return turningPvpId;
    }

    public void setTurningPvpId(long turningPvpId) {
        this.turningPvpId = turningPvpId;
    }

    public List<CountedCommandI> getCommandBuffer() {
        return commandBuffer;
    }

    public void clearCommandBuffer() {
        this.commandBuffer = new CopyOnWriteArrayList<CountedCommandI>();
    }

    public PvpBattleStateEnum getBattleState() {
        return battleState;
    }

    public boolean inState(PvpBattleStateEnum state) {
        return battleState == state;
    }

    public void setBattleState(PvpBattleStateEnum battleState) {
        this.battleState = battleState;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(long lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public long getBattleId() {
        return battleId;
    }

    public long getLastChangeStateTime() {
        return lastChangeStateTime;
    }

    public void setLastChangeStateTime(long lastChangeStateTime) {
        this.lastChangeStateTime = lastChangeStateTime;
    }

    public ConcurrentNavigableMap<Short, CountedCommandI> getFutureCommandBuffer() {
        return futureCommandBuffer;
    }

    public void clearFutureCommandBuffer() {
        this.futureCommandBuffer = new ConcurrentSkipListMap<Short, CountedCommandI>();
    }

    public long getStartTurnTime() {
        return startTurnTime;
    }

    public void setStartTurnTime(long startTurnTime) {
        this.startTurnTime = startTurnTime;
        this.sendLongTimeInTurningWarning = false;
    }

    public boolean isSendLongTimeInTurningWarning() {
        return sendLongTimeInTurningWarning;
    }

    public void setSendLongTimeInTurningWarning(boolean sendLongTimeInTurningWarning) {
        this.sendLongTimeInTurningWarning = sendLongTimeInTurningWarning;
    }

    public BattleWager getWager() {
        return wager;
    }

    public int getMapId() {
        return mapId;
    }

    public boolean isPvE() {
        return missionIds != null && missionIds.length > 0;
    }

    public short[] getMissionIds() {
        return missionIds;
    }

    public void setMissionId(short[] missionIds) {
        this.missionIds = missionIds;
    }

    public PvpBattleType getBattleType() {
        return battleType;
    }

    public PvpUser getCreator() {
        return creator;
    }

    public void setCreator(PvpUser creator) {
        this.creator = creator;
    }

    public byte[] getTurningNumByTeam() {
        return turningNumByTeam;
    }

    public void setTurningNumByTeam(byte[] turningNumByTeam) {
        this.turningNumByTeam = turningNumByTeam;
    }

    public List<Byte> getReagents() {
        return reagents;
    }

    public byte[] getReagentsForBattle() {
        if(ArrayUtils.isEmpty(reagentsForBattle)) {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        } else {
            return reagentsForBattle;
        }
    }

    public void setWager(BattleWager wager) {
        this.wager = wager;
    }

    public BattleProposal getBattleProposal() {
        return battleProposal;
    }

    public void setBattleProposal(BattleProposal battleProposal) {
        this.battleProposal = battleProposal;
    }

    public long getStartBattleTime() {
        return startBattleTime;
    }

    public void setStartBattleTime(long startBattleTime) {
        this.startBattleTime = startBattleTime;
    }

    public long getFinishBattleTime() {
        return finishBattleTime;
    }

    public void setFinishBattleTime(long finishBattleTime) {
        this.finishBattleTime = finishBattleTime;
    }

    public PvpBattleStateEnum getWrappedState() {
        return wrappedState;
    }

    public void setWrappedState(PvpBattleStateEnum wrappedState) {
        this.wrappedState = wrappedState;
    }

    public TaskLock getLock() {
        return lock;
    }

    public void setLock(TaskLock lock) {
        this.lock = lock;
    }

    public int getTurnPenaltyTime() {
        return turnPenaltyTime;
    }

    public void setTurnPenaltyTime(int turnPenaltyTime) {
        this.turnPenaltyTime = turnPenaltyTime;
    }

    public void incTurnPenaltyTime(long penaltyTime) {
        this.turnPenaltyTime += penaltyTime;
    }

    public int getBattlePenaltyTime() {
        return battlePenaltyTime;
    }

    public void setBattlePenaltyTime(int battlePenaltyTime) {
        this.battlePenaltyTime = battlePenaltyTime;
    }

    public void incButtlePenaltyTime() {
        this.battlePenaltyTime += this.turnPenaltyTime;
    }

    public long getLastPvpActionExTime() {
        return lastPvpActionExTime;
    }

    public void setLastPvpActionExTime(long lastPvpActionExTime) {
        this.lastPvpActionExTime = lastPvpActionExTime;
    }

    @Override
    public String toString() {
        return "BattleBuffer{" +
                "battleId=" + battleId +
                ", " + battleState +
                '}';
    }

    public Runnable getOnStartBattle() {
        return onStartBattle;
    }

    public void setOnStartBattle(Runnable onStartBattle) {
        this.onStartBattle = onStartBattle;
    }

    public short getQuestId() {
        return wager.questId;
    }

    public void startPvpBattleLogIf(boolean precondition) {
        if(precondition) {
            this.pvpBattleLog = new PvpBattleLog();
        }
    }

    public Optional<PvpBattleLog> getBattleLog() {
        return Optional.ofNullable(pvpBattleLog);
    }
}

