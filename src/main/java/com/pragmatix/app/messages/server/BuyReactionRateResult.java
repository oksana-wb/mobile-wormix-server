package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * Результат покупки скорости реакции
 *
 * @author denis
 *         Date: 27.12.2009
 *         Time: 17:28:47
 */
@Command(10037)
public class BuyReactionRateResult {
    /**
     * результат покупки
     */
    public ShopResultEnum result;
    /**
     * сколько реакции начислилось на сервере
     */
    public int reactionRateCount;
    /**
     * уровень реакции соответствующий количеству
     */
    public int level;

    public BuyReactionRateResult() {
    }

    public BuyReactionRateResult(ShopResultEnum result) {
        this.result = result;
    }

    public BuyReactionRateResult(ShopResultEnum result, int level, int reactionRateCount) {
        this.result = result;
        this.reactionRateCount= reactionRateCount;
        this.level= level;
    }

    @Override
    public String toString() {
        return "BuyReactionRateResult{" +
                "result=" + result +
                ", reactionRateCount=" + reactionRateCount +
                ", level=" + level +
                '}';
    }
}
