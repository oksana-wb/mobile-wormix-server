package com.pragmatix.achieve.messages.server;

import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.messages.client.BuyResetBonusItems;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.11.12 13:02
 *  @see com.pragmatix.achieve.controllers.AchieveController#onBuyResetBonusItems(BuyResetBonusItems, UserProfile)
 *
 */
@Command(13010)
public class BuyResetBonusItemsResult implements SecuredResponse {

    public ShopResultEnum result;

    /**
     * номер последовательности из запроса BuyResetBonusItems
     */
    public int requestNum;

    public BuyResetBonusItemsResult() {
    }

    public BuyResetBonusItemsResult(ShopResultEnum result, int requestNum) {
        this.result = result;
        this.requestNum = requestNum;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "BuyResetBonusItemsResult{" +
                "result=" + result +
                ", requestNum=" + requestNum +
                '}';
    }
}
