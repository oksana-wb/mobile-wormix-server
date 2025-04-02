package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * Команда задаёт конфигурацию рюкзака (порядок и состав оружия)
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 04.02.14 14:50
 *  @see com.pragmatix.app.controllers.ProfileController#onSetHotkeys(SetHotkeys, com.pragmatix.app.model.UserProfile)
 */
@Command(128)
public class SetHotkeys {

    public short[] hotkeys;

    public SetHotkeys() {
    }


    public SetHotkeys(short[] hotkeys) {
        this.hotkeys = hotkeys;
    }

    @Override
    public String toString() {
        return "SetHotkeys{" +
                ", hotkeys=" + Arrays.toString(hotkeys) +
                '}';
    }

}
