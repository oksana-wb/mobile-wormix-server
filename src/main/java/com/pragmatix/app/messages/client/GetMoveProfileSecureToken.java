package com.pragmatix.app.messages.client;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.06.2016 11:16
 * @see com.pragmatix.app.controllers.ProfileController#onGetMoveProfileSecureToken(GetMoveProfileSecureToken, UserProfile)
 */
@Command(139)
public class GetMoveProfileSecureToken extends SecuredCommand {

    public String sessionKey;

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "RequestMoveProfileSecureToken{" +
                ", sessionKey=" + sessionKey + "" +
                '}';
    }

}
