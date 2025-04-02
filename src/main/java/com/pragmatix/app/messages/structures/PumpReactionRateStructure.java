package com.pragmatix.app.messages.structures;

import com.pragmatix.app.messages.server.PumpReactionRateResult;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Structure;
import com.pragmatix.serialization.annotations.Resize;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.05.11 13:25
 */
@Structure
public class PumpReactionRateStructure {
    /**
     * id друга которому прокачивали реакцию
     */
    @Resize(TypeSize.UINT32)
    public long friendId;
    /**
     * результат прокачки
     * @see com.pragmatix.app.messages.server.PumpReactionRateResult.ResultEnum
     */
    public byte result;

    public PumpReactionRateStructure() {
    }

    public PumpReactionRateStructure(long friendId, PumpReactionRateResult.ResultEnum resultEnum) {
        this.friendId = friendId;
        this.result = (byte)resultEnum.getType();
    }

    @Override
    public String toString() {
        return "PumpReactionRateStructure{" +
                "friendId=" + friendId +
                ", result=" + result +
                '}';
    }
}
