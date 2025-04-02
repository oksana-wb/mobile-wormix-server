package com.pragmatix.steam.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 13.03.2017 11:18
 *
 * @see com.pragmatix.steam.SteamController#onCommitPurchaseTxRequest(CommitPurchaseTxRequest, UserProfile)
 */
@Command(6003)
public class CommitPurchaseTxRequest extends SecuredCommand {

    public int purchaseOrderId;

    public String purchaseTxId;

    public String sessionKey;

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "CommitPurchaseTxRequest{" +
                "purchaseOrderId=" + purchaseOrderId +
                ", purchaseTxId='" + purchaseTxId + '\'' +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
