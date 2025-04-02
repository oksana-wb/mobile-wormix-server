package com.pragmatix.craft.messages;

import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.07.12 9:17
 *
 * @see com.pragmatix.craft.cotrollers.CraftController#onDowngradeWeapon(DowngradeWeapon, com.pragmatix.app.model.UserProfile)
 */
@Command(10088)
public class DowngradeWeaponResult implements SecuredResponse {

    public short recipeId;

    public short result;

    public DowngradeWeaponResult() {
    }

    public DowngradeWeaponResult(short recipeId, ShopResultEnum result) {
        this.recipeId = recipeId;
        this.result = (short) result.getType();
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "DowngradeWeaponResult{" +
                "recipeId=" + recipeId +
                ", result=" + ShopResultEnum.valueOf(result) +
                '}';
    }
}
