package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import com.pragmatix.pvp.services.matchmaking.BattleProposal;
import com.pragmatix.pvp.services.matchmaking.WagerMatchmakingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 14.05.13 12:34
 */
@Component
public class WidenSearchHandler extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private WagerMatchmakingService wagerMatchmakingService;

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {
        BattleProposal battleProposal = battleBuffer.getBattleProposal();
        if(battleProposal != null) {
            battleProposal.incGridQuality();
            boolean matchmakeResult = wagerMatchmakingService.matchmake(battleBuffer, battleProposal);
            return matchmakeResult ? PvpBattleActionEnum.Match : null;
        } else {
            if(log.isDebugEnabled()){
                log.debug("battleId={}, не возможно расширить критерии поиска для боя, бою ещё не назначена заявка", battleBuffer.getBattleId());
            }
            return null;
        }
    }

}
