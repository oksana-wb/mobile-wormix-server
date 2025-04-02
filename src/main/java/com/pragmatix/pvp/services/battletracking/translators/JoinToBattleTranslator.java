package com.pragmatix.pvp.services.battletracking.translators;

import com.pragmatix.pvp.messages.battle.server.PvpSystemMessage;
import com.pragmatix.pvp.messages.handshake.client.JoinToBattle;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.11.12 11:17
 */
public class JoinToBattleTranslator implements TranslatePvpCommandI<JoinToBattle> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private PvpService pvpService;

    public JoinToBattleTranslator(PvpService pvpService) {
        this.pvpService = pvpService;
    }

    @Override
    public PvpBattleActionEnum translateCommand(JoinToBattle cmd, PvpUser profile, BattleBuffer battle) {
        BattleParticipant participant = battle.getParticipant(cmd.socialNetId, cmd.profileId);
        if(participant != null) {
            participant.setState(BattleParticipant.State.connectedAndHasProfile);
        } else {
            log.error("профиль [{}] не зарегистрироват в бою [{}], к которому присоединяется", profile, battle.getBattleId());
        }

        if(battle.allInState(BattleParticipant.State.connectedAndHasProfile)) {
            if(battle.getWager().getValue() > 0 && battle.getParticipants().size() > 1) {
                pvpService.dispatchToAll(battle, new PvpSystemMessage(PvpSystemMessage.TypeEnum.AllJoinedToBattle, -1, battle.getBattleId()), true);
            }
            return PvpBattleActionEnum.AllInState;
        } else {
            return null;
        }
    }

}
