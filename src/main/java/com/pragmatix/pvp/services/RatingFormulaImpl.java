package com.pragmatix.pvp.services;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import static com.pragmatix.app.common.PvpBattleResult.NOT_WINNER;
import static com.pragmatix.app.common.PvpBattleResult.WINNER;

/**
 * Расчет набранных.потерянных очков рейтинга по итогам PVP боя
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 29.07.13 15:49
 */
public class RatingFormulaImpl implements RatingFormula {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private double winDeltaV1 = 35;
    private double winDeltaV2 = 2.5;

    private double notWinDeltaV1 = 38;
    private double notWinDeltaV2 = 4;

    private double sigmaV1 = 0.25;
    private double sigmaV2 = 1000;
    private double sigmaV3 = 5;

    private double EXP_GAIN_THRESHOLD = 1900;
    private double EXP_LOOSE_THRESHOLD = 1600;
    private double CUT_PERCENTAGE = 0.05;

    //Снимать за проигрыш только когда игрок набирает 300 дневного рейтинга
    @Value("${RatingFormula.defeatDailyRatingThreshold:300}")
    private int defeatDailyRatingThreshold = 300;

    public double[] teamSizeModifiers;

    private boolean isNeedCountRating(PvpBattleResult result, BattleWager wager) {
        return (result == NOT_WINNER || result == WINNER) && wager.getValue() > 0;
    }

    /**
     * расчитать количество полученного (потерянного) рейтинга
     */
    @Override
    public int getRatingPoints(BattleBuffer battleBuffer, BattleParticipant battleParticipant, PvpBattleResult result) {
        BattleWager wager = battleBuffer.getWager();
        if(!isNeedCountRating(result, wager)) {
            return 0;
        }
        double myTeamLevelSum = 0;
        double myTeamGroupLevelSum = 0;
        double myTeamSize = 0;
        double opponentTeamGroupLevelSum = 0;
        double opponentTeamSize = 0;

        for(BattleParticipant participant : battleBuffer.getParticipants()) {
            if(participant.getPlayerTeam() == battleParticipant.getPlayerTeam()) {
                myTeamGroupLevelSum += Math.min(30, participant.getGroupLevel());
                myTeamLevelSum += Math.min(30, participant.getLevel());
                myTeamSize++;
            } else {
                opponentTeamGroupLevelSum +=  Math.min(30, participant.getGroupLevel());
                opponentTeamSize++;
            }
        }

        double myLevel = myTeamLevelSum / myTeamSize;// мой уровень (если команда то средний)
        double myTeamLevel = (myTeamGroupLevelSum - myTeamLevelSum) / myTeamSize; // средняя (если комонда) разница между суммой уровней всех юнитов команды и суммой уровней воладельцев
        double myAvgSquadLevel = myTeamGroupLevelSum / myTeamSize;//средний уровень команды со стороны игрока
        double enemyAvgSquadLevel = opponentTeamGroupLevelSum / opponentTeamSize;//средний уровень команды противника

        int dailyRating = battleParticipant.getDailyRating();
        float ratingBonus = wager.getRatingBonus();
        int ratingPoints = getRatingPoints(result, myLevel, myTeamLevel, dailyRating, ratingBonus, myAvgSquadLevel, enemyAvgSquadLevel, battleParticipant.teamSize);

        if(log.isDebugEnabled()) {
            long battleId = battleBuffer.getBattleId();
            log.debug(String.format("[%s] RatingPoints: %s {%s, %s, %s, %s, %s, %s, %s} battleId=%s",
                    PvpService.formatPvpUserId(battleParticipant.getPvpUserId()), ratingPoints, result, dailyRating, wager, myLevel, myTeamLevel, myAvgSquadLevel, enemyAvgSquadLevel, battleId));
        }

        return ratingPoints;
    }

    public int getRatingPoints(PvpBattleResult result, double myLevel, double myTeamLevel, int dailyRating, double wagerRatingBonus,
                               double myAvgSquadLevel, double enemyAvgSquadLevel, int teamSize) {
        double teamRatio = enemyAvgSquadLevel / myAvgSquadLevel;
        myTeamLevel = Math.min(3 * myLevel, myTeamLevel);

        int ratingPoints = 0;
        if(result == WINNER) {
            double sigmaCoeff = dailyRating > EXP_GAIN_THRESHOLD ? sigma(dailyRating) : 1.0;
            double delta = Math.max(2, Math.ceil(1.0 * myLevel * myLevel / winDeltaV1 + Math.floor(myTeamLevel / winDeltaV2)));
            ratingPoints = 1 + (int) Math.round((1 + wagerRatingBonus) * delta * sigmaCoeff * teamRatio);
            ratingPoints = (int) Math.round(ratingPoints * teamSizeModifiers[teamSize - 1]);
        } else if(result == NOT_WINNER && dailyRating >= defeatDailyRatingThreshold) {
            double delta = Math.ceil(1.0 * myLevel * myLevel / notWinDeltaV1 + Math.floor(myTeamLevel / notWinDeltaV2));
            ratingPoints = (int) -Math.ceil(delta / teamRatio + linearCut(dailyRating));
        }

        return ratingPoints;
    }

    private double sigma(int x) {
        return Math.max(sigmaV1, 1 / (1 + Math.exp(x / sigmaV2 - sigmaV3)));
    }

    private double linearCut(int x) {
        return x > EXP_LOOSE_THRESHOLD ? CUT_PERCENTAGE * (x - EXP_LOOSE_THRESHOLD) : 0;
    }

    public void setTeamSizeModifiers(double[] teamSizeModifiers) {
        this.teamSizeModifiers = teamSizeModifiers;
    }

    @Override
    public String toString() {
        return "RatingFormula{\n" +
                " winDeltaV1=" + winDeltaV1 + '\n' +
                " winDeltaV2=" + winDeltaV2 + '\n' +
                " notWinDeltaV1=" + notWinDeltaV1 + '\n' +
                " notWinDeltaV2=" + notWinDeltaV2 + '\n' +
                " sigmaV1=" + sigmaV1 + '\n' +
                " sigmaV2=" + sigmaV2 + '\n' +
                " sigmaV3=" + sigmaV3 + '\n' +
                " EXP_GAIN_THRESHOLD=" + EXP_GAIN_THRESHOLD + '\n' +
                " EXP_LOOSE_THRESHOLD=" + EXP_LOOSE_THRESHOLD + '\n' +
                " CUT_PERCENTAGE=" + CUT_PERCENTAGE + '\n' +
                '}';
    }
}
