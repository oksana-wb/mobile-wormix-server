package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * Created: 30.04.11 10:40
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
@Command(10032)
public class PumpReactionRateResult {

    public enum ResultEnum implements TypeableEnum {

        OK(0),
        TODAY_ALREADY_PUMPED(1),
        ERROR(2),
        lIMIT_EXEEDED(3);

        private int type;

        ResultEnum(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    /**
     * результат прокачки
     */
    public byte result;

    public PumpReactionRateResult() {
    }

    public PumpReactionRateResult(ResultEnum result) {
        this.result = (byte)result.getType();
    }

    @Override
    public String toString() {
        return "PumpReactionRateResult{" +
                "result=" + result +
                '}';
    }
}
