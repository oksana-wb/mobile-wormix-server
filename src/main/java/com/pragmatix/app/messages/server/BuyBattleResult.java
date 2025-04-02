package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * Результат покупки боя
 *
 * @author denis
 *         Date: 27.12.2009
 *         Time: 17:28:47
 */
@Command(10012)
public class BuyBattleResult {

    public ShopResultEnum result;

    public BuyBattleResult() {
    }

    public BuyBattleResult(ShopResultEnum result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "BuyBattleResult{" +
                "result=" + result +
                '}';
    }

}
