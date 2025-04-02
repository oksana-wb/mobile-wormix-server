package com.pragmatix.app.messages.client;

import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.app.controllers.ProfileController#onDisconnect(Disconnect, com.pragmatix.app.model.UserProfile)
 */
@Command(60)
public class Disconnect {

    public SocialServiceEnum socialNetId;

    public Disconnect() {
    }

    @Override
    public String toString() {
        return "Disconnect{" +
                "socialNetId=" + socialNetId +
                '}';
    }
}
