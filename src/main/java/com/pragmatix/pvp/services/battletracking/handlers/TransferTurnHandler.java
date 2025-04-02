package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.client.CountedCommandI;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.server.PvpStartTurn;
import com.pragmatix.pvp.messages.battle.server.PvpSystemMessage;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.ReplayService;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.*;

import static com.pragmatix.pvp.model.BattleParticipant.State.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.06.11 17:04
 */
@Component
public class TransferTurnHandler extends AbstractHandler {

    @Resource
    private ReplayService replayService;

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, final BattleBuffer battle) {
        PvpEndTurn endTurn = beforeTransfer(battle);
        if(endTurn == null){
            return PvpBattleActionEnum.Desync;
        }

        // считаем оставшиеся команды
        TreeSet<Byte> liveTeams = new TreeSet<Byte>(pvpService.countLiveTeams(battle));

        // убитые за ход по командам
        Set<Byte> droppedFromTeams = new TreeSet<Byte>();
        for(byte droppedPlayer : endTurn.droppedPlayers) {
            BattleParticipant droppedParticipant = battle.getParticipantByNum(droppedPlayer);
            if(droppedParticipant != null) {
                droppedFromTeams.add(droppedParticipant.getPlayerTeam());
            }
        }

        byte currentPlayerTeam = battle.getInTurn().getPlayerTeam();

        // присваиваем ничью
        // живых не осталось, но они были убиты все за этот ход
        if(liveTeams.isEmpty() && droppedFromTeams.size() > 1) {
            for(byte droppedPlayer : endTurn.droppedPlayers) {
                BattleParticipant droppedParticipant = battle.getParticipantByNum(droppedPlayer);
                if(droppedParticipant != null) {
                    if(droppedParticipant.getState().droppedFromBattle()) {
                        droppedParticipant.setState(BattleParticipant.State.draw);
                    } else {
                        // если уничтожена команда под внешним управлением, управляющему игроку - ничью
                        for(BattleParticipant participant : battle.getParticipants()) {
                            if(participant.getPlayerTeam() == droppedParticipant.getPlayerTeam() && participant.getTroopOnLoan() == droppedParticipant.getPlayerNum()) {
                                participant.setState(BattleParticipant.State.draw);
                                break;
                            }
                        }
                    }
                }
            }
        }

        if(liveTeams.size() > 1) {
            // бой продолжается
            // определяем команду которой ходить дальше
            Byte nextTeam = liveTeams.higher(currentPlayerTeam);
            if(nextTeam == null) {
                nextTeam = liveTeams.first();
            }

            // находим кто остался в живых в команде
            TreeMap<Byte, Long> liveTeamPlayers = new TreeMap<Byte, Long>();
            for(BattleParticipant battleParticipant : battle.getParticipants()) {
                if(battleParticipant.getPlayerTeam() == nextTeam && battleParticipant.getState().canTurn()) {
                    liveTeamPlayers.put(battleParticipant.getPlayerNum(), battleParticipant.getPvpUserId());
                }
            }

            // кому ходить дальше
            long nextProfile;
            byte nextNum = 0;

            if(liveTeamPlayers.size() > 1) {
                // кто в команде ходил последним
                byte lastTeamPlayerNum = battle.getTurningNumByTeam()[nextTeam];
                Map.Entry<Byte, Long> nextEntry = liveTeamPlayers.higherEntry(lastTeamPlayerNum);
                if(nextEntry == null) {
                    nextEntry = liveTeamPlayers.firstEntry();
                }
                nextProfile = nextEntry.getValue();
                nextNum = nextEntry.getKey();
            } else {
                // остался последний живой в команде
                Map.Entry<Byte, Long> nextEntry = liveTeamPlayers.firstEntry();
                nextProfile = nextEntry.getValue();
                nextNum = nextEntry.getKey();
            }

            transferTurn(battle, nextProfile, nextNum);
        } else {
            // бой окончен
            pvpService.finishBattle(battle);
        }
        return null;
    }

    @Null
    protected PvpEndTurn beforeTransfer(final BattleBuffer battle) {
        if(!(battle.getLastBufferedCommand() instanceof PvpEndTurn)) {
            return null;
        }

        // удаляем из боя тех кто не подтвердил окончание хода
        battle.visitExceptTurningAndInStates(waitEndTurnResponce, droppedAndWaitEndTurnResponce, participant -> {
            // остались клиенты которые еще не подтвердили окончание хода
            pvpService.unbindFromBattle(participant, battle, PvpSystemMessage.TypeEnum.PlayerDroppedByResponceTimeout, responseTimeout);
        });

        PvpEndTurn endTurn = (PvpEndTurn) battle.getLastBufferedCommand();

        // удаляям из боя тех кто почил с миров во время хода, и смог ранее подтвердить передачу хода
        pvpService.dropPlayesFromBattle(battle, endTurn);

        // сохраняем найденные реагенты
        pvpService.addReagents(battle, endTurn.collectedReagents);

        // сохраняем выбывших юнитов
        pvpService.storeDroppedUnits(battle, endTurn.droppedUnits);

        // сохраняем HP участников боя в % от первоночального
        battle.participantsHealthInPercent = endTurn.participantsHealthInPercent;

        // сохраняем потраченное оружие
        pvpService.addItems(battle, endTurn.items);
        return endTurn;
    }

    protected void transferTurn(BattleBuffer battle, long nextUserId, byte nextNum) {
        // передаём ход
        battle.setTurningPvpId(nextUserId);
        BattleParticipant turningUser = battle.getInTurn();
        battle.getTurningNumByTeam()[turningUser.getPlayerTeam()] = nextNum;
        battle.getCurrentTurn().incrementAndGet();
        battle.setStartTurnTime(System.currentTimeMillis());
        battle.incButtlePenaltyTime();
        battle.setTurnPenaltyTime(0);
        // бой начинается с 1-цы 1-ая команда StartTurn
        battle.getCurrentCommandNum().set(1);
        battle.clearCommandBuffer();
        battle.clearFutureCommandBuffer();
        battle.getBattleLog().ifPresent(battleLog ->
            battleLog.startNewTurnBy(turningUser, battle.getCurrentTurn().get(), battle.getStartTurnTime())
        );

        // передаем в StartTurn тех кто уже выбыл
        Set<Byte> droppedPlayers = new HashSet<Byte>();
        for(BattleParticipant battleParticipant : battle.getParticipants()) {
            BattleParticipant.State state = battleParticipant.getState();
            if(!state.canTurn()) {
                droppedPlayers.add(battleParticipant.getPlayerNum());
                if(state.canAccept()) {
                    battleParticipant.setState(droppedFromBattle);
                }
            } else if(battleParticipant.getPvpUserId() == nextUserId) {
                battleParticipant.setState(sendCommand);
            } else {
                battleParticipant.setState(acceptCommand);
            }
        }
        byte[] droppedArr;
        if(droppedPlayers.size() > 0) {
            droppedArr = new byte[droppedPlayers.size()];
            int i = 0;
            for(byte b : droppedPlayers) {
                droppedArr[i] = b;
                i++;
            }
        } else {
            droppedArr = new byte[0];
        }
        PvpStartTurn startTurn = new PvpStartTurn(battle, droppedArr);

        List<CountedCommandI> commandBuffer = battle.getCommandBuffer();
        commandBuffer.add(startTurn);

        replayService.onPvpStartTurn(battle, startTurn);

        // рассылаем  StartTurn всем, с указанием кому сейчас ходить
        pvpService.dispatchToAll(battle, startTurn, false);
    }
}
