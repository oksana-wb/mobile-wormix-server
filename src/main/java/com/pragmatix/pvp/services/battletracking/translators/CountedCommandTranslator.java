package com.pragmatix.pvp.services.battletracking.translators;

import com.pragmatix.pvp.messages.battle.client.CountedCommandI;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.08.11 10:28
 */
public class CountedCommandTranslator implements TranslatePvpCommandI<CountedCommandI> {

    protected PvpBattleActionEnum validCommand;

    public CountedCommandTranslator(PvpBattleActionEnum validCommand) {
        this.validCommand = validCommand;
    }

    @Override
    public PvpBattleActionEnum translateCommand(CountedCommandI cmd, PvpUser profile, BattleBuffer battleBuffer) {
        if(cmd.getTurnNum() > battleBuffer.getCurrentTurn().get()) {
            return PvpBattleActionEnum.Desync;
        }
        return validCommand;
    }
}
