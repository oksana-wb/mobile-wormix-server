package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;

import java.util.*;

/**
 * Команда задаёт конфигурацию рюкзака (порядок и состав оружия)
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 04.02.14 14:50
 *
 *  @see com.pragmatix.app.controllers.ProfileController#onSetBackpackConf(SetBackpackConf, com.pragmatix.app.model.UserProfile)
 */
@Command(129)
public class SetBackpackConf {

    public short[] config1;

    public short[] config2;

    public short[] config3;

    public byte activeConfig;

    public SetBackpackConf() {
    }


    public SetBackpackConf(short[] config1, short[] config2, short[] config3, int activeConfig) {
        this.config1 = config1;
        this.config2 = config2;
        this.config3 = config3;
        this.activeConfig = (byte)activeConfig;
    }

    @Override
    public String toString() {
        return "SetBackpackConf{" +
                "config1=" + Arrays.toString(config1) +
                ", config2=" + Arrays.toString(config2) +
                ", config3=" + Arrays.toString(config3) +
                ", activeConfig=" + activeConfig +
                '}';
    }

}
