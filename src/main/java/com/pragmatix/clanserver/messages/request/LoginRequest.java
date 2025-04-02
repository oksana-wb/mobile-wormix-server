package com.pragmatix.clanserver.messages.request;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 08.04.13 9:11
 *
 * @see com.pragmatix.clanserver.controllers.ClanAuthController#onLoginRequest(LoginRequest, UserProfile)
 */
@Command(Messages.LOGIN_REQUEST)
public class LoginRequest extends LoginBase {

    @Override
    public int getCommandId() {
        return Messages.LOGIN_REQUEST;
    }
}
