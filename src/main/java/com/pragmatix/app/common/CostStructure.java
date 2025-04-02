package com.pragmatix.app.common;

import com.pragmatix.serialization.annotations.Structure;

/**
 * Стоимость предмета в конкретной валюте
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.08.2016 11:36
 */
@Structure
public class CostStructure {

    public MoneyType currencyType;

    public int itemId = -1;

    public int value;

    public CostStructure() {
    }

    public CostStructure(MoneyType currencyType, int value) {
        this.currencyType = currencyType;
        this.value = value;
    }

    public CostStructure(MoneyType currencyType, int itemId, int value) {
        this.currencyType = currencyType;
        this.itemId = itemId;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("{%s%s:%s}", currencyType.name(), itemId >= 0 ? "(" + itemId + ")" : "", value);
    }

}
