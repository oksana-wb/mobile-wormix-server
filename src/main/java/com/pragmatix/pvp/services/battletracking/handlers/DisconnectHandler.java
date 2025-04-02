package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.server.PvpSystemMessage;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.11.12 11:41
 */
@Component
public class DisconnectHandler extends AbstractHandler {

    @Override
    public PvpBattleActionEnum handle(PvpUser user, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {
        BattleParticipant battleParticipant = battleBuffer.getParticipant(user.getId());
        if(battleParticipant != null) {
            BattleParticipant.State state = battleParticipant.getState();
            if(state.canTurn()) {
                battleParticipant.stateBeforeDisconnect = battleParticipant.getState();
                battleParticipant.setState(BattleParticipant.State.waitReconnect);
                battleParticipant.setDisconnectTime(System.currentTimeMillis());

                pvpService.dispatchToParticipants(user.getId(), battleBuffer, new PvpSystemMessage(PvpSystemMessage.TypeEnum.PlayerDisconnected, battleParticipant.getPlayerNum(), user.getBattleId()));
            } else if(state.droppedFromBattle()) {
                // если игрок dropped - шансов на реконнект у него не будет
                pvpService.unbindFromBattle(battleParticipant, battleBuffer, PvpSystemMessage.TypeEnum.PlayerDisconnected, BattleParticipant.State.disconnect);
            }
        }
        return null;
    }
}
