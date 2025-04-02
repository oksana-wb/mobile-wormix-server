package com.pragmatix.app.services;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.dao.TrueSkillDao;
import com.pragmatix.app.domain.TrueSkillEntity;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.pvp.TeamBattleResult;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.services.matchmaking.BattleProposal;
import jskills.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.04.13 13:35
 */
@Service
public class TrueSkillService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final double defaultInitialMean = 1.0;
    private static final double defaultInitialStandardDeviation = defaultInitialMean / 3.0;
    private static final double defaultBeta = defaultInitialMean / 6.0;
    private static final double defaultDynamicsFactor = defaultInitialMean / 300.0;
    private static final double defaultDrawProbability = 0.10;

    @Resource
    private TrueSkillDao trueSkillDao;

    private final GameInfo gameInfo;

    private double rubyLeagueSkillMean = 1.63;
    private double rubyLeagueSkillDeviation = 0.01;
    private Rating rubyLeagueRating; // 800

    private int rubyDailyRatingThreshold = 1000;

    public TrueSkillService() {
        gameInfo = new GameInfo(defaultInitialMean, defaultInitialStandardDeviation, defaultBeta, defaultDynamicsFactor, defaultDrawProbability);
        updateRubyLeagueRating();
    }

    private void updateRubyLeagueRating() {
        rubyLeagueRating = new Rating(rubyLeagueSkillMean, rubyLeagueSkillDeviation);
    }

    public double calculateMatchQuality(BattleProposal firstProposal, BattleProposal secondProposal) {
        // Мастерство игрока не должно опускаться ниже определенного уровня, если он достиг рубиновой лиги
        //  или набрал больше 1000 рейтинга за день
        ITeam firstSkill = firstProposal.getRank() == 0 || firstProposal.getDailyRating() >= rubyDailyRatingThreshold ? bestSkill(firstProposal) : firstProposal.getJSkillTeam();
        ITeam secondSkill = secondProposal.getRank() == 0 || secondProposal.getDailyRating() >= rubyDailyRatingThreshold ? bestSkill(secondProposal) : secondProposal.getJSkillTeam();
        return TrueSkillCalculator.calculateMatchQuality(gameInfo, Arrays.asList(firstSkill, secondSkill));
    }

    private ITeam newRubyTeam(BattleProposal proposal) {
        return new Team(proposal.getJSkillTeam().keySet().iterator().next(), rubyLeagueRating);
    }

    private ITeam bestSkill(BattleProposal proposal) {
        Rating rating = proposal.getJSkillTeam().values().iterator().next();
        int skill = (int) Math.round((rating.getMean() - rating.getStandardDeviation() * (double) 3) * (double) 500);
        int rubySkill = (int) Math.round((rubyLeagueSkillMean - rubyLeagueSkillDeviation * (double) 3) * (double) 500);
        return skill > rubySkill ? proposal.getJSkillTeam() : newRubyTeam(proposal);
    }

    public static int trueSkillRating(double mean, double standardDeviation) {
        return (int) Math.round((mean - standardDeviation * (double) 3) * (double) 500);
    }

    public TrueSkillEntity getTrueSkillFor(UserProfile userProfile) {
        if(userProfile.getTrueSkillEntity() == null) {
            TrueSkillEntity trueSkillEntity = trueSkillDao.get(userProfile.getId());
            if(trueSkillEntity == null) {
                trueSkillEntity = new TrueSkillEntity(userProfile.getId().intValue(), defaultInitialMean, defaultInitialStandardDeviation);
            }
            userProfile.setTrueSkillEntity(trueSkillEntity);
        }
        return userProfile.getTrueSkillEntity();
    }

    public void wipeTrueSkill(UserProfile profile) {
        TrueSkillEntity trueSkillEntity = getTrueSkillFor(profile);
        trueSkillEntity.setMean(defaultInitialMean);
        trueSkillEntity.setStandardDeviation(defaultInitialStandardDeviation);
        trueSkillEntity.setBattles(0);
        trueSkillEntity.setDirty(true);
    }

    public Map<IPlayer, Rating> calculateNewRatings(BattleBuffer battleBuffer, Map<Byte, TeamBattleResult> resultByTeams) {
        boolean updateTrueRating = battleBuffer.getWager().getValue() > 0 && resultByTeams.size() >= 2;
        if(!updateTrueRating) {
            return null;
        }
        List<TeamBattleResult> teamBattleResults = new ArrayList<>(resultByTeams.values());
        if(log.isTraceEnabled()) {
            for(TeamBattleResult teamBattleResult : teamBattleResults) {
                for(Map.Entry<IPlayer, Rating> ratingEntry : teamBattleResult.entrySet()) {
                    log.trace(String.format("battleId=%s: skill %s %s", battleBuffer.getBattleId(), ratingEntry.getKey(), ratingEntry.getValue()));
                }
            }
        }
        Collections.sort(teamBattleResults);

        int[] teamRanks = calculateTeamRanks(teamBattleResults);

        if(log.isTraceEnabled()) {
            log.trace(String.format("battleId=%s: sorted %s ranks: %s", battleBuffer.getBattleId(), teamBattleResults, Arrays.toString(teamRanks)));
        }

        Map<IPlayer, Rating> playerRatingMap = TrueSkillCalculator.calculateNewRatings(gameInfo, teamBattleResults, teamRanks);

        // удаляем рейтинг тех участников которые завершили бой рассинхроном
        for(TeamBattleResult teamBattleResult : teamBattleResults) {
            if(teamBattleResult.getBattleResult() == PvpBattleResult.DRAW_DESYNC) {
                for(IPlayer desyncedPlayer : teamBattleResult.keySet()) {
                    playerRatingMap.remove(desyncedPlayer);
                }
            }
        }
        if(log.isTraceEnabled()) {
            for(Map.Entry<IPlayer, Rating> ratingEntry : playerRatingMap.entrySet()) {
                log.trace(String.format("battleId=%s: new skill %s %s", battleBuffer.getBattleId(), ratingEntry.getKey(), ratingEntry.getValue()));
            }
        }
        return playerRatingMap;
    }

    // метод возврящает занятые командами места
    private int[] calculateTeamRanks(List<TeamBattleResult> teamBattleResults) {
        int[] result = new int[teamBattleResults.size()];
        PvpBattleResult lastResult = teamBattleResults.get(0).getBattleResult();
        int rank = 1;
        result[0] = rank;
        for(int i = 1; i < teamBattleResults.size(); i++) {
            PvpBattleResult battleResult = teamBattleResults.get(i).getBattleResult();
            if(battleResult != lastResult || battleResult == PvpBattleResult.NOT_WINNER || battleResult == PvpBattleResult.WINNER) {
                rank++;
            }
            result[i] = rank;
            lastResult = battleResult;
        }
        return result;
    }

    public int getRubyLeagueSkill() {
        return (int) Math.round((rubyLeagueSkillMean - rubyLeagueSkillDeviation * (double) 3) * (double) 500);
    }

    //====================== Getters and Setters =================================================================================================================================================

    public void setRubyLeagueSkillMean(double rubyLeagueSkillMean) {
        this.rubyLeagueSkillMean = rubyLeagueSkillMean;
        updateRubyLeagueRating();
    }

    public void setRubyLeagueSkillDeviation(double rubyLeagueSkillDeviation) {
        this.rubyLeagueSkillDeviation = rubyLeagueSkillDeviation;
        updateRubyLeagueRating();
    }

    public double getRubyLeagueSkillMean() {
        return rubyLeagueSkillMean;
    }

    public double getRubyLeagueSkillDeviation() {
        return rubyLeagueSkillDeviation;
    }

}
