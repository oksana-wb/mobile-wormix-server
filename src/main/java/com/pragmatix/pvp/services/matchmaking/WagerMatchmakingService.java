package com.pragmatix.pvp.services.matchmaking;

import com.pragmatix.gameapp.IGameApp;
import com.pragmatix.performance.statictics.StatCollector;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleKey;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.BattleFactory;
import com.pragmatix.pvp.services.PvpDailyRegistry;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.battletracking.PvpBattleStateEnum;
import com.pragmatix.pvp.services.matchmaking.lobby.WagerMatchmakingLobby;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.05.12 15:19
 */

public class WagerMatchmakingService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private GroupHpService groupHpService;

    @Resource
    private BattleFactory battleFactory;

    @Resource
    private PvpService pvpService;

    private Map<PvpBattleKey, WagerMatchmakingLobby> wagerMatchmakingLobbyMap;

    @Resource
    private PvpDailyRegistry dailyRegistry;

    @Resource
    private IGameApp gameApp;

    private final Object lock = new Object();

    public void init() throws ClassNotFoundException, InterruptedException {
        StatCollector statCollector = gameApp.getStatCollector();
        for(Map.Entry<PvpBattleKey, WagerMatchmakingLobby> entry : wagerMatchmakingLobbyMap.entrySet()) {
            entry.getValue().init(entry.getKey(), statCollector);
        }
    }

    public boolean leaveLobby(BattleBuffer battle) {
        BattleProposal battleProposal = battle.getBattleProposal();
        // бою еще не была сопоставлена заявка
        if(battleProposal == null) {
            return false;
        }
        WagerMatchmakingLobby matchmakingLobby = getWagerMatchmakingLobbyFor(battle.getBattleType(), battle.getWager());
        synchronized (lock) {
            return matchmakingLobby.removeBattleProposalByWager(battleProposal);
        }
    }

    public boolean matchmake(BattleBuffer battle) {
        byte[] units = GroupMassService.getUnitsOf(battle);
        BattleWager wager = battle.getWager();
        BattleProposal battleProposal;
        if(battle.getParticipants().size() == 1) {
            BattleParticipant participant = battle.getParticipants().get(0);
            participant.setGroupHp(groupHpService.calculateGroupHp(participant, battle.getBattleType()));
            battleProposal = new UserBattleProposal(battle.getLock(), battle.getParticipants().get(0), wager, units, battle, pvpService);
        } else if(battle.getParticipants().size() == 2) {
            for(BattleParticipant participant : battle.getParticipants()) {
                participant.setGroupHp(groupHpService.calculateGroupHp(participant, battle.getBattleType()));
            }
            battleProposal = new TeamBattleProposal(battle.getParticipants(), wager, units, battle, pvpService);
        } else {
            throw new IllegalStateException("количество участников в заявке не может быть > 2");
        }
        // связываем бой с заявкой. используется для отзыва заявки при дисконнекте
        battle.setBattleProposal(battleProposal);

        return matchmake(battle, battleProposal);
    }

    public boolean matchmake(final BattleBuffer battle, BattleProposal battleProposal) {
        WagerMatchmakingLobby matchmakingLobby = getWagerMatchmakingLobbyFor(battle.getBattleType(), battle.getWager());
        synchronized (lock) {
            if(battleProposal.tryLock()) {
                int needParticipants = battle.getBattleType().getNeedParticipants();
                // подбираем участников
                int quantity = needParticipants + 1 - battleProposal.getSize();
                List<BattleProposal> candidats = new ArrayList<>(quantity);
                if(!matchmakingLobby.findCandidatesByWager(battleProposal, quantity, candidats)) {
                    fail(matchmakingLobby, battleProposal, candidats);
                    return false;
                }

                // доп проверка что все по прежнему онлайн и не отменили заявку
                if(battleProposal.inValidState() && allInValidState(candidats)) {
                    // переносим в бой подобранных участников
                    battleFactory.mergeParticipantsIntoWagerBattle(battleProposal, candidats, matchmakingLobby);
                    // инвалидируем бои подобранных участников
                    for(BattleProposal candidate : candidats) {
                        BattleBuffer candidatBattleBuffer = candidate.getBattleBuffer();
                        log.info("battleId={} setBattleState(DropBattle)", candidatBattleBuffer.getBattleId());
                        candidatBattleBuffer.setBattleState(PvpBattleStateEnum.DropBattle);
                    }
                    //запоминаем подобранных оппонентов (после начала боя)
                    /**
                     * {@link com.pragmatix.pvp.services.battletracking.handlers.StartHandler#onStartBattle(com.pragmatix.pvp.model.BattleBuffer)}
                     */
                    battle.setOnStartBattle(() -> incBattlesCount(battle.getParticipants()));

                    return true;
                } else {
                    fail(matchmakingLobby, battleProposal, candidats);
                }
                return false;
            } else {
                if(!battle.inState(PvpBattleStateEnum.DropBattle)) {
                    // если бой не был подобран ранее, кладем себя на поиск, но блокировку не снимаем
                    matchmakingLobby.addBattleProposalByWager(battleProposal, false);
                }
                return false;
            }
        }
    }

    public void incBattlesCount(List<BattleParticipant> participants) {
        for(BattleParticipant participant : participants) {
            if(participant.isEnvParticipant()) {
                continue;
            }
            PvpUser user = pvpService.getUser(participant.getPvpUserId());
            user.cleanLastMatchmakingOpponents();
        }
        for(BattleParticipant participant1 : participants) {
            if(participant1.isEnvParticipant()) {
                continue;
            }
            for(BattleParticipant participant2 : participants) {
                if(participant2.isEnvParticipant()) {
                    continue;
                }
                // не увеличиваем количество боёв между членами одной команды
                if(participant1.getPlayerTeam() != participant2.getPlayerTeam()) {
                    incBattleCount(participant1.getPvpUserId(), participant2.getPvpUserId());
                }
            }
        }
    }

    private void incBattleCount(Long pvpUserId1, Long pvpUserId2) {
        dailyRegistry.incBattlesCount(pvpUserId1, pvpUserId2);

        PvpUser pvpUser1 = pvpService.getUser(pvpUserId1);
        pvpUser1.addLastMatchmakingOpponent(pvpUserId2);
    }

    private void fail(WagerMatchmakingLobby matchmakingLobby, BattleProposal author, List<BattleProposal> candidats) {
        //todo сброс статистики заявки
        // подобрать не удалось
        for(BattleProposal foundCandidat : candidats) {
            // возвращаем его обратно для поиска
            matchmakingLobby.addBattleProposalByWager(foundCandidat, true);
        }
        // кладем себя на поиск
        matchmakingLobby.addBattleProposalByWager(author, true);
    }

    private boolean allInValidState(List<BattleProposal> candidates) {
        for(BattleProposal candidate : candidates) {
            if(!candidate.inValidState()) {
                return false;
            }
        }
        return true;
    }

    public WagerMatchmakingLobby getWagerMatchmakingLobbyFor(PvpBattleType battleType, BattleWager battleWager) {
        PvpBattleKey pvpBattleKey = PvpBattleKey.valueOf(battleType, battleWager);
        WagerMatchmakingLobby result = wagerMatchmakingLobbyMap.get(pvpBattleKey);
        if(result == null)
            throw new IllegalStateException("не найнено WagerMatchmakingLobby для " + pvpBattleKey);
        return result;
    }

    public void setWagerMatchmakingLobbyMap(Map<PvpBattleKey, WagerMatchmakingLobby> wagerMatchmakingLobbyMap) {
        this.wagerMatchmakingLobbyMap = wagerMatchmakingLobbyMap;
    }
}
