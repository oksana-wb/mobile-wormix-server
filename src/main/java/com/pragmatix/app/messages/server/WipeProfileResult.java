package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.serialization.annotations.Command;

@Command(10036)
public class WipeProfileResult {

    public short result;

    public WipeProfileResult() {
    }

    public WipeProfileResult(short result) {
        this.result = result;
    }

    public WipeProfileResult(ShopResultEnum result) {
        this.result = (short) result.getType();
    }

    @Override
    public String toString() {
        return "WipeProfileResult{" +
                "result=" + ShopResultEnum.valueOf(result) +
                '}';
    }
}
