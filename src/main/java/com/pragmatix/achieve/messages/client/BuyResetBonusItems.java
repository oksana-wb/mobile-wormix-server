package com.pragmatix.achieve.messages.client;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.11.12 13:00
 *
 * @see com.pragmatix.achieve.controllers.AchieveController#onBuyResetBonusItems(BuyResetBonusItems, UserProfile)
 */
@Command(3010)
public class BuyResetBonusItems extends SecuredCommand {
    /**
     * номер последовательности
     */
    public int requestNum;

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "BuyResetBonusItems{" +
                "requestNum=" + requestNum +
                ", secureResult=" + secureResult +
                '}';
    }

}
