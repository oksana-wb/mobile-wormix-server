package com.pragmatix.craft.model;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.serialization.annotations.Structure;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.07.12 9:17
 *
 * @see com.pragmatix.craft.services.CraftService#craftItem(UserProfile, short, MoneyType)
 */
@Structure(isAbstract = true)
public class CraftItemResult {

    public short stuffId;

    public int weaponId;

    public int weaponCount;

    public int moneyCount;

    public int rubyCount;

    public short recipeId;

    public ShopResultEnum result;

    protected CraftItemResult() {
    }

    public CraftItemResult(ShopResultEnum result) {
        this.result = result;
    }

}
