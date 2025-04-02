package com.pragmatix.pvp.services.matchmaking;

import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.services.matchmaking.lobby.ProposalStat;
import jskills.ITeam;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.11.2015 15:00
 */
public class TestBattleProposal implements BattleProposal {

    public int profileId;

    public boolean lock;
    public boolean validState = true;
    public int size = 1;
    public int level;
    public int rating;
    public int dailyRating;
    public int groupHp;
    public byte[] coliseumProgress;
    public ProposalStat proposalStat = new ProposalStat();
    public double matchQuality;
    public BattleBuffer battleBuffer;
    public BattleWager wager;
    public ITeam jskillTeam;
    public int groupSize;
    public int[] reactionLevel;
    public int gridQuality;
    public int rank;

    @Override
    public boolean tryLock() {
        lock = true;
        return true;
    }

    @Override
    public void unlock() {
        lock = false;
    }

    @Override
    public boolean inValidState() {
        return validState;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getRating() {
        return rating;
    }

    @Override
    public int getDailyRating() {
        return dailyRating;
    }

    @Override
    public int getBattlesCount() {
        return 0;
    }

    @Override
    public int getGroupHp() {
        return groupHp;
    }

    @Override
    public int getGroupSize() {
        return groupSize;
    }

    @Override
    public int[] getReactionLevel() {
        return reactionLevel;
    }

    @Override
    public BattleWager getWager() {
        return wager;
    }

    @Override
    public BattleBuffer getBattleBuffer() {
        return battleBuffer;
    }

    @Override
    public void cancel() {

    }

    @Override
    public ITeam getJSkillTeam() {
        return jskillTeam;
    }

    @Override
    public void setMatchQuality(double matchQuality) {
        this.matchQuality = matchQuality;
    }

    @Override
    public int getGridQuality() {
        return gridQuality;
    }

    @Override
    public void incGridQuality() {
        gridQuality++;
    }

    @Override
    public ProposalStat getStat() {
        return proposalStat;
    }

    public byte[] getAuxMatchParams() {
        return coliseumProgress;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public String getClientAddress() {
        return "";
    }

    @Override
    public byte getSocialNetId() {
        return 0;
    }

    @Override
    public int getRank() {
        return rank;
    }

    @Override
    public int compareTo(BattleProposal o) {
        return 0;
    }

    @Override
    public String toString() {
        return "(" + profileId + "){" +
                "gridQuality=" + gridQuality +
                ", coliseumProgress=" + Arrays.toString(coliseumProgress) +
                '}';
    }
}
