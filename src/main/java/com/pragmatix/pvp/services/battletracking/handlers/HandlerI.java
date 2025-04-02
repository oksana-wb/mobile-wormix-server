package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.06.11 13:09
 */
public interface HandlerI {

  PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer);

}
