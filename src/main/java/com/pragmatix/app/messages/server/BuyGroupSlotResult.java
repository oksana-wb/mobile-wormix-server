package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 15.02.2016 15:41
 *         <p>
 *         Ответ на команду покупки доп.слота для члена команды
 * @see com.pragmatix.app.messages.client.BuyGroupSlot
 */
@Command(10122)
public class BuyGroupSlotResult implements SecuredResponse {

    public ShopResultEnum result;

    /**
     * @see com.pragmatix.app.messages.client.BuyGroupSlot#newSlotIndex
     */
    public byte newSlotsIndex;

    public String sessionKey;

    public BuyGroupSlotResult() {
    }

    public BuyGroupSlotResult(ShopResultEnum result, byte newSlotsIndex, String sessionKey) {
        this.result = result;
        this.newSlotsIndex = newSlotsIndex;
        this.sessionKey = sessionKey;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "BuyGroupSlotResult{" +
                "result=" + result +
                ", newSlotsIndex=" + newSlotsIndex +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
