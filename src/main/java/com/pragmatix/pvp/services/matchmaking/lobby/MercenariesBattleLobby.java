package com.pragmatix.pvp.services.matchmaking.lobby;

import com.pragmatix.pvp.services.matchmaking.BattleProposal;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 11.10.13 15:59
 */
public class MercenariesBattleLobby extends WagerBattleLobby {

    @Value("${MercenariesBattleLobby.maxBattlesWithSameUser:1}")
    private int maxBattlesWithSameUser = 1;

    @Resource
    private LobbyConf lobbyConf;

    @Override
    protected ProposalStat.MatchType isSameMassTeams(BattleProposal battleProposal, BattleProposal candidateTeam) {
        int[] userStatus = (int[]) battleProposal.getAuxMatchParams();
        int[] candidateProgress = (int[]) candidateTeam.getAuxMatchParams();
        int userGroup = lobbyGroup(userStatus);
        int candidateGroup = lobbyGroup(candidateProgress);

        return userGroup == candidateGroup ? null : ProposalStat.MatchType.EXTRA;
    }

    public int lobbyGroup(int[] status) {
        // подбор по проценту побед (сначала можно делить игроков на группы с процентом <=50 и >50)
        int total_win = status[0];
        int total_draw = status[1];
        int total_defeat = status[2];
        int total = total_win + total_draw + total_defeat;

        return total == 0 || total_win * 100 / total <= lobbyConf.getMercenariesBattleLobbyWinPercent() ? 1 : 2;
    }

    @Override
    protected boolean matchByRating(BattleProposal battleProposal, BattleProposal candidateProposal, double matchQuality) {
        return true;
    }

    @Override
    protected int getMaxBattlesWithSameUser() {
        return maxBattlesWithSameUser;
    }

}
