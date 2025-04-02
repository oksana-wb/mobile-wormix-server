package com.pragmatix.pvp;

import com.pragmatix.app.services.rating.RatingService;
import com.pragmatix.pvp.dsl.WagerDuelBattle;
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.07.13 14:47
 */
public class RestrictLobbyTest extends PvpAbstractTest {

    long user1Id = 58027749l;
    long user2Id = 58027748l;
    long user3Id = 58027747l;
    long user4Id = 58027746l;

    @Resource
    private LobbyConf lobbyConf;

    @Resource
    private RatingService ratingService;

    @Before
    public void startBattle() throws InterruptedException {
        turnOffLobbyRestrict();
    }

    @Test
    public void test2x2() throws Exception {
        Wager2x2Test.Wager2x2Battle wager2x2Battle = new Wager2x2Test.Wager2x2Battle(binarySerializer);

        wager2x2Battle.startBattle(user1Id, user2Id, user3Id, user4Id);
        wager2x2Battle.finishBattle();

        wager2x2Battle.failureStartBattle(user1Id, user2Id, user3Id, user4Id);
    }

    @Test
    public void test2x2Friends() throws Exception {
        Wager2x2FriendsTest.Wager2x2FriendsBattle wager2x2FriendsBattle = new Wager2x2FriendsTest.Wager2x2FriendsBattle(binarySerializer);

        wager2x2FriendsBattle.startBattle(user1Id, user2Id, user3Id, user4Id);
        wager2x2FriendsBattle.finishBattle();

        wager2x2FriendsBattle.failureStartBattle(user1Id, user2Id, user3Id, user4Id);
    }

    @Test
    public void test1x1x1() throws Exception {
        Wager3ForAllPvpTest.Wager3ForAllBattle wager3ForAllBattle = new Wager3ForAllPvpTest.Wager3ForAllBattle(binarySerializer);

        wager3ForAllBattle.startBattle(user1Id, user2Id, user3Id);
        wager3ForAllBattle.finishBattle();

        wager3ForAllBattle.failureStartBattle(user1Id, user2Id, user3Id);
        wager3ForAllBattle.disconnectFromPvp();

        wager3ForAllBattle.failureStartBattle(user1Id, user2Id, user4Id);
        wager3ForAllBattle.disconnectFromPvp();
    }

    @Test
    public void testDuel() throws Exception {
        WagerDuelBattle wagerDuelBattle = new WagerDuelBattle(binarySerializer);

        lobbyConf.setCheckOpponents(true);
        lobbyConf.setCheckIp(false);

        wagerDuelBattle.startBattle(user1Id, user2Id);
        wagerDuelBattle.finishBattle();
        Thread.sleep(1000);
        System.out.println("===============================================================");

//        wagerDuelBattle.failureStartBattle(user1Id, user2Id);
//        wagerDuelBattle.disconnectFromPvp();
//        Thread.sleep(1000);
//        System.out.println("===============================================================");
//
//        wagerDuelBattle.startBattle(user1Id, user3Id);
//        wagerDuelBattle.finishBattle();
    }

}
