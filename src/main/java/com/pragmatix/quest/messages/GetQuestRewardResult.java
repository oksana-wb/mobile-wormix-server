package com.pragmatix.quest.messages;

import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

import java.util.List;

@Command(10118)
public class GetQuestRewardResult implements SecuredResponse {

    public ShopResultEnum resultEnum;

    public List<GenericAwardStructure> reward;

    public String sessionKey;

    public GetQuestRewardResult() {
    }

    public GetQuestRewardResult(ShopResultEnum resultEnum, List<GenericAwardStructure> reward, String sessionKey) {
        this.resultEnum = resultEnum;
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
                "resultEnum=" + resultEnum +
                ", reward=" + reward +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
