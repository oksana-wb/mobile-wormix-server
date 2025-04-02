package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.app.services.BattleService;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.handshake.server.BattleCreated;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.ReplayService;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.11.12 11:21
 */
@Component
public class CreateHandler extends AbstractHandler {

    @Resource
    private CraftService craftService;

    @Resource
    private ReplayService replayService;

    @Resource
    private BattleService battleService;

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {

        battleBuffer.setBattleProposal(null);

        int seed = AppUtils.generateRandom(1000);

        BattleCreated battleCreated = null;
        for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
            if(battleParticipant.isEnvParticipant()) {
                continue;
            }
            pvpService.calculateEndBattlePoints(battleBuffer, battleParticipant);

            battleCreated = commandFactory.constructBattleCreated(battleBuffer, battleParticipant, seed);
            pvpService.sendCommand(battleParticipant, battleCreated, true, battleBuffer.getBattleId());
        }

        replayService.onBattleCreated(battleBuffer, battleCreated);

        return null;
    }

}
