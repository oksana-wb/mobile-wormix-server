package com.pragmatix.pvp.services;

import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.matchmaking.BattleProposal;
import com.pragmatix.pvp.services.matchmaking.TeamBattleProposal;
import com.pragmatix.pvp.services.matchmaking.WagerMatchmakingService;
import com.pragmatix.pvp.services.matchmaking.lobby.JSkillLobby;
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf;
import com.pragmatix.sessions.AppServerAddress;
import com.pragmatix.sessions.IAppServer;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 02.07.13 14:38
 */
public class PvpDailyRegistryTest extends AbstractSpringTest {

    @Resource
    private JSkillLobby jSkillLobby;

    @Resource
    private LobbyConf lobbyConf;

    @Resource
    private PvpDailyRegistry dailyRegistry;

    @Resource
    private WagerMatchmakingService wagerMatchmakingService;

    @Test
    public void testMayMatchedUsers() throws Exception {

        PvpUser user1 = newPvpUser(1);
        PvpUser user2 = newPvpUser(2);
        BattleProposal proposal1 = newTeamBattleProposal(user1);
        BattleProposal proposal2 = newTeamBattleProposal(user2);

        for(int i = 0; i < lobbyConf.getMaxBattlesWithSameUser(); i++) {
            assertTrue(jSkillLobby.mayMatched(proposal1, proposal2));
            wagerMatchmakingService.incBattlesCount(asParticipants(user1, user2));
            System.out.println((i + 1) + ":" + dailyRegistry.getStore());
        }

        assertFalse(jSkillLobby.mayMatched(proposal1, proposal2));

        dailyRegistry.getDailyTask().runServiceTask();

        assertTrue(jSkillLobby.mayMatched(proposal1, proposal2));

    }

    private TeamBattleProposal newTeamBattleProposal(PvpUser ... users){
        TeamBattleProposal teamBattleProposal = newTeamBattleProposal();
        teamBattleProposal.setTeam(users);
        return teamBattleProposal;
    }
    
    private List<BattleParticipant> asParticipants(PvpUser... users) {
        List<BattleParticipant> result = new ArrayList<>();
        for(PvpUser user : users) {
            result.add(new BattleParticipantMock(user));
        }
        return result;
    }

    @Test
    public void testMayMatchedTeams() throws Exception {
        PvpUser user1 = newPvpUser(1);
        PvpUser user2 = newPvpUser(2);
        PvpUser user3 = newPvpUser(3);
        PvpUser user4 = newPvpUser(4);
        BattleProposal proposal1 = newTeamBattleProposal(user1, user2);
        BattleProposal proposal2 = newTeamBattleProposal(user3, user4);

        for(int i = 0; i < lobbyConf.getMaxBattlesWithSameUser(); i++) {
            assertTrue(jSkillLobby.mayMatched(proposal1, proposal2));
            wagerMatchmakingService.incBattlesCount(asParticipants(user1, user2, user3, user4));
            System.out.println((i + 1) + ":" + dailyRegistry.getStore());
        }

        assertFalse(jSkillLobby.mayMatched(proposal1, proposal2));

        proposal1 = newTeamBattleProposal(user1, user3);
        proposal2 = newTeamBattleProposal(user2, user4);

        assertFalse(jSkillLobby.mayMatched(proposal1, proposal2));

        proposal1 = newTeamBattleProposal(user1, user3);
        proposal2 = newTeamBattleProposal(user2, newPvpUser(5));

        assertFalse(jSkillLobby.mayMatched(proposal1, proposal2));

        proposal1 = newTeamBattleProposal(user1, user4);
        proposal2 = newTeamBattleProposal(newPvpUser(5), newPvpUser(6));

        assertTrue(jSkillLobby.mayMatched(proposal1, proposal2));

        dailyRegistry.getDailyTask().runServiceTask();
    }

    private PvpUser newPvpUser(int profileId) {
        return new PvpUser(profileId, "profileId", (byte) 1, PvpDailyRegistryTest.this.getMainServer());
    }

    class BattleParticipantMock extends BattleParticipant {
        public BattleParticipantMock(PvpUser user) {
            super(user.getProfileId(), (byte) 1, State.acceptCommand, 0, 0, PvpDailyRegistryTest.this.getMainServer());
        }
    }

    @NotNull
    public IAppServer getMainServer() {
        return new AppServerAddress("main");
    }
}
