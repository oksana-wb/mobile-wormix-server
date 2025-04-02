package com.pragmatix.pvp.services.battletracking.translators;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.11.12 10:38
 */
public class SimpleTranslator implements TranslatePvpCommandI {

    private PvpBattleActionEnum pvpBattleActionEnum;

    public SimpleTranslator(PvpBattleActionEnum pvpBattleActionEnum) {
        this.pvpBattleActionEnum = pvpBattleActionEnum;
    }

    @Override
    public PvpBattleActionEnum translateCommand(PvpCommandI cmd, PvpUser profile, BattleBuffer battleBuffer) {
        return pvpBattleActionEnum;
    }

}
