package com.pragmatix.pvp.services.matchmaking.lobby

import com.pragmatix.app.services.TrueSkillService
import com.pragmatix.arena.coliseum.ColiseumService
import com.pragmatix.pvp.BattleWager
import com.pragmatix.pvp.services.matchmaking.BattleProposal
import com.pragmatix.pvp.services.matchmaking.TestBattleProposal
import org.junit.Assert
import org.junit.Test

import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.11.2015 14:48
 */
class GladiatorBattleLobbyTest {

    @Test
    public void test() {
        def coliseumService = mock(ColiseumService.class);
        when(coliseumService.isColiseumOpen()).thenReturn(true)

        def trueSkillService = mock(TrueSkillService.class)
        when(trueSkillService.calculateMatchQuality(any() as BattleProposal, any() as BattleProposal)).thenReturn(0D)

        def lobbyConf = new LobbyConf()
        lobbyConf.checkLastOpponent = false

        def lobby = new GladiatorBattleLobby()
        lobby.coliseumService = coliseumService
        lobby.lobbyConf = lobbyConf
        lobby.trueSkillService = trueSkillService

        def battleProposal0 = newBattleProposal(10, 5, 0, 0)
        def battleProposal1 = newBattleProposal(11, 8, 0, 1)
        def battleProposal2 = newBattleProposal(12, 9, 0, 0)

        List<BattleProposal> candidats = new ArrayList<>(1)
        lobby.addBattleProposalByWager(battleProposal0, true)
        lobby.addBattleProposalByWager(battleProposal1, true)
        lobby.findCandidatesByWager(battleProposal2, 1, candidats)

        Assert.assertEquals(1, candidats.size())
    }

    def newBattleProposal(int profileId, int win, int defeat, int gridQuality) {
        def proposal = new TestBattleProposal()
        proposal.profileId = profileId
        proposal.coliseumProgress = [win, 0, defeat] as byte[]
        proposal.wager = BattleWager.GLADIATOR_DUEL
        proposal.gridQuality = gridQuality

        proposal
    }

}