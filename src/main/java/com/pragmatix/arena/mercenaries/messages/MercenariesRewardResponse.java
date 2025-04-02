package com.pragmatix.arena.mercenaries.messages;

import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * @see GetMercenariesReward
 */
@Command(10125)
public class MercenariesRewardResponse implements SecuredResponse {

    public GenericAwardStructure[] reward;

    public String sessionKey;

    public MercenariesRewardResponse() {
    }

    public MercenariesRewardResponse(GenericAwardStructure[] reward, String sessionKey) {
        this.reward = reward;
        this.sessionKey = sessionKey;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "MercenariesRewardResponse{" +
                "reward=" + Arrays.toString(reward) +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
