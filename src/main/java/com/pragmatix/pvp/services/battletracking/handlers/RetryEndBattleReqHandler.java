package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.09.11 10:47
 */
@Component
public class RetryEndBattleReqHandler extends AbstractHandler {

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {
        for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
            if(!battleParticipant.getState().endBattle()) {
                pvpService.dispatchToServerEndBattleRequest(battleBuffer, true);
                return null;
            }
        }
        // если все участники боя "отвалились" ранее
        return PvpBattleActionEnum.AllInState;
    }

}
