package com.pragmatix.arena.coliseum.messages;

import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

@Command(10114)
public class GetRewardResponse implements SecuredResponse {

    public GenericAwardStructure[] reward;

    public String sessionKey;

    public GetRewardResponse() {
    }

    public GetRewardResponse(GenericAwardStructure[] reward, String sessionKey) {
        this.reward = reward;
        this.sessionKey = sessionKey;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "GetRewardResponse{" +
                "reward=" + Arrays.toString(reward) +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
