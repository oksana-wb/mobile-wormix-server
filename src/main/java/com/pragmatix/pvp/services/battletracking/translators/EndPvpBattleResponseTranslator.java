package com.pragmatix.pvp.services.battletracking.translators;

import com.pragmatix.intercom.messages.EndPvpBattleResponse;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 27.11.12 11:02
 */
public class EndPvpBattleResponseTranslator implements TranslatePvpCommandI<EndPvpBattleResponse> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private PvpService pvpService;

    public EndPvpBattleResponseTranslator(PvpService pvpService) {
        this.pvpService = pvpService;
    }

    @Override
    public PvpBattleActionEnum translateCommand(EndPvpBattleResponse cmd, PvpUser user, BattleBuffer battleBuffer) {
        if(user != null) {
            pvpService.getPvpEndBattleCommand(battleBuffer, battleBuffer.getParticipant(user.getId())).ifPresent(pvpEndBattleCommand -> {
                pvpService.sendToUser(pvpEndBattleCommand
                                .setConfirmed(true)
                                .fillAward(cmd.award)
                        , user, battleBuffer.getBattleId());
            });

            BattleParticipant battleParticipant = battleBuffer.getParticipant(user.getId());
            if(battleParticipant != null){
                battleParticipant.stateBeforeFinate = battleParticipant.getState();
            }

            battleBuffer.setParticipantState(user.getId(), BattleParticipant.State.finate);

            return battleBuffer.allInStateOrEndBattle(BattleParticipant.State.finate) ? PvpBattleActionEnum.AllInState : null;
        } else {
            log.warn("получена команда {} но пользователь не найден", cmd);
            return null;
        }
    }

}
