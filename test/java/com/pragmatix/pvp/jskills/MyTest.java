package com.pragmatix.pvp.jskills;

import com.pragmatix.pvp.PvpPlayer;
import com.pragmatix.pvp.TeamBattleResult;
import com.pragmatix.testcase.AbstractTest;
import jskills.*;
import jskills.trueskill.TwoPlayerTrueSkillCalculator;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 09.04.13 12:27
 */
public class MyTest extends AbstractTest {

    public static final int BATTLES = 1;

    private TwoPlayerTrueSkillCalculator calculator;

    @Before
    public void setup() {
        calculator = new TwoPlayerTrueSkillCalculator();
    }

    private static final double defaultInitialMean = 1.0;
    private static final double defaultInitialStandardDeviation = defaultInitialMean / 3.0;
    private static final double defaultBeta = defaultInitialMean / 6.0;
    private static final double defaultDynamicsFactor = defaultInitialMean / 300.0;
    private static final double defaultDrawProbability = 0.10;

    @Test
    public void test() {
        PvpPlayer player1 = new PvpPlayer(1l);
        PvpPlayer player2 = new PvpPlayer(2l);
        PvpPlayer player3 = new PvpPlayer(3l);
        GameInfo gameInfo = new GameInfo(defaultInitialMean, defaultInitialStandardDeviation, defaultBeta, defaultDynamicsFactor, defaultDrawProbability);

        Map<IPlayer, Rating> ratingMap = new HashMap<IPlayer, Rating>();
        ratingMap.put(player1, gameInfo.getDefaultRating());
        ratingMap.put(player2, gameInfo.getDefaultRating());
        ratingMap.put(player3, gameInfo.getDefaultRating());

        System.out.println("ratingMap=" + ratingMap);

        long start = System.currentTimeMillis();
        for(int i = 0; i < BATTLES; i++) {
            playGame(player1, player2, player3, gameInfo, ratingMap);
            System.out.println("ratingMap=" + ratingMap);
        }
        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    public void testEtalon() {
        Player<Integer> player1 = new Player<Integer>(1);
        Player<Integer> player2 = new Player<Integer>(2);
        GameInfo gameInfo = new GameInfo(defaultInitialMean, defaultInitialStandardDeviation, defaultBeta, defaultDynamicsFactor, defaultDrawProbability);

        Map<IPlayer, Rating> ratingMap = new HashMap<IPlayer, Rating>();
        ratingMap.put(player1, gameInfo.getDefaultRating());
        ratingMap.put(player2, gameInfo.getDefaultRating());

        System.out.println("ratingMap=" + ratingMap);

        long start = System.currentTimeMillis();
        for(int i = 0; i < BATTLES; i++) {
            playGame(player1, player2, gameInfo, ratingMap);
        }
        System.out.println("ratingMap=" + ratingMap);
        System.out.println(System.currentTimeMillis() - start);
    }

    private void playGame(PvpPlayer player1, PvpPlayer player2, PvpPlayer player3, GameInfo gameInfo, Map<IPlayer, Rating> ratingMap) {
        TeamBattleResult team1 = new TeamBattleResult(player1, ratingMap.get(player1));
        TeamBattleResult team2 = new TeamBattleResult(player2, ratingMap.get(player2));
        TeamBattleResult team3 = new TeamBattleResult(player3, ratingMap.get(player3));
        Collection<? extends ITeam> teams = Team.concat(team1, team2, team3);

        Map<IPlayer, Rating> newRatings = TrueSkillCalculator.calculateNewRatings(gameInfo, teams, 1, 2, 3);

        ratingMap.put(player1, newRatings.get(player1));
        ratingMap.put(player2, newRatings.get(player2));
        ratingMap.put(player3, newRatings.get(player3));
    }

    private void playGame(Player<Integer> player1, Player<Integer> player2, GameInfo gameInfo, Map<IPlayer, Rating> ratingMap) {
        Team team1 = new Team(player1, ratingMap.get(player1));
        Team team2 = new Team(player2, ratingMap.get(player2));
        Collection<? extends ITeam> teams = Team.concat(team1, team2);

        Map<IPlayer, Rating> newRatings = TrueSkillCalculator.calculateNewRatings(gameInfo, teams, 1, 2);

        ratingMap.put(player1, newRatings.get(player1));
        ratingMap.put(player2, newRatings.get(player2));
    }

}
