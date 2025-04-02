package com.pragmatix.pvp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.pvp.messages.PvpProfileStructure;
import com.pragmatix.pvp.messages.battle.server.EndPvpBattleResultStructure;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.matchmaking.TeamBattleProposal;
import com.pragmatix.sessions.IAppServer;
import io.vavr.Tuple3;

import java.sql.Time;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.pragmatix.app.common.PvpBattleResult.DRAW_DESYNC;
import static com.pragmatix.app.common.PvpBattleResult.DRAW_SHUTDOWN;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.06.11 11:26
 */
public class BattleParticipant {

    public enum State {
        needProfile(10, false, false),
        waitConnect(11, false, false),
        connectedAndHasProfile(12, false, false),
        readyForBattle(13, false, false),
        sendCommand(14, true, true),
        waitEndTurnResponce(15, true, true),
        submitEndTurnResponse(16, true, true),
        acceptCommand(17, true, true),
        waitReconnect(18, true, true),
        waitTransferTurn(19, true, true),
        // выбыл из боя disconnect, timeout, cheat, surrendered
        disconnect(2, false, false, true, false),
        commandTimeout(3, false, false, true, true),
        responseTimeout(4, false, false, true, true),
        error(5, false, false, true, true),
        cheat(6, false, false, true, false),
        surrendered(7, false, false, true, false),
        // ничья
        draw(20, false, false),
        desync(21, false, false),
        // почил с миром
        droppedFromBattle(22, true, false),
        droppedAndWaitEndTurnResponce(23, true, false),
        droppedAndSubmitEndTurnResponse(24, true, false),

        timeoutInEnvBattle(25, false, false),

        finate(0, false, false),
        waitEndBattleRequestConfirm(1, false, false),;

        private boolean canAccept;
        private boolean canTurn;
        private boolean endBattle;
        private boolean needDump;

        // код нужен исключительно для целей логгирования
        // коды < 10 наиболее логгируемые
        public final int type;

        State(int type, boolean canAccept, boolean canTurn) {
            this.type = type;
            this.canAccept = canAccept;
            this.canTurn = canTurn;
        }

        State(int type, boolean canAccept, boolean canTurn, boolean endBattle, boolean needDump) {
            this.type = type;
            this.canAccept = canAccept;
            this.canTurn = canTurn;
            this.endBattle = endBattle;
            this.needDump = needDump;
        }

        public boolean canAccept() {
            return canAccept;
        }

        public boolean canTurn() {
            return canTurn;
        }

        public boolean endBattle() {
            return endBattle;
        }

        public boolean droppedFromBattle() {
            return canAccept && !canTurn;
        }

        public boolean isNeedDump() {
            return needDump;
        }

        public static final Map<Integer, State> valuesMap = Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(it -> it.type, it -> it));

        @JsonCreator
        public static State valueOf(int type) {
            return valuesMap.get(type);
        }
    }

    private final Long pvpUserId;

    private final IAppServer mainServer;

    private byte playerNum;

    private byte playerTeam;

    public State stateBeforeDisconnect;

    public State stateBeforeFinate;

    private volatile State state;

    private volatile long disconnectTime = 0;

    //Рейтинги просчитанные на случай победы, поражения игрока или ничьи
    public Tuple3<EndPvpBattleResultStructure, EndPvpBattleResultStructure, EndPvpBattleResultStructure> preCalculatedPoints;

    public PvpBattleResult battleResult;

    private long leaveBattleTime;

    private PvpProfileStructure pvpProfileStructure;

    private final List<BackpackItemStructure> items = new CopyOnWriteArrayList<>();

    private TeamBattleProposal battleProposal;

    private boolean droppedFromBattle;

    /**
     * отряд в займы
     */
    private byte troopOnLoan = -1;

    public int battlesCount;

    // значение TrueSkill до боя рейтинга

    /**
     * The statistical mean value of the rating (also known as μ). *
     */
    public double trueSkillMean;

    /**
     * The standard deviation (the spread) of the rating. This is also known as σ. *
     */
    public double trueSkillStandardDeviation;

    // значение TrueSkill рейтинга после боя
    public double newTrueSkillMean = -1;
    public double newTrueSkillStandardDeviation = -1;

    private int level;

    private int dailyRating;

    private int groupLevel;

    private int groupHp;

    private Object auxMatchParams;

    public int teamSize;

    private int version;

    public int profileRankPoints;

    public int bestRank;

    /**
     * Битовая маска наложенных (на момент начала боя) запретов
     */
    public short restrictionBlocks;

    public int bossWinAwardToken;

    public String clientAddress;

    public long offlineTime;

    public BattleParticipant(long profileId, byte socialNetId, State state, int playerNum, int playerTeam, IAppServer mainServer) {
        this.pvpUserId = PvpService.getPvpUserId(profileId, socialNetId);
        this.state = state;
        this.playerNum = (byte) playerNum;
        this.playerTeam = (byte) playerTeam;
        this.mainServer = mainServer;
    }

    private static final EndPvpBattleResultStructure DRAW_SHUTDOWN_RESULT = new EndPvpBattleResultStructure(DRAW_SHUTDOWN, 0, 0);
    private static final EndPvpBattleResultStructure DRAW_DESYNC_RESULT = new EndPvpBattleResultStructure(DRAW_DESYNC, 0, 0);

    public EndPvpBattleResultStructure battleResultStructure() {
        switch (battleResult) {
            case WINNER:
                return preCalculatedPoints._1;
            case NOT_WINNER:
                return preCalculatedPoints._2;
            case DRAW_GAME:
                return preCalculatedPoints._3;

            case DRAW_SHUTDOWN:
                return DRAW_SHUTDOWN_RESULT;
            default:
                return DRAW_DESYNC_RESULT;
        }
    }

    public long getProfileId() {
        return PvpService.getProfileId(pvpUserId);
    }

    public byte getSocialNetId() {
        return PvpService.getSocialNetId(pvpUserId);
    }

    public Long getPvpUserId() {
        return pvpUserId;
    }

    public State getState() {
        return state;
    }

    public boolean inState(State expectedState) {
        return state.equals(expectedState);
    }

    public void setState(State state) {
        this.state = state;
    }

    public long getDisconnectTime() {
        return disconnectTime;
    }

    public void setDisconnectTime(long disconnectTime) {
        this.disconnectTime = disconnectTime;
    }

    public byte getPlayerNum() {
        return playerNum;
    }

    public IAppServer getMainServer() {
        return mainServer;
    }

    public PvpProfileStructure getPvpProfileStructure() {
        return pvpProfileStructure;
    }

    public void setPvpProfileStructure(PvpProfileStructure pvpProfileStructure) {
        this.pvpProfileStructure = pvpProfileStructure;
    }

    public byte getPlayerTeam() {
        return playerTeam;
    }

    public List<BackpackItemStructure> getItems() {
        return items;
    }

    public void setPlayerNum(byte playerNum) {
        this.playerNum = playerNum;
        if(pvpProfileStructure != null) {
            pvpProfileStructure.playerNum = playerNum;
        }
    }

    public void setPlayerTeam(byte playerTeam) {
        this.playerTeam = playerTeam;
        if(pvpProfileStructure != null) {
            pvpProfileStructure.playerTeam = playerTeam;
        }
    }

    public TeamBattleProposal getBattleProposal() {
        return battleProposal;
    }

    public void setBattleProposal(TeamBattleProposal battleProposal) {
        this.battleProposal = battleProposal;
    }

    public long getLeaveBattleTime() {
        return leaveBattleTime;
    }

    public void setLeaveBattleTime(long leaveBattleTime) {
        this.leaveBattleTime = leaveBattleTime;
    }

    public String formatUserId() {
        return PvpService.formatPvpUserId(pvpUserId);
    }

    public boolean isDroppedFromBattle() {
        return droppedFromBattle;
    }

    public void setDroppedFromBattle(boolean droppedFromBattle) {
        this.droppedFromBattle = droppedFromBattle;
    }

    public boolean canTurn() {
        return state.canTurn();
    }

    public boolean endBattle() {
        return state.endBattle;
    }

    public byte getTroopOnLoan() {
        return troopOnLoan;
    }

    public void setTroopOnLoan(byte troopOnLoan) {
        this.troopOnLoan = troopOnLoan;
    }

    public boolean isEnvParticipant() {
        return getSocialNetId() == 0;
    }

    public double getTrueSkillMean() {
        return trueSkillMean;
    }

    public void setTrueSkillMean(double trueSkillMean) {
        this.trueSkillMean = trueSkillMean;
    }

    public double getTrueSkillStandardDeviation() {
        return trueSkillStandardDeviation;
    }

    public void setTrueSkillStandardDeviation(double trueSkillStandardDeviation) {
        this.trueSkillStandardDeviation = trueSkillStandardDeviation;
    }

    public double getNewTrueSkillMean() {
        return newTrueSkillMean;
    }

    public void setNewTrueSkillMean(double newTrueSkillMean) {
        this.newTrueSkillMean = newTrueSkillMean;
    }

    public double getNewTrueSkillStandardDeviation() {
        return newTrueSkillStandardDeviation;
    }

    public void setNewTrueSkillStandardDeviation(double newTrueSkillStandardDeviation) {
        this.newTrueSkillStandardDeviation = newTrueSkillStandardDeviation;
    }

    public void setGroupLevel(int groupLevel) {
        this.groupLevel = groupLevel;
    }

    public int getGroupLevel() {
        return groupLevel;
    }

    public int getLevel() {
        return level;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getDailyRating() {
        return dailyRating;
    }

    public void setDailyRating(int dailyRating) {
        this.dailyRating = dailyRating;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getProfileRankPoints() {
        return profileRankPoints;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BattleParticipant{");
        sb.append("id=").append(formatUserId());
        sb.append(", num=").append(playerNum);
        sb.append(", team=").append(playerTeam);
        sb.append(", state=").append(state);
        sb.append(", ip=").append(clientAddress);
        if(droppedFromBattle) {
            sb.append(", dropped");
        }
        if(troopOnLoan >= 0) {
            sb.append(", troopOnLoan=").append(troopOnLoan);
        }
        if(battleResult != null) {
            sb.append(", battleResultStructure=").append(battleResultStructure());
        }
        if(items.size() > 0) {
            sb.append(", items=").append(items);
        }
        if(disconnectTime > 0) {
            sb.append(", disconnectTime=").append(new Time(disconnectTime));
        }
        sb.append('}');
        return sb.toString();
    }

    public int getGroupHp() {
        return groupHp;
    }

    public void setGroupHp(int groupHp) {
        this.groupHp = groupHp;
    }

    public Object getAuxMatchParams() {
        return auxMatchParams;
    }

    public void setAuxMatchParams(Object auxMatchParams) {
        this.auxMatchParams = auxMatchParams;
    }

    public short getRestrictionBlocks() {
        return restrictionBlocks;
    }

    public void setRestrictionBlocks(short restrictionBlocks) {
        this.restrictionBlocks = restrictionBlocks;
    }
}

