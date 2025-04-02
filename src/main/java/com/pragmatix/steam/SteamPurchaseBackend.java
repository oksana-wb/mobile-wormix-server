package com.pragmatix.steam;


import com.pragmatix.app.model.UserProfile;
import com.pragmatix.steam.dao.PurchaseTx;
import com.pragmatix.steam.web.request.FinalizeTxnRequest;
import com.pragmatix.steam.web.request.GetUserInfoRequest;
import com.pragmatix.steam.web.request.InitTxnRequest;
import com.pragmatix.steam.web.responses.FinalizeTxnResponse;
import com.pragmatix.steam.web.responses.GetUserInfoResponse;
import com.pragmatix.steam.web.responses.InitTxnResponse;
import com.pragmatix.steam.web.responses.SteamWebResponse;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Author: Vladimir
 * Date: 06.06.2017 10:50
 */
@Component
public class SteamPurchaseBackend {

    @Resource
    private SteamWebAPI steamWebAPI;

    public InitTxnResponse initTxn(InitTxnRequest txnRequest) {
        return steamWebAPI.initTxn(txnRequest);
    }

    public SteamWebResponse.ErrorBlock updatePurchaseInfo(String socialUserId, UserProfile userProfile) {
        GetUserInfoResponse userResponse = steamWebAPI.getUserInfo(new GetUserInfoRequest(socialUserId));

        if (!userResponse.isSuccess()) {
            return userResponse.error;
        } else {
            userProfile.setCountryCode(userResponse.country);
            userProfile.setCurrencyCode(userResponse.currency);
            return null;
        }
    }

    public FinalizeTxnResponse finalizeTxn(PurchaseTx tx) {
        FinalizeTxnRequest txnRequest = new FinalizeTxnRequest(tx.getId());

        return steamWebAPI.finalizeTxn(txnRequest);
    }

    public boolean isDebugPaymentMode() {
        return steamWebAPI.isDebugPaymentMode();
    }
}
