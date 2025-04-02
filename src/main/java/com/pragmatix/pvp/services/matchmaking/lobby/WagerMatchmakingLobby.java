package com.pragmatix.pvp.services.matchmaking.lobby;

import com.pragmatix.performance.statictics.StatCollector;
import com.pragmatix.pvp.PvpBattleKey;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.services.matchmaking.BattleProposal;

import java.util.*;

/**
 * Хранилище заявок на бой
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.12.12 17:38
 */
public interface WagerMatchmakingLobby {

    void init(PvpBattleKey pvpBattleKey, StatCollector statCollector);

    boolean findCandidatesByWager(BattleProposal profile, int quantity, List<BattleProposal> candidates);

    boolean addBattleProposalByWager(BattleProposal profile, boolean unlock);

    boolean removeBattleProposalByWager(BattleProposal profile);

    <T extends BattleProposal> void playTurn(List<T> teams);

}
