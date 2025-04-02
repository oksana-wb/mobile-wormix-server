package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.CostStructure;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.messages.client.BuySkin;
import com.pragmatix.app.model.PurchaseResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Structure;

import java.util.List;

/**
 * Ответ на команду покупки
 *
 * @see com.pragmatix.app.controllers.ShopController#onBuySkin(BuySkin, UserProfile)
 */
@Structure(isAbstract = true)
public class PurchaseResponse implements SecuredResponse {

    public ShopResultEnum result;

    // сколько и в какой валюте было списано за покупку
    public List<CostStructure> cost;

    public String sessionKey;

    public PurchaseResponse() {
    }

    public PurchaseResponse(PurchaseResult purchaseResult, String sessionKey) {
        this(purchaseResult.result, purchaseResult.cost, sessionKey);
    }

    public PurchaseResponse(ShopResultEnum result, List<CostStructure> cost, String sessionKey) {
        this.result = result;
        this.cost = cost;
        this.sessionKey = sessionKey;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

}
