package com.pragmatix.app.messages.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;

/**
 * Комадна отправляется клиентом при обнаружении попытки взлома
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 12.05.12 17:18
 *
 * @see com.pragmatix.app.controllers.BattleController#onCheatDetected(CheatDetected, com.pragmatix.app.model.UserProfile)
 */
@Command(87)
public class CheatDetected extends SecuredCommand {

    public String sessionKey;

    public short banType;

    /**
     * доп. информация по бану
     */
    public String banNote;


    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "CheatDetected{" +
                "banType=" + banType +
                "banNote=" + banNote +
                ", secureResult=" + secureResult +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
