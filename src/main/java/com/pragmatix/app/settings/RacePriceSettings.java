package com.pragmatix.app.settings;

import com.pragmatix.app.common.Race;

import java.util.HashMap;
import java.util.Map;

/**
 * Настройки цены смены расы
 *
 * @author denis
 *         Date: 27.12.2009
 *         Time: 17:45:37
 */
public class RacePriceSettings {

    private Map<Integer, IItemRequirements> priceMap;

    public Map<Integer, IItemRequirements> getPriceMap() {
        return priceMap;
    }

    public void setPriceMap(Map<Race, IItemRequirements> map) {
        priceMap = new HashMap<>();
        for(Map.Entry<Race, IItemRequirements> entry : map.entrySet()) {
            priceMap.put(entry.getKey().getType(), entry.getValue());
        }
    }

}
