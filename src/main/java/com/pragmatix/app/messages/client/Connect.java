package com.pragmatix.app.messages.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.serialization.annotations.Command;

import java.util.List;

/**
 * @see com.pragmatix.app.controllers.ProfileController#onConnect(com.pragmatix.app.messages.client.Connect, com.pragmatix.app.model.UserProfile)
 */
@Command(27)
public class Connect extends SecuredCommand {

    /**
     * id пользователя
     */
    public String socialProfileId;

    /**
     * автризационный ключ
     */
    public String authKey;

    /**
     * id профайлов которые нобходимо прислать
     */
    public List<String> ids;

    public SocialServiceEnum socialNetId;


    public Connect() {
    }

    @Override
    public String getSessionKey() {
        //todo add sessionKey field
        return null;
    }

    @Override
    public String toString() {
        return "Connect{" +
                "socialProfileId=" + socialProfileId +
                ", socialNetId=" + socialNetId +
                ", ids=" + ids +
                ", authKey='" + authKey + '\'' +
                ", secureResult=" + secureResult +
                "'}";
    }

}
