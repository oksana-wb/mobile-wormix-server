package com.pragmatix.pvp.services.matchmaking.lobby;

import com.pragmatix.performance.statictics.StatCollector;
import com.pragmatix.performance.statictics.ValueHolder;
import com.pragmatix.pvp.PvpBattleKey;
import com.pragmatix.pvp.services.matchmaking.BattleProposal;
import org.apache.mina.util.ConcurrentHashSet;

import java.util.Set;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 11.10.13 15:59
 */
public class WagerBattleLobby extends JSkillLobby {
    /**
     * профайлы игроков которые сделали подобную ставку
     */
    private Set<BattleProposal> proposalsByWager =  new ConcurrentHashSet<>();

    @Override
    public void init(PvpBattleKey battleKey, StatCollector statCollector) {
        statCollector.needCollect("matchmaking.pvp", battleKey.name(), new ValueHolder() {
            @Override
            public long getValue() {
                return proposalsByWager.size();
            }
        }, false);
    }

    protected Set<BattleProposal> getBattleProposals(BattleProposal battleProposal) {
        return proposalsByWager;
    }

}