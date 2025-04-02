package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.server.PvpSystemMessage;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.06.11 16:19
 */
@Component
public class ForcedEndTurnHandler extends EndTurnHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battle) {
        BattleParticipant inTurn = battle.getInTurn();
        // если того кто ходит не выкинули из боя ранее
        if(inTurn != null && inTurn.getState().canTurn()) {
            // таймаут активного игрока
            pvpService.unbindFromBattle(battle.getInTurn(), battle, PvpSystemMessage.TypeEnum.PlayerDroppedByCommandTimeout, BattleParticipant.State.commandTimeout);
        }

        PvpEndTurn endTurn = new PvpEndTurn();
        endTurn.setForced(true);
        endTurn.battleId = battle.getBattleId();
        endTurn.turnNum = (short) battle.getCurrentTurn().get();
        endTurn.commandNum = (short) (battle.getCurrentCommandNum().get() + 1);
        endTurn.droppedPlayers = new byte[0];
        endTurn.items = new BackpackItemStructure[0];
        endTurn.collectedReagents = new byte[0];

        return super.handle(profile, endTurn, action, battle);
    }

}
