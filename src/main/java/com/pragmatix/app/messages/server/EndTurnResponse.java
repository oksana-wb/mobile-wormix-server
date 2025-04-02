package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.client.EndTurn;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 25.10.2017 12:41
 * @see com.pragmatix.app.messages.client.EndTurn
 * @see com.pragmatix.app.controllers.BattleController#onEndTurn(EndTurn, UserProfile)
 */
@Command(10120)
public class EndTurnResponse {

    public SimpleResultEnum result;
    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;
    /**
     * номер хода в бою
     */
    public short turnNum;

    public short lastTurnNum;

    public EndTurnResponse() {
    }

    public EndTurnResponse(SimpleResultEnum result, EndTurn msg, short lastTurnNum) {
        this.result = result;
        this.battleId = msg.battleId;
        this.turnNum = msg.turn.turnNum;
        this.lastTurnNum = lastTurnNum;
    }

    @Override
    public String toString() {
        return "EndTurnResponse(" + result.name() + "){" +
                "battleId=" + battleId +
                ", turnNum=" + turnNum +
                ", lastTurnNum=" + lastTurnNum +
                '}';
    }

}
