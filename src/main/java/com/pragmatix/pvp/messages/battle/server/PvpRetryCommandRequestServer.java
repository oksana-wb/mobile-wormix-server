package com.pragmatix.pvp.messages.battle.server;

import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.06.11 15:45
 */

@Command(1016)
public class PvpRetryCommandRequestServer implements SecuredResponse {

    @Resize(TypeSize.UINT32)
    public long battleId;
    /**
     * порядковые номера команд которые ожидает сервер
     * {commandNum,-1} - серверу нужны все комманды начиная с commandNum}
     */
    public short[] commandNums;
    /**
     * текущий номер хода
     */
    public short turnNum;


    public PvpRetryCommandRequestServer() {
    }

    public PvpRetryCommandRequestServer(long battleId, short turnNum, short[] commandNums) {
        this.battleId = battleId;
        this.turnNum = turnNum;
        this.commandNums = commandNums;
    }

    public PvpRetryCommandRequestServer(long battleId, short turnNum, short commandNumFrom) {
        this.battleId = battleId;
        this.turnNum = turnNum;
        this.commandNums = new short[]{commandNumFrom, -1};
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "PvpRetryCommandRequestServer{" +
                "battleId=" + battleId +
                ", turnNum=" + turnNum +
                ", commandNums=" + Arrays.toString(commandNums) +
                '}';
    }
}
