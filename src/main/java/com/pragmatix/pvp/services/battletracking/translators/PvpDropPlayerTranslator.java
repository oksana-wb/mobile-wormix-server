package com.pragmatix.pvp.services.battletracking.translators;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.client.DropReasonEnum;
import com.pragmatix.pvp.messages.battle.client.PvpDropPlayer;
import com.pragmatix.pvp.messages.battle.server.PvpSystemMessage;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.ReplayService;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.12.12 13:24
 */
public class PvpDropPlayerTranslator implements TranslatePvpCommandI {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ReplayService replayService;

    public PvpDropPlayerTranslator(ReplayService replayService) {
        this.replayService = replayService;
    }

    @Override
    public PvpBattleActionEnum translateCommand(PvpCommandI cmd, PvpUser profile, BattleBuffer battleBuffer) {
        PvpDropPlayer msg = (PvpDropPlayer) cmd;

        replayService.onPvpDropPlayer(battleBuffer, msg);

        if(msg.reason == DropReasonEnum.I_AM_CHEATER || msg.reason == DropReasonEnum.SURRENDER) {
            /**
             * {@link com.pragmatix.pvp.services.battletracking.handlers.UnbindHandler}
             */
            return PvpBattleActionEnum.Unbind;
        } else if(msg.reason == DropReasonEnum.DECYNC) {
            /**
             * {@link com.pragmatix.pvp.services.battletracking.handlers.DesyncBattleHandler}
             */
            return PvpBattleActionEnum.Desync;
        } else {
            log.error("battleId={}: invalid type in {}", battleBuffer.getBattleId(), msg);
        }
        return null;
    }

}
