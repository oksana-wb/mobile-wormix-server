package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 07.07.11 12:33
 */
@Component
public class DesyncBattleHandler extends AbstractHandler {

    @Override
    public PvpBattleActionEnum handle(PvpUser user, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {
        // тем кто еще в игре приписываем ничью, те кто читерил, кто уже не с нами или отвалился будут аннигилированы
        for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
           if(battleParticipant.getState().canTurn()){
               battleParticipant.setState(BattleParticipant.State.desync);
           }
        }

        // лог боя нужно тогда сохранить
        battleBuffer.getBattleLog().ifPresent(battleLog -> battleLog.setNeedToBeSaved(battleBuffer.hasInState(BattleParticipant.State.desync)));

        pvpService.finishBattle(battleBuffer);
        return null;
    }

}
