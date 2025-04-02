package com.pragmatix.pvp;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.pvp.model.BattleParticipant;
import jskills.IPlayer;
import jskills.ITeam;
import jskills.Rating;

import java.util.HashMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.04.13 14:49
 */
public class TeamBattleResult extends HashMap<IPlayer, Rating> implements ITeam, Comparable<TeamBattleResult> {

    private PvpBattleResult battleResult;
    private long leaveBattleTime;

    public TeamBattleResult(BattleParticipant battleParticipant, PvpBattleResult battleResult, long leaveBattleTime) {
        super(2);
        IPlayer player = new PvpPlayer(battleParticipant.getPvpUserId());
        Rating rating = new Rating(battleParticipant.getTrueSkillMean(), battleParticipant.getTrueSkillStandardDeviation());
        addPlayer(player, rating);
        this.battleResult = battleResult;
        this.leaveBattleTime = leaveBattleTime;
    }

    public TeamBattleResult(IPlayer player, Rating rating) {
        addPlayer(player, rating);
    }

    public void addPlayer(BattleParticipant battleParticipant) {
        IPlayer player = new PvpPlayer(battleParticipant.getPvpUserId());
        Rating rating = new Rating(battleParticipant.getTrueSkillMean(), battleParticipant.getTrueSkillStandardDeviation());
        addPlayer(player, rating);
    }

    public TeamBattleResult addPlayer(IPlayer player, Rating rating) {
        put(player, rating);
        return this;
    }

    @Override
    public int compareTo(TeamBattleResult o) {
        return compareTo(o.battleResult, o.leaveBattleTime);
    }

    // порядок сортировки обратный!
    public int compareTo(PvpBattleResult battleResult, long leaveBattleTime) {
        int result = battleResult.getWeight() - this.battleResult.getWeight();
        if(result == 0 && battleResult == PvpBattleResult.NOT_WINNER) {
            result = (int) (leaveBattleTime - this.leaveBattleTime);
        }
        return result;
    }

    @Override
    public String toString() {
        return leaveBattleTime > 0 ?
                String.format("%s /%tT/ %s", battleResult.name(), leaveBattleTime, super.keySet()) :
                String.format("%s %s", battleResult.name(), super.keySet());
    }

    boolean isNeedUpdateRating() {
        return battleResult != PvpBattleResult.DRAW_DESYNC;
    }

    public PvpBattleResult getBattleResult() {
        return battleResult;
    }

    public void setBattleResult(PvpBattleResult battleResult) {
        this.battleResult = battleResult;
    }

    public long getLeaveBattleTime() {
        return leaveBattleTime;
    }

    public void setLeaveBattleTime(long leaveBattleTime) {
        this.leaveBattleTime = leaveBattleTime;
    }

}
