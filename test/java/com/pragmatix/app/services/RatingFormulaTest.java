package com.pragmatix.app.services;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.pvp.services.RatingFormula;
import com.pragmatix.pvp.services.RatingFormulaImpl;
import com.pragmatix.testcase.AbstractTest;
import org.junit.Test;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 29.07.13 16:53
 */
public class RatingFormulaTest extends AbstractTest {

    RatingFormulaImpl ratingFormula = new RatingFormulaImpl();

    @Test
    public void testNewFormula() throws Exception {
        RatingParams paramsSkinny = new RatingParams(30, 0, 30, 30);
        RatingParams paramsFat = new RatingParams(30, 90, 120, 120);

        for(int rating = 500; rating <= 7000; rating += 500) {
            System.out.println(getRatingPoints(paramsSkinny.win(rating)));
            System.out.println(getRatingPoints(paramsSkinny.loose(rating)));
            System.out.println(getRatingPoints(paramsFat.win(rating)));
            System.out.println(getRatingPoints(paramsFat.loose(rating)));
        }
    }

    @Test
    public void testGetRatingPoints() throws Exception {
        RatingParams params = new RatingParams(30, 0, 30, 30);
        int rating = 2000;
        System.out.println(getRatingPoints(params.win(rating)));
        System.out.println(getRatingPoints(params.loose(rating)));
    }

    private RatingParams getRatingPoints(RatingParams params) {
        params.ratingPoints = ratingFormula.getRatingPoints(params.result, params.myLevel, params.myTeamLevel, params.playerDailyRating, params.wagerRatingBonus, params.myAvgSquadLevel, params.enemyAvgSquadLevel, 4);
        return params;
    }

    class RatingParams {
        PvpBattleResult result;
        final double myLevel;
        final double myTeamLevel;
        int playerDailyRating;
        final double wagerRatingBonus = 0;
        final double myAvgSquadLevel;
        final double enemyAvgSquadLevel;

        int ratingPoints;

        RatingParams(double myLevel, double myTeamLevel, double myAvgSquadLevel, double enemyAvgSquadLevel) {
            this.myLevel = myLevel;
            this.myTeamLevel = myTeamLevel;
            this.myAvgSquadLevel = myAvgSquadLevel;
            this.enemyAvgSquadLevel = enemyAvgSquadLevel;
        }

        RatingParams win(int rating) {
            result = PvpBattleResult.WINNER;
            playerDailyRating = rating;
            return this;
        }

        RatingParams loose(int rating) {
            result = PvpBattleResult.NOT_WINNER;
            playerDailyRating = rating;
            return this;
        }

        @Override
        public String toString() {
            return "{" +
                    "result=" + result +
                    ", myLevel=" + myLevel +
                    ", myTeamLevel=" + myTeamLevel +
                    ", rating=" + playerDailyRating +
                    "}: " + ratingPoints;
        }
    }
}
