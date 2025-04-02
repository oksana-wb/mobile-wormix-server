package com.pragmatix.pvp.messages.battle.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Запрос серверу повторить команды из диапазона
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.06.11 15:45
 *
 *  @see com.pragmatix.pvp.controllers.PvpController#onPvpRetryCommandRequestClient(PvpRetryCommandRequestClient, com.pragmatix.pvp.model.PvpUser)
 */

@Command(1018)
public class PvpRetryCommandRequestClient extends SecuredCommand implements PvpCommandI {
    /**
     *  номер первой команды
     */
    public short fromCommandNum;
    /**
     * номер текущего хода
     */
    public short turnNum;
    /**
     * id текущего боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;

    public PvpRetryCommandRequestClient() {
    }

    public PvpRetryCommandRequestClient(long battleId, short turnNum, short fromCommandNum) {
        this.battleId = battleId;
        this.turnNum = turnNum;
        this.fromCommandNum = fromCommandNum;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public long getBattleId() {
        return battleId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("PvpRetryCommandRequestClient");
        sb.append("{fromCommandNum=").append(fromCommandNum);
        sb.append(", turnNum=").append(turnNum);
        sb.append(", battleId=").append(battleId);
        sb.append(", secureResult=").append(secureResult);
        sb.append('}');
        return sb.toString();
    }
}
