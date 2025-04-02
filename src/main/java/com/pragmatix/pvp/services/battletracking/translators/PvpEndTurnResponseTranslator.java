package com.pragmatix.pvp.services.battletracking.translators;

import com.pragmatix.pvp.messages.battle.client.CountedCommandI;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurnResponse;
import com.pragmatix.pvp.messages.battle.server.PvpStartTurn;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static com.pragmatix.pvp.model.BattleParticipant.State.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.08.11 10:41
 */
public class PvpEndTurnResponseTranslator implements TranslatePvpCommandI<PvpEndTurnResponse> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public PvpBattleActionEnum translateCommand(PvpEndTurnResponse cmd, PvpUser profile, BattleBuffer battleBuffer) {

        CountedCommandI lastBufferedCommand = battleBuffer.getLastBufferedCommand();

        if(lastBufferedCommand instanceof PvpStartTurn) {
            // ходил бот
            PvpEndTurn endTurn = new PvpEndTurn();
            endTurn.turnNum = (short) battleBuffer.getCurrentTurn().get();
            endTurn.commandNum = (short) battleBuffer.getCurrentCommandNum().incrementAndGet();
            endTurn.forced = true;
            battleBuffer.getCommandBuffer().add(endTurn);
            lastBufferedCommand = endTurn;
        } else if(!(lastBufferedCommand instanceof PvpEndTurn)) {
            log.error("последней в буфере ожидается команда PvpEndTurn, вместо {}", cmd);
            return null;
        }

        if(!validResponse((PvpEndTurn) lastBufferedCommand, cmd)) {
            log.error("battleId={}, Некорректный ответ: [{}] на [{}]", new Object[]{battleBuffer.getBattleId(), cmd, lastBufferedCommand});
            return null;
        }

        PvpEndTurn endTurn = (PvpEndTurn) lastBufferedCommand;

        BattleParticipant battleParticipant = battleBuffer.getParticipant(profile.getId());
        // ждем от игрока подтверждения передачи хода
        if(battleParticipant.inState(waitEndTurnResponce) || battleParticipant.inState(droppedAndWaitEndTurnResponce)) {
            if(endTurn.isForced()) {
                // если EndTurn был создан сервером, то мы не владеем достоверной информацией о выбывших игроках и состоянии боя
                // берем её из PvpEndTurnResponse
                endTurn.droppedPlayers = cmd.droppedPlayers;
                endTurn.items = cmd.items;
                endTurn.collectedReagents = cmd.collectedReagents;
                endTurn.droppedUnits = cmd.droppedUnits;
                endTurn.participantsHealthInPercent = cmd.participantsHealthInPercent;
                endTurn.battleState = cmd.battleState;
                endTurn.setForced(false);
            } else if(!Arrays.equals(endTurn.droppedPlayers, cmd.droppedPlayers)
                    || !Arrays.equals(endTurn.items, cmd.items)
                    || !Arrays.equals(endTurn.collectedReagents, cmd.collectedReagents)
                    || !Arrays.equals(endTurn.droppedUnits, cmd.droppedUnits)
                    || !Arrays.equals(endTurn.participantsHealthInPercent, cmd.participantsHealthInPercent)
                    || endTurn.battleState != cmd.battleState
                    ) {
                // валидируем PvpEndTurnResponse только от игроков которые продолжают играть
                if(battleParticipant.inState(waitEndTurnResponce)) {
                    log.warn("battleId=:" + battleBuffer.getBattleId() + " Desync: PvpEndTurnResponse не соответствует PvpEndTurn\n" + endTurn + "\n" + cmd);

                    return PvpBattleActionEnum.Desync;
                } else {
                    log.warn("battleId=:" + battleBuffer.getBattleId() + " PvpEndTurnResponse от dropped игрока не соответствует PvpEndTurn\n" + endTurn + "\n" + cmd);
                }
            }

            // выставляем признак что окончание хода игроком подтверждено
            if(battleParticipant.inState(waitEndTurnResponce)) {
                battleParticipant.setState(submitEndTurnResponse);
            } else {
                battleParticipant.setState(droppedAndSubmitEndTurnResponse);
            }

            /**
             * если остались клиенты которые еще не подтвердили окончание хода ждем дальше, иначе выполняем передачу хода
             * {@link com.pragmatix.pvp.services.battletracking.handlers.TransferTurnHandler}
             */
            return battleBuffer.hasInStates(waitEndTurnResponce, droppedAndWaitEndTurnResponce, waitReconnect) ? null : PvpBattleActionEnum.AllInState;
        } else {
            log.warn("battleId={}: участник не находится в состоянии waitEndTurnResponce {}", battleBuffer.getBattleId(), battleParticipant);
        }
        return null;
    }

    private boolean validResponse(PvpEndTurn request, PvpEndTurnResponse response) {
        return request.turnNum == response.turnNum && request.commandNum == response.commandNum;
    }

}
