package com.pragmatix.pvp.services.matchmaking.lobby;

import com.pragmatix.performance.statictics.StatCollector;
import com.pragmatix.performance.statictics.ValueHolder;
import com.pragmatix.pvp.PvpBattleKey;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.matchmaking.BattleProposal;
import org.apache.mina.util.ConcurrentHashSet;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 11.10.13 13:07
 */
public class BossBattleLobby extends JSkillLobby {

    /**
     * профайлы игроков которые сделали подобную ставку
     */
    private ConcurrentMap<String, Set<BattleProposal>> proposalsMap = new ConcurrentHashMap<>();

    private StatCollector statCollector;

    @Override
    public void init(PvpBattleKey battleKey, StatCollector statCollector) {
        this.statCollector = statCollector;
    }

    @Override
    protected Set<BattleProposal> getBattleProposals(BattleProposal proposal) {
        short[] missionIds = proposal.getBattleBuffer().getMissionIds();
        long mapId = proposal.getBattleBuffer().getMapId();
        final String key = missionIds.length == 1 ? String.valueOf(missionIds[0]) : String.format("%s_%s#%s", missionIds[0], missionIds[1], mapId);
        Set<BattleProposal> battleProposalSet = proposalsMap.get(key);
        if(battleProposalSet == null) {
            battleProposalSet = new ConcurrentHashSet<>();
            Set<BattleProposal> putResult = proposalsMap.putIfAbsent(key, battleProposalSet);
            if(putResult == null){
                statCollector.needCollect("matchmaking.pve", key, new ValueHolder() {
                    @Override
                    public long getValue() {
                        return proposalsMap.get(key).size();
                    }
                }, false);
            }else{
                battleProposalSet = putResult;
            }
        }
        return battleProposalSet;
    }

    @Override
    protected boolean isSameClan(PvpUser user1, PvpUser user2) {
        return false;
    }
}
