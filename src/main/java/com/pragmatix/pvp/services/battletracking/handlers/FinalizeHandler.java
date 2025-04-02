package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.09.11 10:47
 */
@Component
public class FinalizeHandler extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public PvpBattleActionEnum handle(PvpUser none, PvpCommandI command, PvpBattleActionEnum action, final BattleBuffer battle) {
        // бой окончен, рассылаем команду с результатами боя (с confirmed = false) тем чьи main сервера не подтвердили оконочание боя
        battle.visitAllInState(BattleParticipant.State.waitEndBattleRequestConfirm, participant -> {
            pvpService.getPvpEndBattleCommand(battle, participant).ifPresent(pvpEndBattleCommand -> {
                pvpService.sendToUser(pvpEndBattleCommand, participant.getPvpUserId(), battle.getBattleId());
                log.error("battleId {}: окочание боя не подтверждено для {}", battle.getBattleId(), participant);
            });
        });
        pvpService.logEndBattleStatistic(battle);
        // TODO: отказаться от ↑ в пользу ↓ (пока дублирует)
        pvpService.logEndBattleDetails(battle);
        return null;
    }

}
