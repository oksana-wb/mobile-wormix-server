package com.pragmatix.craft.messages;

import com.pragmatix.app.common.MoneyType;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.07.12 9:14
 *
 * @see com.pragmatix.craft.cotrollers.CraftController#onAssembleStuff(AssembleStuff, com.pragmatix.app.model.UserProfile)
 */
@Command(99)
public class AssembleStuff {

    public short recipeId;

    /**
     * тип денег при покупке
     */
    public MoneyType moneyType;

    @Override
    public String toString() {
        return "AssembleStuff{" +
                "recipeId=" + recipeId +
                ", moneyType=" + moneyType +
                '}';
    }

}
