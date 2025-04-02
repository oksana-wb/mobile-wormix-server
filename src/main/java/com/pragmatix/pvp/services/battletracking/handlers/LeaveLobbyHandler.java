package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import com.pragmatix.pvp.services.matchmaking.WagerMatchmakingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.12.12 15:51
 */
@Component
public class LeaveLobbyHandler extends CancelHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private WagerMatchmakingService wagerMatchmakingService;

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battle) {

        wagerMatchmakingService.leaveLobby(battle);
        battle.getBattleProposal().cancel();

        pvpService.logLeaveLobbyStatistic(battle);

        return super.handle(profile, command, action, battle);
    }

}
