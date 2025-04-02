package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import com.pragmatix.pvp.services.battletracking.PvpBattleStateEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.pragmatix.pvp.model.BattleParticipant.State.droppedAndWaitEndTurnResponce;
import static com.pragmatix.pvp.model.BattleParticipant.State.waitEndTurnResponce;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.06.11 17:04
 */
@Component
public class TransferTurnEnvHandler extends TransferTurnHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, final BattleBuffer battle) {
        PvpEndTurn endTurn = beforeTransfer(battle);
        if(endTurn == null){
            // тем кто еще в игре приписываем ничью, но по причине таймаута
            for(BattleParticipant battleParticipant : battle.getParticipants()) {
               if(battleParticipant.getState().canTurn()){
                   battleParticipant.setState(BattleParticipant.State.timeoutInEnvBattle);
               }
            }
            return PvpBattleActionEnum.Desync;
        }

        // считаем оставшиеся команды
        TreeSet<Byte> liveTeams = new TreeSet<>(pvpService.countLiveTeams(battle));

        byte currentTeam = battle.getInTurn().getPlayerTeam();

        if(liveTeams.size() > 1) {
            // бой продолжается

            // определяем команду которой ходить дальше, если наступит её черёд
            Byte nextTeam = liveTeams.higher(currentTeam);
            if(nextTeam == null) {
                nextTeam = liveTeams.first();
            }

            // находим кто остался в живых в командах
            TreeMap<Byte, Long> liveCurrTeamPlayers = new TreeMap<>();
            TreeMap<Byte, Long> liveNextTeamPlayers = new TreeMap<>();
            for(BattleParticipant battleParticipant : battle.getParticipants()) {
                if(battleParticipant.getState().canTurn()) {
                    TreeMap<Byte, Long> liveTeamPlayers = null;
                    if(battleParticipant.getPlayerTeam() == nextTeam) {
                        liveTeamPlayers = liveNextTeamPlayers;
                    } else if(battleParticipant.getPlayerTeam() == currentTeam) {
                        liveTeamPlayers = liveCurrTeamPlayers;
                    }
                    if(liveTeamPlayers != null) {
                        liveTeamPlayers.put(battleParticipant.getPlayerNum(), battleParticipant.getPvpUserId());
                        if(battleParticipant.getTroopOnLoan() > 0) {
                            liveTeamPlayers.put(battleParticipant.getTroopOnLoan(), battleParticipant.getPvpUserId());
                        }
                    }
                }
            }

            // кому ходить дальше
            long nextProfile = 0;
            byte nextNum = 0;

            // кто в текущей команде ходил последним
            byte lastTeamPlayerNum = battle.getTurningNumByTeam()[currentTeam];
            Map.Entry<Byte, Long> nextEntry = liveCurrTeamPlayers.higherEntry(lastTeamPlayerNum);
            if(nextEntry != null) {
                // есть кому ещё ходить в текущей команде
                nextProfile = nextEntry.getValue();
                nextNum = nextEntry.getKey();
            }

            if(nextProfile == 0) {
                // передаем ход след. команде
                // кто в след. команде ходил последним
                lastTeamPlayerNum = battle.getTurningNumByTeam()[nextTeam];
                if(liveNextTeamPlayers.size() > 1) {
                    nextEntry = liveNextTeamPlayers.higherEntry(lastTeamPlayerNum);
                    if(nextEntry == null) {
                        nextEntry = liveNextTeamPlayers.firstEntry();
                    }
                    nextProfile = nextEntry.getValue();
                    nextNum = nextEntry.getKey();
                } else {
                    // остался последний живой в команде
                    nextEntry = liveNextTeamPlayers.firstEntry();
                    nextProfile = nextEntry.getValue();
                    nextNum = nextEntry.getKey();
                }
            }

            transferTurn(battle, nextProfile, nextNum);

            PvpBattleStateEnum nextState = battle.getInTurn().isEnvParticipant() ? PvpBattleStateEnum.EnvironmentInTurn : PvpBattleStateEnum.ReadyToDispatch;
            // необходимо подправить состояния, в случае если "ходить" будет бот
            if(nextState == PvpBattleStateEnum.EnvironmentInTurn) {
                for(BattleParticipant battleParticipant : battle.getParticipants()) {
                    if(!battleParticipant.isEnvParticipant()) {
                        BattleParticipant.State state = battleParticipant.getState();
                        if(!state.canTurn()) {
                            if(state.canAccept()) {
                                battleParticipant.setState(droppedAndWaitEndTurnResponce);
                            }
                        } else {
                            battleParticipant.setState(waitEndTurnResponce);
                        }
                    }
                }
            }

            // при передачи хода от бота к боту необходимо вручную выставить время изменения состояния
            if(battle.getBattleState() == PvpBattleStateEnum.EnvironmentInTurn && nextState == PvpBattleStateEnum.EnvironmentInTurn) {
                battle.setLastChangeStateTime(System.currentTimeMillis());
            }

            battle.setBattleState(nextState);
        } else {
            // бой окончен
            pvpService.finishBattle(battle);
        }
        return null;
    }
}
