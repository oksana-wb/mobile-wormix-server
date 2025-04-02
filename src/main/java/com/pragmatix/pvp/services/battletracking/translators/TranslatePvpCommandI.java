package com.pragmatix.pvp.services.battletracking.translators;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;

/**
 * Классы этого Интерфейса преобразуют входящин PVP команды а лексемы (BattleActionEnum) автомата боя
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.08.11 10:26
 */
public interface TranslatePvpCommandI<PVP_COMMAND extends PvpCommandI> {

    PvpBattleActionEnum translateCommand(PVP_COMMAND cmd, PvpUser profile, BattleBuffer battleBuffer);

}
