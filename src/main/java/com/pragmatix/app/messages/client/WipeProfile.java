package com.pragmatix.app.messages.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;

/**
 * Команда на "обнуление" профиля
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.05.11 8:54
 *
 * @see com.pragmatix.app.controllers.UserProfileController#onWipeProfile(WipeProfile, com.pragmatix.app.model.UserProfile)
 */
@Command(48)
public class WipeProfile extends SecuredCommand {
    /**
     * код подтвержения обнуления введенный игрогом
     */
    public String confirmCode;

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "WipeProfile{" +
                "confirmCode='" + confirmCode + '\'' +
                ", secureResult=" + secureResult +
                '}';
    }
}
