package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.server.PvpSystemMessage;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 16.01.13 13:16
 */
@Component
public class EnvWarnTimeoutHandler extends AbstractHandler {

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {
        PvpSystemMessage pvpSystemMessage = new PvpSystemMessage(PvpSystemMessage.TypeEnum.PlayerLongTimeInTurn, battleBuffer.getInTurn().getPlayerNum(), battleBuffer.getBattleId());
        pvpService.dispatchToAll(battleBuffer, pvpSystemMessage, false);
        return null;
    }

}
