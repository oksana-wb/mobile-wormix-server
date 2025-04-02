package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 04.02.14 14:54
 */
@Command(10129)
public class SetBackpackConfResult {

    public SimpleResultEnum result;

    public short[] config1;

    public short[] config2;

    public short[] config3;

    public byte activeConfig;

    public SetBackpackConfResult() {
    }

    public SetBackpackConfResult(SimpleResultEnum result, short[] config1, short[] config2, short[] config3, byte activeConfig) {
        this.result = result;
        this.config1 = config1;
        this.config2 = config2;
        this.config3 = config3;
        this.activeConfig = activeConfig;
    }

    @Override
    public String toString() {
        return "SetBackpackConfResult{" +
                "result=" + result +
                ", config1=" + Arrays.toString(config1) +
                ", config2=" + Arrays.toString(config2) +
                ", config3=" + Arrays.toString(config3) +
                ", activeConfig=" + activeConfig +
                '}';
    }

}
