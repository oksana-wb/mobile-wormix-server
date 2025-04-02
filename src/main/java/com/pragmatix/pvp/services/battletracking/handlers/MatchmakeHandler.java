package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import com.pragmatix.pvp.services.matchmaking.WagerMatchmakingService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.12.12 15:51
 */
@Component
public class MatchmakeHandler extends AbstractHandler {

    @Resource
    private WagerMatchmakingService wagerMatchmakingService;

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {
        boolean matchmakeResult = wagerMatchmakingService.matchmake(battleBuffer);
        return matchmakeResult ? PvpBattleActionEnum.Match : null;
    }

}
