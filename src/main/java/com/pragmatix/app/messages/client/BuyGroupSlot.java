package com.pragmatix.app.messages.client;

import com.pragmatix.app.common.MoneyType;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 15.02.2016 15:29
 *         <p>
 *         Команда покупки доп.слота для члена команды:
 *         @see com.pragmatix.app.model.UserProfile#extraGroupSlotsCount
 *         @see com.pragmatix.app.controllers.ShopController#onBuyGroupSlot(BuyGroupSlot, com.pragmatix.app.model.UserProfile)
 *         @see com.pragmatix.app.messages.server.BuyGroupSlotResult
 */
@Command(122)
public class BuyGroupSlot {

    public byte newSlotIndex;  // начиная с единицы: 1, 2 или 3

    public MoneyType moneyType;

    public BuyGroupSlot() {
    }

    public BuyGroupSlot(byte newSlotIndex, MoneyType moneyType) {
        this.newSlotIndex = newSlotIndex;
        this.moneyType = moneyType;
    }

    @Override
    public String toString() {
        return "BuyGroupSlot{" +
                "newSlotIndex=" + newSlotIndex +
                ", moneyType=" + moneyType +
                '}';
    }
}
