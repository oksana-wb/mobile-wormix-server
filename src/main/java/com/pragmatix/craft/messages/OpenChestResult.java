package com.pragmatix.craft.messages;

import com.pragmatix.achieve.messages.server.IncreaseAchievementsResult;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.craft.model.CraftItemResult;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.07.12 9:17
 * @see com.pragmatix.craft.cotrollers.CraftController#onOpenChest(OpenChest, UserProfile)
 */
@Command(10102)
public class OpenChestResult extends CraftItemResult implements SecuredResponse {

    @Ignore
    public IncreaseAchievementsResult increaseAchievementsResult;

    public String sessionKey;

    public int medalCount;

    public int mutagenCount;

    public int experience;

    public int battles = 0;

    public int bossToken = 0;

    public int wagerToken = 0;

    public OpenChestResult() {
    }

    public OpenChestResult(ShopResultEnum result) {
        this.result = result;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "OpenChestResult{" +
                "stuffId=" + stuffId +
                ", weaponId=" + weaponId +
                ", weaponCount=" + weaponCount +
                ", moneyCount=" + moneyCount +
                ", rubyCount=" + rubyCount +
                ", medalCount=" + medalCount +
                ", mutagenCount=" + mutagenCount +
                ", experience=" + experience +
                ", battles=" + battles +
                ", bossToken=" + bossToken +
                ", wagerToken=" + wagerToken +
                ", recipeId=" + recipeId +
                ", result=" + result +
                (increaseAchievementsResult != null ? ", " + increaseAchievementsResult : "") +
                '}';
    }
}
