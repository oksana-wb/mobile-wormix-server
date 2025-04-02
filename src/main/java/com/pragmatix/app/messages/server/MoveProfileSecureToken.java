package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.RestrictionItemStructure;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * @see com.pragmatix.app.messages.client.GetMoveProfileSecureToken
 */
@Command(10139)
public class MoveProfileSecureToken implements SecuredResponse {

    public String secureToken;

    public String sessionKey;

    public MoveProfileSecureToken() {
    }

    public MoveProfileSecureToken(String secureToken, String sessionKey) {
        this.secureToken = secureToken;
        this.sessionKey = sessionKey;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "MoveProfileSecureToken{" +
                "secureToken='" + secureToken + '\'' +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }

}
