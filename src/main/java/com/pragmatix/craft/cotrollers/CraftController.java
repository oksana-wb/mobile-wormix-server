package com.pragmatix.craft.cotrollers;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.common.Connection;
import com.pragmatix.app.messages.server.AwardGranted;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.craft.messages.*;
import com.pragmatix.craft.model.CraftItemResult;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.sessions.Sessions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 07.07.12 14:26
 */
@Controller
public class CraftController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private CraftService craftService;

    @OnMessage
    public ReagentsForProfile onGetReagentsForProfile(final GetReagentsForProfile msg, final UserProfile userProfile) {
        int[] reagentsForProfile = craftService.getReagentsForProfile(msg.profileId).getValues();
        if(reagentsForProfile != null) {
            return new ReagentsForProfile(reagentsForProfile);
        } else {
            log.warn("reagents not found for profile [{}]", msg.profileId);
            return null;
        }
    }

    @OnMessage
    public UpgradeWeaponResult onUpgradeWeapon(final UpgradeWeapon msg, final UserProfile userProfile) {
        ShopResultEnum result = craftService.upgradeWeapon(userProfile, msg.recipeId);
        return new UpgradeWeaponResult(msg.recipeId, result);
    }

    @OnMessage
    public DowngradeWeaponResult onDowngradeWeapon(final DowngradeWeapon msg, final UserProfile userProfile) {
        ShopResultEnum result = craftService.downgradeWeapon(userProfile, msg.recipeId);
        return new DowngradeWeaponResult(msg.recipeId, result);
    }

    @OnMessage
    public AssembleStuffResult onAssembleStuff(final AssembleStuff msg, final UserProfile userProfile) {
        CraftItemResult craftItemResult = craftService.craftItem(userProfile, msg.recipeId, msg.moneyType);
        return new AssembleStuffResult(craftItemResult.stuffId, msg.recipeId, craftItemResult.result, Sessions.getKey());
    }

    @OnMessage
    public OpenChestResult onOpenChest(final OpenChest msg, final UserProfile userProfile) {
        OpenChestResult openChestResult = craftService.openChest(userProfile, msg.recipeId);
        openChestResult.recipeId = msg.recipeId;
        openChestResult.sessionKey = Sessions.getKey();

        if(openChestResult.increaseAchievementsResult != null) {
            Messages.toUser(openChestResult.increaseAchievementsResult);
            if(!openChestResult.increaseAchievementsResult.awards.isEmpty()){
                AwardGranted awardGranted = new AwardGranted(AwardTypeEnum.ACHIEVE, openChestResult.increaseAchievementsResult.awards, "", Sessions.getKey(userProfile));
                Messages.toUser(awardGranted);
            }
        }

        return openChestResult;
    }

}
