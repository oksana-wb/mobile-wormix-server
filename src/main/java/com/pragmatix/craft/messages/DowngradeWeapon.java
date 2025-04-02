package com.pragmatix.craft.messages;

import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.07.12 9:14
 *
 * @see com.pragmatix.craft.cotrollers.CraftController#onDowngradeWeapon(DowngradeWeapon, com.pragmatix.app.model.UserProfile)
 */
@Command(88)
public class DowngradeWeapon {

    public short recipeId;

    @Override
    public String toString() {
        return "DowngradeWeapon{" +
                "recipeId=" + recipeId +
                '}';
    }

}
