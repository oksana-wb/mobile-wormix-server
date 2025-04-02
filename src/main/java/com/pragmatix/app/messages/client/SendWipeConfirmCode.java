package com.pragmatix.app.messages.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 13.07.11 18:34
 *
 *  @see com.pragmatix.app.controllers.UserProfileController#onSendWipeConfirmCode(SendWipeConfirmCode, com.pragmatix.app.model.UserProfile)
 */
@Command(52)
public class SendWipeConfirmCode extends SecuredCommand {

    public int level;

    public int experience;

    public int rating;

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "SendWipeConfirmCode{" +
                "level=" + level +
                ", experience=" + experience +
                ", rating=" + rating +
                ", secureResult=" + secureResult +
                '}';
    }
}
