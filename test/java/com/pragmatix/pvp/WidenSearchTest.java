package com.pragmatix.pvp;

import com.pragmatix.app.domain.TrueSkillEntity;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.pvp.messages.handshake.client.CancelBattle;
import com.pragmatix.pvp.messages.handshake.client.WidenSearch;
import com.pragmatix.pvp.messages.handshake.server.BattleCreated;
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNull;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 14.05.13 15:53
 */
public class WidenSearchTest extends AbstractSpringTest {

    private long user1Id = 58027749l;
    private long user2Id = 58027748l;
    private long user3Id = 58027747l;

    @Resource
    private LobbyConf lobbyConf;

    @Resource
    private ProfileService profileService;


    @Test
    public void test() throws Exception {
        lobbyConf.setSandboxBattlesDelimiter(0);
        lobbyConf.setBestMatchQuality(0.8);
        lobbyConf.setDeltaMatchQuality(0.2);

        setRating(user1Id, 1.0);
        setRating(user2Id, 0.79);
        // matchQlty = 0.67

        PvpParticipant pvpUser1 = new PvpParticipant(user1Id, binarySerializer);
        PvpParticipant pvpUser2 = new PvpParticipant(user2Id, binarySerializer);
        PvpParticipant pvpUser3 = new PvpParticipant(user3Id, binarySerializer);

        List<PvpParticipant> users = Arrays.asList(pvpUser1, pvpUser2);

        for(PvpParticipant user : users) {
            user.loginMain();
            user.connectToPvp();
        }

        for(PvpParticipant user : users) {
            user.sendCreateBattleRequest(BattleWager.WAGER_15_DUEL);
            Thread.sleep(100);
        }

        for(PvpParticipant user : users) {
            assertNull(user.reciveFromPvpNullable(BattleCreated.class, 300));
        }

        pvpUser1.sendToPvp(new WidenSearch());

        for(PvpParticipant user : users) {
            assertNull(user.reciveFromPvpNullable(BattleCreated.class, 300));
        }

        pvpUser2.sendToPvp(new WidenSearch());

        for(PvpParticipant user : users) {
            BattleCreated battleCreated = user.reciveFromPvp(BattleCreated.class, 300);
            user.sendToPvp(new CancelBattle(battleCreated.getBattleId()));
        }

    }

    private void setRating(long user1Id1, double mean) {
        UserProfile profile1 = getProfile(user1Id1);
        TrueSkillEntity trueSkillEntity = profileService.getTrueSkillFor(profile1);
        trueSkillEntity.setMean(mean);
        trueSkillEntity.setStandardDeviation(0.0001);
    }
}
