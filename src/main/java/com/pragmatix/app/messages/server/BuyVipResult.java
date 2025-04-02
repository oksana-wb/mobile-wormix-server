package com.pragmatix.app.messages.server;

import com.pragmatix.app.model.RentedItems;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

/**
 * Отсылается в случае успешной покупки VIP аккаунта
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 04.02.14 14:54
 */
@Command(10130)
public class BuyVipResult implements SecuredResponse {

    public RentedItems rentedItems;

    public byte renameAct;

    public String sessionKey;

    public BuyVipResult() {
    }

    public BuyVipResult(RentedItems rentedItems, byte renameAct, String sessionKey) {
        this.rentedItems = rentedItems;
        this.renameAct = renameAct;
        this.sessionKey = sessionKey;
    }

    @Override
    public String toString() {
        return "BuyVipResult{" +
                "renameAct=" + renameAct +
                ", rentedItems=" + rentedItems +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }
}
