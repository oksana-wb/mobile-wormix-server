package com.pragmatix.pvp.services.battletracking.translators;

import com.pragmatix.pvp.messages.handshake.client.ReadyForBattle;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.11.12 12:15
 */
public class ReadyForBattleTranslator implements TranslatePvpCommandI<ReadyForBattle> {

    @Override
    public PvpBattleActionEnum translateCommand(ReadyForBattle cmd, PvpUser profile, BattleBuffer battle) {
        battle.setParticipantState(profile.getId(), BattleParticipant.State.readyForBattle, BattleParticipant.State.connectedAndHasProfile);

        return battle.allInState(BattleParticipant.State.readyForBattle) ? PvpBattleActionEnum.AllInState : null;
    }

}
