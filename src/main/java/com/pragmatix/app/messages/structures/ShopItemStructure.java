package com.pragmatix.app.messages.structures;

import com.pragmatix.app.common.MoneyType;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Structure;
import com.pragmatix.serialization.annotations.Resize;

/**
 * структура передайться с клиента на сервер при покупке вещей в магазине
 * User: denis
 * Date: 30.11.2009
 * Time: 1:50:39
 */
@Structure
public class ShopItemStructure {

    /**
     * id оружия
     */
    @Resize(TypeSize.UINT32)
    public int id;

    /**
     * количество данного оружия
     * <b>если -1, то количество бесконечно</b>
     */
    public int count;

    /**
     * тип денег при покупке
     */
    public int moneyType;

    @Ignore
    public int cost;

    public ShopItemStructure() {
    }

    @Override
    public String toString() {
        return "ShopItemStructure{" +
                "id=" + id +
                ", count=" + count +
                ", moneyType=" + moneyType +
                '}';
    }

    public ShopItemStructure(int id, int count, int moneyType) {
        this.id = id;
        this.count = count;
        this.moneyType = moneyType;
    }

    public boolean isRealMoneyType(){
        return moneyType == MoneyType.REAL_MONEY.getType();
    }
}
