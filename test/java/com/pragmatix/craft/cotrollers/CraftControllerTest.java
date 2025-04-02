package com.pragmatix.craft.cotrollers;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.craft.domain.Reagent;
import com.pragmatix.craft.messages.GetReagentsForProfile;
import com.pragmatix.craft.messages.OpenChest;
import com.pragmatix.craft.messages.ReagentsForProfile;
import com.pragmatix.craft.model.CraftItemResult;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 07.07.12 15:52
 */
public class CraftControllerTest extends AbstractSpringTest {

    @Test
    public void testOnGetReagentsForProfile() throws Exception {
        loginMain();
        sendMain(new GetReagentsForProfile(testerProfileId));
        receiveMain(ReagentsForProfile.class, 500);
        disconnectMain();
        Thread.sleep(1000);
    }

    @Resource
    private CraftService craftService;

    @Test
    public void successOpenCheast() {
        UserProfile profile = profileService.getUserProfile(testerProfileId);
        craftService.getReagentsForProfile(testerProfileId);
        profile.getReagents().setReagentValue(Reagent.prize_key, 1);

        OpenChest msg = new OpenChest();
        msg.recipeId = 3000;
        CraftItemResult craftItemResult = craftService.craftItem(profile, msg.recipeId, null);
        Assert.assertEquals(ShopResultEnum.SUCCESS, craftItemResult.result);
        Assert.assertEquals(0, profile.getReagents().getReagentValue(Reagent.prize_key));
    }

}
