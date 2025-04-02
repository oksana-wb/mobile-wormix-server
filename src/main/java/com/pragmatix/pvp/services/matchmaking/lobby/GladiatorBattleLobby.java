package com.pragmatix.pvp.services.matchmaking.lobby;

import com.pragmatix.arena.coliseum.ColiseumService;
import com.pragmatix.pvp.services.matchmaking.BattleProposal;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 11.10.13 15:59
 */
public class GladiatorBattleLobby extends WagerBattleLobby {

    @Value("${GladiatorBattleLobby.maxBattlesWithSameUser:1}")
    private int maxBattlesWithSameUser = 1;

    @Resource
    private ColiseumService coliseumService;

    @Override
    protected Set<BattleProposal> getBattleProposals(BattleProposal battleProposal) {
        if(coliseumService.isColiseumOpen()) {
            return super.getBattleProposals(battleProposal);
        } else {
            return new HashSet<>();
        }
    }

    @Override
    protected ProposalStat.MatchType isSameMassTeams(BattleProposal battleProposal, BattleProposal candidateTeam) {
//      http://jira.pragmatix-corp.com/browse/WORMIX-4224
//        Включить подбор на гладиаторской арене с учетом числа побед и поражений. Делим игроков на 3 группы:
//        1) 1-2 поражения при 0-2 победах
//        2) 0-4 победы (если не попали в первую группу)
//        3) 5-9 побед
//
//        Если от клиента приходит команда на увеличение диапазона поиска, то добавляем к возможным противникам следующую по старшинству группу
        byte[] userProgress = (byte[]) battleProposal.getAuxMatchParams();
        byte[] candidateProgress = (byte[]) candidateTeam.getAuxMatchParams();
        int userGroup = lobbyGroup(userProgress);
        int userWin = userProgress[0];
        int candidateGroup = lobbyGroup(candidateProgress);
        int candidateWin = candidateProgress[0];

        boolean result = false;
        if(userGroup == candidateGroup) {
            result = true;
        } else {
            int userGroupD = Math.min(userGroup + battleProposal.getGridQuality(), 3);
            int candidateGroupD = Math.min(candidateGroup + candidateTeam.getGridQuality(), 3);
            if(userGroup <= candidateGroup && candidateGroup <= userGroupD) {// кандидат мною "достижим"
                // если ждем одинаково или он ждет больше - он нам подходит безусловно
                // если кандидат ждет меньше - кандидат должен быть сильнее
                result = battleProposal.getGridQuality() <= candidateTeam.getGridQuality() || candidateWin >= userWin;
            } else if(candidateGroup <= userGroup && userGroup <= candidateGroupD) {// // я достижим для кандидата
                //инвертируем пред. условие
                result = candidateTeam.getGridQuality() <= battleProposal.getGridQuality() || userWin >= candidateWin;
            }
        }

        return result ? null : ProposalStat.MatchType.EXTRA;
    }

    public static int lobbyGroup(byte[] coliseumProgress) {
        int win = coliseumProgress[0];
        int defeat = coliseumProgress[2];
        if(win + defeat == 0 || win <= 2 && defeat >= 1 && defeat <= 2) {
            return 1;
        } else if(win <= 4) {
            return 2;
        } else {
            return 3;
        }
    }

    @Override
    protected boolean matchByRating(BattleProposal battleProposal, BattleProposal candidateProposal, double matchQuality) {
        return true;
    }

    @Override
    protected void candidateMatched(List<ImmutablePair<BattleProposal, Double>> candidatesByMatchQuality, BattleProposal battleProposal, BattleProposal candidateProposal, double matchQuality) {
        // сортировать потом будем по left в обратном порядке
        // для этого вместо matchQuality кладем в left разницу побед + 10
        byte[] userProgress = (byte[]) battleProposal.getAuxMatchParams();
        byte[] candidateProgress = (byte[]) candidateProposal.getAuxMatchParams();
        int userWin = userProgress[0];
        int candidateWin = candidateProgress[0];

        candidatesByMatchQuality.add(new ImmutablePair<>(candidateProposal, 10D - Math.abs(userWin - candidateWin)));
        battleProposal.getStat().matchedCandidats++;
    }

    @Override
    protected int getMaxBattlesWithSameUser() {
        return maxBattlesWithSameUser;
    }

}
