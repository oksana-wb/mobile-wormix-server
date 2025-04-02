package com.pragmatix.pvp.services;

import com.pragmatix.app.services.rating.RankService;
import com.pragmatix.pvp.PvpBattleKey;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 01.06.2018 15:07
 */
public class SteamRatingFormulaTest extends AbstractSpringTest {

    @Resource
    RankService rankService;

    @Test
    public void test() {

        PvpBattleKey pvpBattleKey = PvpBattleKey.WAGER_PvP_DUEL_15;
        int enemyRank = 5, myRank = 5;
        int teamSize = 4;

        for(int myLevel = 1; myLevel < 100; myLevel++) {
            for(int enemyLevel = 1; enemyLevel < 100; enemyLevel++) {
                int ratingPoints = rankService.getVictoryRatingPoints(teamSize, enemyRank, enemyLevel, myRank, myLevel, pvpBattleKey);
                System.out.println(myLevel + " win " + enemyLevel + " => " + ratingPoints);
            }
            System.out.println("");
        }
    }

}