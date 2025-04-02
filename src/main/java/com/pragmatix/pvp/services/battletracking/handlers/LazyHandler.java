package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 07.07.11 12:35
 */
@Component
public class LazyHandler extends AbstractHandler{

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {
        return null;
    }

}
