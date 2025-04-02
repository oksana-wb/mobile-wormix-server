package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.11.2016 11:44
 * @see com.pragmatix.app.messages.client.SellStuff
 */
@Command(10137)
public class SellStuffResult implements SecuredResponse {

    public List<Integer> soldItems;

    public List<GenericAwardStructure> awards;

    public String sessionKey;

    public SellStuffResult() {
    }

    public SellStuffResult(List<Integer> soldItems, List<GenericAwardStructure> awards, String sessionKey) {
        this.soldItems = soldItems;
        this.awards = awards;
        this.sessionKey = sessionKey;
    }

    @Override
    public String toString() {
        return "SellStuffResult{" +
                "soldItems=" + soldItems +
                ", awards=" + awards +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

}
