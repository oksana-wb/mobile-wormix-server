package com.pragmatix.pvp.services.matchmaking;

import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.app.services.ReactionRateService;
import com.pragmatix.app.services.rating.RankService;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.task.TaskLock;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpPlayer;
import com.pragmatix.pvp.messages.PvpProfileStructure;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.matchmaking.lobby.ProposalStat;
import jskills.ITeam;
import jskills.Rating;
import jskills.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.11.12 18:52
 */
public class TeamBattleProposal implements BattleProposal {

    protected BattleWager wager;

    protected int groupHp;

    protected int groupSize;
    /**
     * уровни юнитов членов команды
     */
    protected byte[] units;

    protected PvpUser[] team;

    protected int level;

    protected int rating;

    protected int dailyRating = 0;

    protected int[] reactionLevel = {0, 0};

    protected int battlesCount;

    protected BattleBuffer battle;

    protected final TaskLock lock;

    protected volatile boolean canceled = false;

    protected List<BattleParticipant> teamParticipants;
    /**
     * time in matchmaking lobby
     */
    protected long lobbyTime = System.currentTimeMillis();

    protected ITeam jskillTeam;

    protected double matchQuality;

    protected volatile int gridQuality = 0;

    protected ProposalStat stat = new ProposalStat();

    protected int version;

    protected String clientAddress = "";

    protected int rank = RankService.INIT_RANK_VALUE;

    public TeamBattleProposal(TaskLock lock) {
        this.lock = lock;
    }

    public void setTeam(PvpUser[] team) {
        this.team = team;
    }

    public TeamBattleProposal(UserBattleProposal... teamParticipants) {
        this(new TaskLock());
        this.teamParticipants = new ArrayList<>(teamParticipants.length);
        team = new PvpUser[teamParticipants.length];
        int count = 0;
        int battlesCount = 0;
        int rank = RankService.INIT_RANK_VALUE;
        for(UserBattleProposal participant : teamParticipants) {
            count++;
            team[count - 1] = participant.getUser();
            level += participant.level;
            version = participant.version;
            reactionLevel = ReactionRateService.sumReaction(reactionLevel, participant.reactionLevel);
            rating += participant.rating;
            dailyRating = Math.max(dailyRating, participant.dailyRating);
            battlesCount = Math.max(battlesCount, participant.getBattlesCount());

            this.teamParticipants.add(participant.getBattleParticipant());
            rank = Math.min(rank, participant.rank);
        }
        if(count > 0) {
            this.wager = teamParticipants[0].wager;
        }
        this.units = new byte[0];
        level = level / count;
        this.rank = rank;
    }

    public TeamBattleProposal(List<BattleParticipant> teamParticipants, BattleWager wager, byte[] units, BattleBuffer battle, PvpService pvpService) {
        this(new TaskLock());
        team = new PvpUser[teamParticipants.size()];
        this.wager = wager;
        this.groupSize = 2;
        this.units = units;
        this.battle = battle;
        this.teamParticipants = teamParticipants;
        int count = 0;
        int rank = RankService.INIT_RANK_VALUE;
        for(BattleParticipant participant : teamParticipants) {
            count++;
            team[count - 1] = pvpService.getUser(participant.getPvpUserId());
            PvpProfileStructure profileStructure = participant.getPvpProfileStructure();
            reactionLevel = ReactionRateService.sumReaction(reactionLevel, pvpService.getReactionLevel(profileStructure));
            if(count == 1){
                version = participant.getVersion();
                clientAddress = participant.clientAddress;
            }
            participant.setBattleProposal(this);
            if(pvpService.getRankService() != null) {
                rank = Math.min(rank, pvpService.getRankService().getPlayerRealRank(participant));
            }
        }
        BattleParticipant levelHigherParticipant = getLevelHigherParticipant(teamParticipants.get(0), teamParticipants.get(1));
        PvpProfileStructure profileStructure = levelHigherParticipant.getPvpProfileStructure();
        WormStructure masterWorm = GroupHpService.getMasterWorm(profileStructure);
        this.level = masterWorm.level;
        this.rating = profileStructure.rating;
        this.dailyRating = Math.max(teamParticipants.get(0).getDailyRating(), teamParticipants.get(1).getDailyRating());
        this.groupHp = levelHigherParticipant.getGroupHp();

        BattleParticipant skillHigherParticipant = getSkillHigherParticipant(teamParticipants.get(0), teamParticipants.get(1));
        jskillTeam = new Team(new PvpPlayer(skillHigherParticipant.getPvpUserId()), new Rating(skillHigherParticipant.trueSkillMean, skillHigherParticipant.trueSkillStandardDeviation));
        this.rank = rank;
    }

    // определяем участника c наибольшим рейтингом. По нему будет вестись подбор (кроме мастерства)
    private BattleParticipant getLevelHigherParticipant(BattleParticipant participant1, BattleParticipant participant2) {
        return participant1.getLevel() > participant2.getLevel() ? participant1 : participant2;
    }

    // определяем участника c наибольшим мастерством. По нему будет вестись подбор по мастерству
    private BattleParticipant getSkillHigherParticipant(BattleParticipant participant1, BattleParticipant participant2) {
        int trueSkill1 = (int) Math.round((participant1.trueSkillMean - participant1.trueSkillStandardDeviation * (double) 3) * (double) 500);
        int trueSkill2 = (int) Math.round((participant2.trueSkillMean - participant2.trueSkillStandardDeviation * (double) 3) * (double) 500);
        return trueSkill1 > trueSkill2 ? participant1 : participant2;
    }

    @Override
    public ITeam getJSkillTeam() {
        return jskillTeam;
    }

    @Override
    public boolean tryLock() {
        return inValidState() && lock.tryLock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    @Override
    public boolean inValidState() {
        for(PvpUser pvpUser : team) {
            if(!pvpUser.isOnline()) {
                return false;
            }
        }
        return !canceled;
    }

    @Override
    public int getSize() {
        return 2;
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
        return battlesCount;
    }

    @Override
    public int getGroupHp() {
        return groupHp;
    }

    @Override
    public int getGroupSize() {
        return groupSize;
    }

    public byte[] getUnits() {
        return units;
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
        return battle;
    }

    @Override
    public void cancel() {
        canceled = true;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        TeamBattleProposal that = (TeamBattleProposal) o;

        if(!Arrays.equals(team, that.team)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(team);
    }

    public PvpUser[] getTeam() {
        return team;
    }

    public List<BattleParticipant> getTeamParticipants() {
        return teamParticipants;
    }

    public long getLobbyTime() {
        return lobbyTime;
    }

    public void setLobbyTime(long lobbyTime) {
        // устанавливаем время единожды
        if(lobbyTime < 1000 * 60 * 60 * 24) {
            this.lobbyTime = lobbyTime;
        }
    }

    @Override
    public int compareTo(BattleProposal o) {
        return ReactionRateService.compareReaction(this.reactionLevel, o.getReactionLevel());
    }

    public double getMatchQuality() {
        return matchQuality;
    }

    public void setMatchQuality(double matchQuality) {
        this.matchQuality = matchQuality;
    }

    public ProposalStat getStat() {
        return stat;
    }

    public Object getAuxMatchParams() {
        return new Object();
    }

    public boolean isCanceled() {
        return canceled;
    }

    public int getGridQuality() {
        return gridQuality;
    }

    public void incGridQuality() {
        this.gridQuality++;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public String getClientAddress() {
        return clientAddress;
    }

    @Override
    public byte getSocialNetId() {
        return team[0].getSocialId();
    }

    @Override
    public int getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return "Team{" +
                "" + (team == null ? null : Arrays.asList(team)) +
                ", version=" + AppUtils.versionToString(version) +
                ", level=" + level +
                ", units=" + Arrays.toString(units) +
                ", gridQuality=" + gridQuality +
                ", groupHp=" + groupHp +
                ", rank=" + rank +
                ", " + jskillTeam +
                '}';
    }
}
