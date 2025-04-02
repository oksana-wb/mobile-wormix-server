package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.app.common.BattleState;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.intercom.messages.CompareAndSetBattleState;
import com.pragmatix.intercom.messages.GetProfileResponse;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.12.12 12:44
 */
@Component
public class RollbackHandler extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {
        if(command instanceof GetProfileResponse) {
            GetProfileResponse msg = (GetProfileResponse) command;
            // в рамках подготовки боя был прислан профиль, но бой уже был отменен ранее
            // возвращаем статус участнику
            BattleParticipant participant = battleBuffer.getParticipant(msg.socialNetId, msg.getProfileId());
            if(log.isDebugEnabled()) {
                log.debug("battleId={}: бой уже отменен, для {} возврящаем состояние NOT_IN_BATTLE", battleBuffer.getBattleId(), PvpService.formatPvpUserId(participant.getPvpUserId()));
            }
            CompareAndSetBattleState compareAndSetBattleState = new CompareAndSetBattleState(participant, BattleState.WAIT_START_BATTLE, BattleState.NOT_IN_BATTLE, 0, battleBuffer.getBattleType());
            Messages.toServer(compareAndSetBattleState, participant.getMainServer(), false);
        }
        return null;
    }

}
