package com.pragmatix.achieve.messages.client;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Выбрать оружие за очки достижений
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.10.11 14:16
 * @see com.pragmatix.achieve.controllers.AchieveController#onChooseBonusItem(ChooseBonusItem, UserProfile)
 */
@Command(3005)
public class ChooseBonusItem extends SecuredCommand {
    /**
     * id оружия или шапки
     */
    @Resize(TypeSize.UINT32)
    public int itemId;

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "ChooseBonusItem{" +
                "itemId=" + itemId +
                ", secureResult=" + secureResult +
                '}';
    }
}
