package com.pragmatix.craft.services;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.StuffService;
import com.pragmatix.app.services.WeaponService;
import com.pragmatix.common.utils.VarObject;
import com.pragmatix.craft.model.CraftItemResult;
import com.pragmatix.craft.model.StuffRecipe;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 10.07.13 12:40
 */
public class StuffCraftTest extends AbstractSpringTest {

    @Resource
    private CraftService craftService;

    @Resource
    private StuffService stuffService;

    @Resource
    private WeaponService weaponService;

    @Test
    public void openChestTest() {
        short recipeId = 3000;

        UserProfile profile = getProfile(testerProfileId);

        for(int i = 0; i < 10; i++) {
            CraftItemResult craftItemResult = openChest(recipeId, profile, true);
            System.out.println(String.format("=> stuffId:%s, weaponId:%s(%s), rubyCount:%s", craftItemResult.stuffId, craftItemResult.weaponId, craftItemResult.weaponCount, craftItemResult.rubyCount));
        }

    }

    @Test
    public void assembleTest() {
        short recipeId = 1500;

        UserProfile profile = getProfile(testerProfileId);

        VarObject<Integer> resultStuffId = new VarObject<>(0);
        assembleStuff(recipeId, profile, resultStuffId, true);

        System.out.println("=>" + resultStuffId.value);

        CraftItemResult craftItemResult = craftService.craftItem(profile, recipeId, MoneyType.MONEY);
        assertEquals(ShopResultEnum.MIN_REQUIREMENTS_ERROR, craftItemResult.result);
    }

    @Test
    public void reassembleTest() {
        short recipeId = 1500;

        UserProfile profile = getProfile(testerProfileId);

        VarObject<Integer> resultStuffId = new VarObject<>(0);
        assembleStuff(recipeId, profile, resultStuffId, true);
        int resultStuffId1 = resultStuffId.value;
        System.out.println("=>" + resultStuffId1 + " " + Arrays.toString(profile.getStuff()));

        assembleStuff(recipeId, profile, resultStuffId, false);
        int resultStuffId2 = resultStuffId.value;
        System.out.println("=>" + resultStuffId2 + " " + Arrays.toString(profile.getStuff()));

        assertTrue(resultStuffId1 != resultStuffId2);
    }

    private void assembleStuff(short recipeId, UserProfile profile, VarObject<Integer> resultStuffId, boolean cleanStuffs) {
        StuffRecipe recipe = craftService.getStuffRecipes().get(recipeId);

        if(cleanStuffs) profile.setStuff(new short[0]);
        for(Short stuffId : recipe.getBaseStuffsSet()) {
            stuffService.addStuff(profile, stuffId);
        }
        craftService.wipeReagents(profile);
        for(Map.Entry<Byte, Integer> entry : recipe.getReagentsMap().entrySet()) {
            craftService.setReagentValue(profile, entry.getKey(), entry.getValue());
        }
        profile.setMoney(recipe.needMoney());
//        profile.setRealMoney(recipe.needRealMoney());

        profile.setLevel(recipe.needLevel());

        CraftItemResult craftItemResult = craftService.craftItem(profile, recipeId, MoneyType.MONEY);

        assertEquals(ShopResultEnum.SUCCESS, craftItemResult.result);
        assertEquals(0, profile.getMoney());
//        assertEquals(0, profile.getRealMoney());
        assertEquals(1, profile.getStuff().length);
        assertTrue(stuffService.isExist(profile, craftItemResult.stuffId));
        assertTrue(noReagents(profile));

        resultStuffId.value = (int) craftItemResult.stuffId;
    }

    private CraftItemResult openChest(short recipeId, UserProfile profile, boolean cleanStuffs) {
        StuffRecipe recipe = craftService.getStuffRecipes().get(recipeId);

        if(cleanStuffs) profile.setStuff(new short[0]);
        for(Short stuffId : recipe.getBaseStuffsSet()) {
            stuffService.addStuff(profile, stuffId);
        }
        craftService.wipeReagents(profile);
        for(Map.Entry<Byte, Integer> entry : recipe.getReagentsMap().entrySet()) {
            craftService.setReagentValue(profile, entry.getKey(), entry.getValue());
        }
        profile.setMoney(recipe.needMoney());
        profile.setRealMoney(recipe.needRealMoney());

        profile.setLevel(recipe.needLevel());

        CraftItemResult craftItemResult = craftService.craftItem(profile, recipeId, null);

        assertEquals(ShopResultEnum.SUCCESS, craftItemResult.result);
        assertEquals(0, profile.getMoney());
        assertEquals(profile.getRealMoney(), craftItemResult.rubyCount);
        assertTrue(noReagents(profile));

        if(craftItemResult.stuffId > 0) {
            assertTrue(stuffService.isExist(profile, craftItemResult.stuffId));
        } else if(craftItemResult.weaponId > 0) {
//            weaponService.isPresentShots(profile, craftItemResult.weaponId);
        }

        return craftItemResult;
    }

    private boolean noReagents(UserProfile profile) {
        for(int value : profile.getReagents().getValues()) {
            if(value > 0) return false;
        }
        return true;
    }

}
