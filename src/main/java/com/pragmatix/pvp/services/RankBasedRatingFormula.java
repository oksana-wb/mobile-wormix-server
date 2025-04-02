package com.pragmatix.pvp.services;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.services.rating.RankService;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import io.vavr.Function2;
import io.vavr.Tuple2;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;

import java.util.function.Function;

import static com.pragmatix.app.common.PvpBattleResult.DRAW_GAME;
import static com.pragmatix.app.common.PvpBattleResult.NOT_WINNER;
import static com.pragmatix.app.common.PvpBattleResult.WINNER;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.05.2016 13:50
 */
public class RankBasedRatingFormula implements RatingFormula {

    @Resource
    private RankService rankService;

    @Value("${RatingFormula.defeatMultiplier:0.04}")
    private double defeatMultiplier = 0.04;

    @Value("${RatingFormula.drawMultiplier:0.02}")
    private double drawMultiplier = 0.02;

    //Снимать за проигрыш по 4% и только когда игрок набирает 500 дневного рейтинга
    @Value("${RatingFormula.defeatDailyRatingThreshold:500}")
    private int defeatDailyRatingThreshold = 500;

    @Override
    public int getRatingPoints(BattleBuffer battleBuffer, BattleParticipant battleParticipant, PvpBattleResult result) {
        BattleWager wager = battleBuffer.getWager();
        if(wager.getValue() > 0) {
            if(result == WINNER) {
                return rankService.getVictoryRatingPoints(battleBuffer, battleParticipant, getEnemyRankAndLevel(), getMyRankAndLevel());
            } else if(result == NOT_WINNER && battleParticipant.getDailyRating() >= defeatDailyRatingThreshold) {
                return (int) -Math.floor(battleParticipant.getDailyRating() * defeatMultiplier);
            } else if(result == DRAW_GAME && battleParticipant.getDailyRating() >= defeatDailyRatingThreshold) {
                return (int) -Math.floor(battleParticipant.getDailyRating() * drawMultiplier);
            }
        }
        return 0;
    }

    protected Function<BattleParticipant, Tuple2<Integer, Integer>> getMyRankAndLevel() {
        return rankService::myRankAndLevel;
    }

    protected Function2<BattleBuffer, BattleParticipant, Tuple2<Integer, Integer>> getEnemyRankAndLevel() {
        return rankService::enemyRankAndLevel;
    }

    public void setDefeatMultiplier(double defeatMultiplier) {
        this.defeatMultiplier = defeatMultiplier;
    }

    public void setDefeatDailyRatingThreshold(int defeatDailyRatingThreshold) {
        this.defeatDailyRatingThreshold = defeatDailyRatingThreshold;
    }

    public void setDrawMultiplier(double drawMultiplier) {
        this.drawMultiplier = drawMultiplier;
    }
}
