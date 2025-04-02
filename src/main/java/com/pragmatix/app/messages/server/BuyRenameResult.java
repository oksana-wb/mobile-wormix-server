package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 12.03.14 17:0
 * @author Ivan Novikov <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 15.01.2016 16:40
 * @see com.pragmatix.app.messages.client.BuyRename
 * @see com.pragmatix.app.controllers.ShopController#onBuyRename(com.pragmatix.app.messages.client.BuyRename, com.pragmatix.app.model.UserProfile)
 */
@Command(10119)
public class BuyRenameResult implements SecuredResponse {

    public ShopResultEnum result;

    public int teamMemberId;

    public String name;

    public String sessionKey;

    public BuyRenameResult() {
    }

    public BuyRenameResult(ShopResultEnum result, int teamMemberId, String name, String sessionKey) {
        this.result = result;
        this.teamMemberId = teamMemberId;
        this.name = name;
        this.sessionKey = sessionKey;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "BuyRenameResult{" +
                "result=" + result +
                ", teamMemberId=" + teamMemberId +
                ", name='" + name + '\'' +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
