package com.pragmatix.achieve.messages.server;

import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Результат выбора оружия за очки достижений
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.10.11 14:31
 */
@Command(13006)
public class ChooseBonusItemResult implements SecuredResponse {

    public short result;

    @Resize(TypeSize.UINT32)
    public int itemId;


    public ChooseBonusItemResult() {
    }

    public ChooseBonusItemResult(int itemId, ShopResultEnum result) {
        this.itemId = itemId;
        this.result = (short) result.getType();
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "ChooseBonusItemResult{" +
                "result=" + ShopResultEnum.valueOf(result) +
                ", itemId=" + itemId +
                '}';
    }
}
