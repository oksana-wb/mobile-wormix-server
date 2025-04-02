package com.pragmatix.pvp.services.battletracking;

import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.PvpUser;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.11.12 15:54
 */
public interface BattleStateTrackerI {

    void handleEvent(PvpUser user, Object event, BattleBuffer battleBuffer);

    void handleAction(PvpBattleActionEnum action, BattleBuffer battleBuffer);

    PvpBattleStateEnum getInitState();

}
