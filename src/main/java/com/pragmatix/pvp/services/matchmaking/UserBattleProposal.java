package com.pragmatix.pvp.services.matchmaking;

import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.gameapp.task.TaskLock;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpPlayer;
import com.pragmatix.pvp.messages.PvpProfileStructure;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpService;
import jskills.Rating;
import jskills.Team;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.11.12 18:52
 */
public class UserBattleProposal extends TeamBattleProposal {

    private Object auxMatchParams;

    public UserBattleProposal(TaskLock lock, BattleParticipant participant, BattleWager wager, byte[] units, BattleBuffer battle, PvpService pvpService) {
        super(lock);
        PvpProfileStructure profileStructure = participant.getPvpProfileStructure();
        PvpUser user = pvpService.getUser(participant.getPvpUserId());

        this.wager = wager;
        this.groupHp = participant.getGroupHp();
        this.groupSize = participant.teamSize;
        this.units = units;
        this.team = new PvpUser[]{user};
        this.battle = battle;
        WormStructure masterWorm = GroupHpService.getMasterWorm(profileStructure);
        this.level = masterWorm.level;
        this.version = participant.getVersion();
        this.clientAddress = participant.clientAddress;
        this.reactionLevel = pvpService.getReactionLevel(profileStructure);
        this.rating = profileStructure.rating;
        this.dailyRating = profileStructure.dailyRating;
        this.battlesCount = participant.battlesCount;
        this.teamParticipants = battle.getParticipants();
        this.auxMatchParams = participant.getAuxMatchParams();

        participant.setBattleProposal(this);

        jskillTeam = new Team(new PvpPlayer(participant.getPvpUserId()), new Rating(participant.trueSkillMean, participant.trueSkillStandardDeviation));

        if(pvpService.getRankService() != null) {
            rank = pvpService.getRankService().getPlayerRealRank(participant);
        }
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public boolean inValidState() {
        return team[0].isOnline() && !canceled;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        UserBattleProposal that = (UserBattleProposal) o;

        if(!team[0].equals(that.team[0])) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return team[0].hashCode();
    }

    public PvpUser getUser() {
        return team[0];
    }

    public BattleParticipant getBattleParticipant() {
        return teamParticipants.get(0);
    }

    public Object getAuxMatchParams() {
        return auxMatchParams;
    }

    @Override
    public String toString() {
        return "User{" +
                "" + (team == null ? null : Arrays.asList(team)) +
                ", level=" + level +
                ", units=" + Arrays.toString(units) +
                ", groupHp=" + groupHp +
                ", gridQuality=" + gridQuality +
                ", auxMatchParams=" + auxMatchParams +
                ", rank=" + rank +
                ", ip=" + clientAddress +
                ", " + jskillTeam +
                '}';
    }
}
