package com.pragmatix.pvp;

import com.pragmatix.app.common.BattleState;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.pvp.messages.handshake.server.BattleCreated;
import com.pragmatix.pvp.services.battletracking.PvpBattleTrackerService;
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.12.12 11:04
 */
public class CreateBattleTest extends AbstractSpringTest {

    private long user1Id = 58027749l;
    private long user2Id = 58027748l;
    private long user3Id = 58027747l;
    private long user4Id = 58027746l;

    @Resource
    private ProfileService profileService;

    @Resource
    private PvpBattleTrackerService pvpBattleTrackerService;

    @Resource
    private LobbyConf lobbyConf;

    @Value("${connection.main.port}")
    int mainPort;

    @Value("${connection.pvp.port}")
    int pvpPort;

    @Test
    public void mobileLevelTest() throws Exception {
        /*
        pvp.sandboxRatingDelimeter=0
        pvp.useLevelDiffFactor=false
        pvp.enemyLevelRange=30
        pvp.hpDiffFactor=20
        pvp.bestMatchQuality=0
         */
        lobbyConf.setSandboxBattlesDelimiter(0);
        lobbyConf.setUseLevelDiffFactor(false);
        lobbyConf.setEnemyLevelRange(30);
        lobbyConf.setHpDiffFactor(20);
        lobbyConf.setMaxHpDiffFactor(20);
        lobbyConf.setBestMatchQuality(0);

        final PvpParticipant participant1 = new PvpParticipant(user1Id, binarySerializer);
        final PvpParticipant participant2 = new PvpParticipant(user2Id, binarySerializer);

        List<PvpParticipant> pvpParticipants = Arrays.asList(participant1, participant2);
        for(PvpParticipant user : pvpParticipants) {
            user.loginMain();
            user.connectToPvp();
//            user.loginMain(mainPort);
//            user.connectToPvp(pvpPort);
        }

        UserProfile profile1 = profileService.getUserProfile(user1Id);
        UserProfile profile2 = profileService.getUserProfile(user2Id);

        profile1.setLevel(20);
        profile2.setLevel(11);

        for(PvpParticipant user : pvpParticipants) {
            user.sendCreateBattleRequest(BattleWager.WAGER_50_DUEL);
            Thread.sleep(100);
        }

        for(PvpParticipant user : pvpParticipants) {
            user.reciveFromPvp(BattleCreated.class, 100);
        }

//        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void bigLevelDiffTestFailure() throws Exception {
        lobbyConf.setUseLevelDiffFactor(false);
        lobbyConf.setEnemyLevelRange(2);

        lobbyConf.setHpDiffFactor(100);
        lobbyConf.setMaxHpDiffFactor(100);
        lobbyConf.setBestMatchQuality(0);
        lobbyConf.setSandboxBattlesDelimiter(0);

        final PvpParticipant participant1 = new PvpParticipant(user1Id, binarySerializer);
        final PvpParticipant participant2 = new PvpParticipant(user2Id, binarySerializer);
        final PvpParticipant participant3 = new PvpParticipant(user3Id, binarySerializer);

        List<PvpParticipant> pvpParticipants = Arrays.asList(participant1, participant2, participant3);
        for(PvpParticipant user : pvpParticipants) {
            user.loginMain();
            user.connectToPvp();
        }

        UserProfile profile1 = profileService.getUserProfile(user1Id);
        UserProfile profile2 = profileService.getUserProfile(user2Id);
        UserProfile profile3 = profileService.getUserProfile(user3Id);

        profile1.setLevel(8);
        profile2.setLevel(12);
        profile3.setLevel(10);

        for(PvpParticipant user : pvpParticipants) {
            user.sendCreateBattleRequest(BattleWager.WAGER_50_3_FOR_ALL);
            Thread.sleep(100);
        }

        for(PvpParticipant user : pvpParticipants) {
            BattleCreated battleCreated = user.reciveFromPvpNullable(BattleCreated.class, 100);
            assertNull(battleCreated);
        }

//        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void bigLevelDiffTestSuccess() throws Exception {
        lobbyConf.setUseLevelDiffFactor(false);
        lobbyConf.setEnemyLevelRange(2);

        lobbyConf.setHpDiffFactor(100);
        lobbyConf.setMaxHpDiffFactor(100);
        lobbyConf.setBestMatchQuality(0);
        lobbyConf.setSandboxBattlesDelimiter(0);

        final PvpParticipant participant1 = new PvpParticipant(user1Id, binarySerializer);
        final PvpParticipant participant2 = new PvpParticipant(user2Id, binarySerializer);
        final PvpParticipant participant3 = new PvpParticipant(user3Id, binarySerializer);

        List<PvpParticipant> pvpParticipants = Arrays.asList(participant1, participant2, participant3);
        for(PvpParticipant user : pvpParticipants) {
            user.loginMain();
            user.connectToPvp();
        }

        UserProfile profile1 = profileService.getUserProfile(user1Id);
        UserProfile profile2 = profileService.getUserProfile(user2Id);
        UserProfile profile3 = profileService.getUserProfile(user3Id);

        profile1.setLevel(8);
        profile2.setLevel(9);
        profile3.setLevel(10);

        for(PvpParticipant user : pvpParticipants) {
            user.sendCreateBattleRequest(BattleWager.WAGER_50_3_FOR_ALL);
            Thread.sleep(100);
        }

        for(PvpParticipant user : pvpParticipants) {
            BattleCreated battleCreated = user.reciveFromPvpNullable(BattleCreated.class, 100);
            assertNotNull(battleCreated);
        }

//        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void test_0() throws Exception {
        final PvpParticipant participant1 = new PvpParticipant(user1Id, binarySerializer);
        final PvpParticipant participant2 = new PvpParticipant(user2Id, binarySerializer);
        final PvpParticipant participant3 = new PvpParticipant(user3Id, binarySerializer);

        for(PvpParticipant user : Arrays.asList(participant1, participant2, participant3)) {
            user.loginMain();
            user.connectToPvp();
        }

        UserProfile profile1 = profileService.getUserProfile(user1Id);
        UserProfile profile2 = profileService.getUserProfile(user2Id);
        UserProfile profile3 = profileService.getUserProfile(user3Id);

        profile1.setLevel(4);
        profile2.setLevel(14);
        profile3.setLevel(4);

        for(PvpParticipant user : Arrays.asList(participant1, participant2, participant3)) {
            user.sendCreateBattleRequest(BattleWager.WAGER_15_DUEL);
            Thread.sleep(1000);
        }

        for(PvpParticipant user : Arrays.asList(participant1, participant2, participant3)) {
            user.sendCancelBattle();
        }

        Thread.sleep(10000);
    }

    /**
     * Тест на "перебивание" заявки
     */
    @Test
    public void test_2_1() throws Exception {
        final PvpParticipant participant1 = new PvpParticipant(user1Id, binarySerializer);
        final PvpParticipant participant2 = new PvpParticipant(user2Id, binarySerializer);
        final PvpParticipant participant3 = new PvpParticipant(user3Id, binarySerializer);

        for(PvpParticipant user : Arrays.asList(participant1, participant2, participant3)) {
            user.loginMain();
            user.connectToPvp();
        }

        UserProfile profile1 = profileService.getUserProfile(user1Id);
        UserProfile profile2 = profileService.getUserProfile(user2Id);
        UserProfile profile3 = profileService.getUserProfile(user3Id);

        participant1.sendCreateFriendBattleRequest(new long[]{user1Id, user2Id}, new byte[]{0, 1});
        Thread.sleep(100);
        assertEquals(BattleState.WAIT_START_BATTLE, profile1.getBattleState());
        assertEquals(BattleState.WAIT_START_BATTLE, profile2.getBattleState());

        participant2.sendCreateBattleRequest(BattleWager.WAGER_15_DUEL);
        Thread.sleep(100);
        participant3.sendCreateBattleRequest(BattleWager.WAGER_15_DUEL);

        Thread.sleep(1000);

        assertEquals(BattleState.NOT_IN_BATTLE, profile1.getBattleState());
        assertEquals(BattleState.WAIT_START_BATTLE, profile2.getBattleState());
        assertEquals(BattleState.WAIT_START_BATTLE, profile3.getBattleState());

    }

    @Test
    public void test_1() throws Exception {
        PvpParticipant participant1 = new PvpParticipant(user1Id, binarySerializer);
        PvpParticipant participant2 = new PvpParticipant(user2Id, binarySerializer);
        PvpParticipant participant3 = new PvpParticipant(user3Id, binarySerializer);
        PvpParticipant participant4 = new PvpParticipant(user4Id, binarySerializer);

        participant1.loginMain();
        participant3.loginMain();
        participant4.loginMain();

        participant1.connectToPvp();
        participant1.sendCreateFriendBattleRequest(new long[]{user1Id, user2Id, user3Id, user4Id}, new byte[]{0, 1, 2, 3});

        Thread.sleep(1000);

        UserProfile profile1 = profileService.getUserProfile(user1Id);
        UserProfile profile3 = profileService.getUserProfile(user3Id);
        UserProfile profile4 = profileService.getUserProfile(user4Id);

        assertEquals(BattleState.NOT_IN_BATTLE, profile1.getBattleState());
        assertEquals(BattleState.NOT_IN_BATTLE, profile3.getBattleState());
        assertEquals(BattleState.NOT_IN_BATTLE, profile4.getBattleState());

    }

    @Test
    public void test_2() throws Exception {
        PvpParticipant participant1 = new PvpParticipant(user1Id, binarySerializer);
        PvpParticipant participant2 = new PvpParticipant(user2Id, binarySerializer);
        PvpParticipant participant3 = new PvpParticipant(user3Id, binarySerializer);
        PvpParticipant participant4 = new PvpParticipant(user4Id, binarySerializer);

        participant1.loginMain();
        participant2.loginMain();
        participant3.loginMain();

        participant1.connectToPvp();
        participant1.sendCreateFriendBattleRequest(new long[]{user1Id, user2Id, user3Id, user4Id}, new byte[]{0, 1, 2, 3});

        Thread.sleep(1000);

        UserProfile profile1 = profileService.getUserProfile(user1Id);
        UserProfile profile2 = profileService.getUserProfile(user3Id);
        UserProfile profile3 = profileService.getUserProfile(user3Id);

        assertEquals(BattleState.NOT_IN_BATTLE, profile1.getBattleState());
        assertEquals(BattleState.NOT_IN_BATTLE, profile2.getBattleState());
        assertEquals(BattleState.NOT_IN_BATTLE, profile3.getBattleState());

    }

    @Test
    public void test_3() throws Exception {
        PvpParticipant participant1 = new PvpParticipant(user1Id, binarySerializer);
        PvpParticipant participant2 = new PvpParticipant(user2Id, binarySerializer);
        PvpParticipant participant4 = new PvpParticipant(user4Id, binarySerializer);

        for(PvpParticipant user : Arrays.asList(participant1, participant2, participant4)) {
            user.loginMain();
        }

        participant4.connectToPvp();
        participant4.sendCreateBattleRequest(BattleWager.WAGER_15_DUEL);

        participant1.connectToPvp();
        participant1.sendCreateFriendBattleRequest(new long[]{user1Id, user2Id, user3Id, user4Id}, new byte[]{0, 1, 2, 3});

        Thread.sleep(1000);

        UserProfile profile1 = profileService.getUserProfile(user1Id);
        UserProfile profile2 = profileService.getUserProfile(user3Id);
        UserProfile profile4 = profileService.getUserProfile(user4Id);

        assertEquals(BattleState.NOT_IN_BATTLE, profile1.getBattleState());
        assertEquals(BattleState.NOT_IN_BATTLE, profile2.getBattleState());
        assertEquals(BattleState.WAIT_START_BATTLE, profile4.getBattleState());

    }

}
