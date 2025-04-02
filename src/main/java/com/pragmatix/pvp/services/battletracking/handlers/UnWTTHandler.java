package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.client.PvpDropPlayer;
import com.pragmatix.pvp.messages.battle.server.PvpSystemMessage;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.pragmatix.pvp.model.BattleParticipant.State.droppedAndWaitEndTurnResponce;
import static com.pragmatix.pvp.model.BattleParticipant.State.waitEndTurnResponce;

/**
 * Обработчик выходов из боя во время передачи хода. Игрок: сдался, истекло время реконнекта, записан в читеры
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.12.12 15:10
 */
@Component
public class UnWTTHandler extends UnbindHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {
        BattleParticipant battleParticipant = battleBuffer.getParticipant(profile.getId());
        BattleParticipant.State originalState = battleParticipant.getState();
        long battleId = battleBuffer.getBattleId();
        PvpDropPlayer pvpDropPlayer = (PvpDropPlayer) command;

        PvpSystemMessage.TypeEnum reason = getUnbindReasonMessage(battleId, pvpDropPlayer);
        if(originalState.endBattle()) {
            log.warn(String.format("battleId=%s: попытка 'выкинуть' участника из боя, который этот бой уже покинул [%s] %s", battleId, reason, battleParticipant));
            return null;
        }

        pvpService.unbindFromBattle(battleParticipant, battleBuffer, reason, getUnbindState(battleId, pvpDropPlayer));

        if(originalState.droppedFromBattle()) {
            // unbind from battle player which have been dropped
            return returnResult(battleBuffer);
        }

        int liveTeams = pvpService.countLiveTeams(battleBuffer).size();
        if(liveTeams < 2) {
            // можем завершать бой
            if(log.isDebugEnabled()) {
                log.debug("battleId={}: liveTeams count is {}, send EndBattle action ...", battleId, liveTeams);
            }
            /**
             * {@link com.pragmatix.pvp.services.battletracking.handlers.EndBattleHandler}
             */
            return PvpBattleActionEnum.EndBattle;
        }

        return returnResult(battleBuffer);

    }

    private PvpBattleActionEnum returnResult(BattleBuffer battleBuffer) {
        /**
         * если остались клиенты которые еще не подтвердили окончание хода ждем дальше, иначе выполняем передачу хода
         * {@link TransferTurnHandler}
         */
        return battleBuffer.hasInStates(waitEndTurnResponce, droppedAndWaitEndTurnResponce) ? null : PvpBattleActionEnum.AllInState;
    }

}
