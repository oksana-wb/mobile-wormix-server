package com.pragmatix.craft.messages;

import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.07.12 9:17
 *
 * @see com.pragmatix.craft.cotrollers.CraftController#onUpgradeWeapon(com.pragmatix.craft.messages.UpgradeWeapon, com.pragmatix.app.model.UserProfile)
 */
@Command(10099)
public class AssembleStuffResult implements SecuredResponse {

    public short resultStuffId;

    public short recipeId;

    public ShopResultEnum result;

    public String sessionKey;

    public AssembleStuffResult() {
    }

    public AssembleStuffResult(short resultStuffId, short recipeId, ShopResultEnum result, String sessionKey) {
        this.resultStuffId = resultStuffId;
        this.recipeId = recipeId;
        this.result = result;
        this.sessionKey = sessionKey;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "AssembleStuffResult{" +
                "resultStuffId=" + resultStuffId +
                ", recipeId=" + recipeId +
                ", result=" + result +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }

}
