package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.services.FriendsListService;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;
import java.util.List;


/**
 * @see com.pragmatix.app.controllers.ProfileController#onConnect(com.pragmatix.app.messages.client.Connect, com.pragmatix.app.model.UserProfile)
 */
@Command(1027)
public class ConnectResult implements SecuredResponse {

    /**
     * список профайлов друзей
     */
    public List<UserProfileStructure> profileStructures;

    /**
     * количество друзей online в момент формирования списка друзей
     */
    public short onlineFriends;

    /**
     * количество друзей учитываемых сервером
     */
    public short friends;

    public boolean connectAwardGranted = false;

    public ConnectResult() {
    }

    public ConnectResult(List<UserProfileStructure> profileStructures, FriendsListService.FirstPageBean firstPageBean) {
        this.profileStructures = profileStructures;
        this.onlineFriends = firstPageBean.getOnlineFriends();
        this.friends = firstPageBean.getFriends();
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "ConnectResult{" +
                "friends=" + friends +
                ", onlineFriends=" + onlineFriends +
                ", connectAwardGranted=" + connectAwardGranted +
                ", profileStructures=" + profileStructures +
                '}';
    }
}
