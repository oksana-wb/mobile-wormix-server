package com.pragmatix.pvp.services.matchmaking.lobby;

import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.matchmaking.BattleProposal;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 11.10.13 15:59
 */
public class QuestBattleLobby extends WagerBattleLobby {

    @Value("${QuestBattleLobby.maxBattlesWithSameUser:1}")
    private int maxBattlesWithSameUser = 1;

    @Override
    protected ProposalStat.MatchType isSameMassTeams(BattleProposal battleProposal, BattleProposal candidateTeam) {
        return null;
    }

    @Override
    protected boolean matchByRating(BattleProposal battleProposal, BattleProposal candidateProposal, double matchQuality) {
        return true;
    }

    @Override
    protected int getMaxBattlesWithSameUser() {
        return maxBattlesWithSameUser;
    }

    @Override
    protected boolean isSameClan(PvpUser user1, PvpUser user2) {
        return false;
    }
}
