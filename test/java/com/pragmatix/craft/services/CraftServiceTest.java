package com.pragmatix.craft.services;

import com.pragmatix.achieve.messages.server.IncreaseAchievementsResult;
import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.dao.UserProfileDao;
import com.pragmatix.app.init.UserProfileCreator;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.craft.domain.ReagentsEntity;
import com.pragmatix.craft.model.CraftItemResult;
import com.pragmatix.craft.model.Recipe;
import com.pragmatix.craft.model.StuffRecipe;
import com.pragmatix.quest.QuestService;
import com.pragmatix.quest.dao.QuestEntity;
import com.pragmatix.quest.quest04.Data;
import com.pragmatix.testcase.AbstractSpringTest;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.pragmatix.craft.services.CraftService.ParamsEnum.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 20.07.12 14:46
 */
public class CraftServiceTest extends AbstractSpringTest {

    @Resource
    private CraftService craftService;

    @Resource
    TransactionTemplate transactionTemplate;

    @Resource
    UserProfileDao userProfileDao;

    @Resource
    QuestService questService;

    @Test
    public void spiderUpgradeTest() {
        UserProfile profile = getProfile(testerProfileId);
        profile.setLevel(30);
        profile.setRealMoney(100);
        assertTrue(weaponService.addOrUpdateWeapon(profile, 48, 3));
        profile.setRecipes(new short[]{});

        ShopResultEnum shopResultEnum = craftService.upgradeWeapon(profile, (short) 37);
        assertEquals(ShopResultEnum.SUCCESS, shopResultEnum);
        assertArrayEquals(profile.getRecipes(), new short[]{37});

        shopResultEnum = craftService.upgradeWeapon(profile, (short) 121);
        assertEquals(ShopResultEnum.ERROR, shopResultEnum);
        assertArrayEquals(profile.getRecipes(), new short[]{37});

        shopResultEnum = craftService.downgradeWeapon(profile, (short)37);
        assertEquals(ShopResultEnum.SUCCESS, shopResultEnum);

        shopResultEnum = craftService.upgradeWeapon(profile, (short) 121);
        assertEquals(ShopResultEnum.SUCCESS, shopResultEnum);
        assertArrayEquals(profile.getRecipes(), new short[]{121});
    }

    @Test
    public void downgradeWeaponsInaccessibleByLevelTest() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        profile.setLevel(10);
        profile.setRecipes(new short[]{1, 2});

        LoginAwardStructure loginAwardStructure = craftService.downgradeWeaponsInaccessibleByLevel(profile);
        println(loginAwardStructure);

        assertArrayEquals(profile.getRecipes(), new short[]{1});
        assertEquals(AwardKindEnum.REAL_MONEY, loginAwardStructure.awards.get(0).awardKind);
        assertTrue(loginAwardStructure.awards.get(0).count > 0);
    }

    @Test
    public void trackReagentsTest() throws Exception {
        loginMain();
    }

    @Test
    public void consumeIncreaseSeasonWeaponTest() {
        UserProfile profile = getProfile(testerProfileId);
        profile.setMoney(0);
        profile.setRealMoney(0);
        int weaponId = 50;
        int weaponCount = 2;
        QuestEntity questEntity = questService.getQuestEntity(profile);
        questEntity.q4 = new Data();
        println(questService.questsProgress(profile));
        for(int i = 1; i <= 51; i++) {
            IncreaseAchievementsResult increaseAchievementsResult = craftService.consumeIncreaseSeasonWeapon(profile, weaponId, weaponCount);
            if(!increaseAchievementsResult.awards.isEmpty())
                println(i + ": " + questEntity.q4().progress + " " + increaseAchievementsResult.awards + " money=" + profile.getMoney() + " ruby=" + profile.getRealMoney());
        }

        profileService.updateSync(profile);
    }

    @Test
    public void recraftStuffTest() {
        short recipeId = 2560;
        short craftedStuff = 2565;

        UserProfile profile = getProfile(testerProfileId);
        profile.setHat((short) 0);
        profile.setKit((short) 0);
        profile.setStuff(ArrayUtils.EMPTY_SHORT_ARRAY);
        profile.setTemporalStuff(ArrayUtils.EMPTY_BYTE_ARRAY);
        profile.setReagents(new ReagentsEntity(testerProfileId));
        stuffService.addStuff(profile, craftedStuff);

        println("start with progress: " + questService.questsProgress(profile).stream().filter(questProgressStructure -> questProgressStructure.questId == 3).findFirst());

//        inTransaction(() -> questService.wipeQuestsState(profile));
        StuffRecipe recipe = craftService.getStuffRecipes().get(recipeId);

        IntStream.rangeClosed(1, 5).forEach(i -> {
            profile.setRealMoney(recipe.getRecraftRealMoney() + craftService.realMoneyForAbsentReagents(profile.getReagents().getValues(), recipe.getRecraftReagentsMap()));

            CraftItemResult craftItemResult_ = craftService.craftItem(profile, recipeId, MoneyType.REAL_MONEY);
//            assertEquals(ShopResultEnum.SUCCESS, craftItemResult_.result);
//            assertEquals(0, profile.getRealMoney());
//            assertTrue(recipe.getResultsStuffsSet().contains(profile.getStuff()[0]));

            println(questService.questsProgress(profile));
        });

        profileService.updateSync(profile);
    }

    @Test
    public void craftCraftedStuffTest() {
        craftWithResult((short) 1701, ShopResultEnum.MIN_REQUIREMENTS_ERROR);
        craftWithResult((short) 1702, ShopResultEnum.SUCCESS);
        craftWithResult((short) 1703, ShopResultEnum.SUCCESS);
        craftWithResult((short) 1704, ShopResultEnum.SUCCESS);
    }

    private void craftWithResult(short baseCraftedStuff, ShopResultEnum result) {
        short recipeId = 2560;
        short baseStuff = 2023;

        UserProfile profile = getProfile(testerProfileId);
        profile.setHat((short) 0);
        profile.setKit((short) 0);
        profile.setStuff(ArrayUtils.EMPTY_SHORT_ARRAY);
        profile.setTemporalStuff(ArrayUtils.EMPTY_BYTE_ARRAY);
        profile.setReagents(new ReagentsEntity(testerProfileId));
        StuffRecipe recipe = craftService.getStuffRecipes().get(recipeId);

        stuffService.addStuffUntilTime(profile, baseStuff, (int) TimeUnit.HOURS.toSeconds(1), false);
        stuffService.addStuffUntilTime(profile, baseCraftedStuff, (int) TimeUnit.HOURS.toSeconds(1), false);
        profile.setRealMoney(recipe.needRealMoney + craftService.realMoneyForAbsentReagents(profile.getReagents().getValues(), recipe.getReagentsMap()));

        CraftItemResult craftItemResult = craftService.craftItem(profile, recipeId, MoneyType.REAL_MONEY);
        assertEquals(result, craftItemResult.result);
        if(result == ShopResultEnum.SUCCESS) {
            assertEquals(0, profile.getRealMoney());
            assertFalse(stuffService.isExist(profile, baseStuff));
            assertFalse(stuffService.isExist(profile, baseCraftedStuff));
            assertTrue(recipe.getResultsStuffsSet().contains(profile.getStuff()[0]));
        }
    }

    @Test
    public void openCheastTest() {
        CraftService craftService = spy(this.craftService);
        doReturn(4).when(craftService).rollDice((int[][]) any());

        UserProfile profile = profileService.getUserProfile(testerProfileId);
        craftService.setReagentValue(profile, (byte) 51, 1);
        weaponService.addOrUpdateWeapon(profile, 113, craftService.getParamValue(OpenChestWeaponLimit));
        CraftItemResult craftItemResult = craftService.craftItem(profile, (short) 3000, null);
        println(craftItemResult);
        assertTrue(craftItemResult.moneyCount >= craftService.getParamValue(OpenChestInsteadWeaponMoneyFrom));
        assertTrue(craftItemResult.moneyCount <= craftService.getParamValue(OpenChestInsteadWeaponMoneyTo));

        profileService.updateSync(profile);
    }

    @Test
    public void testUpgradeWeapon() {
        final UserProfile profile = getProfile(testerProfileId);
        profile.setRecipes(new short[0]);
        short recipeId = 4;
        Recipe recipe = craftService.getAllRecipesMap().get(recipeId);
        BackpackItem backpackItemByWeaponId = profile.getBackpackItemByWeaponId(recipe.getWeaponId());
        assertNotNull(backpackItemByWeaponId);
        assertEquals(-1, backpackItemByWeaponId.getCount());

        profile.setRealMoney(100);
        ReagentsEntity reagents = new ReagentsEntity(testerProfileId);
        profile.setReagents(reagents);
        int[] values = reagents.getValues();
        values[1] = 10;
        values[2] = 20;
        values[3] = 30;

        ShopResultEnum shopResultEnum = craftService.upgradeWeapon(profile, recipeId);
        assertEquals(ShopResultEnum.SUCCESS, shopResultEnum);

//        assertEquals(100, profile.getRealMoney());
        assertTrue(craftService.recipeIsPresentAndApplied(profile, recipeId));

        System.out.println(profile.getRealMoney());

        shopResultEnum = craftService.upgradeWeapon(profile, (short) 5);
        assertEquals(ShopResultEnum.SUCCESS, shopResultEnum);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                userProfileDao.updateProfile(profile);
            }
        });
    }

    @Test
    public void testSimpleDowngradeWeapon() {
        ShopResultEnum resultEnum;
        UserProfile profile = getProfile(testerProfileId);
        profile.setRecipes(new short[0]);

        craftService.applyRecipe(profile, (short) 1);
        craftService.applyRecipe(profile, (short) 3);
        craftService.applyRecipe(profile, (short) 4);
        craftService.applyRecipe(profile, (short) 5);

        resultEnum = craftService.downgradeWeapon(profile, (short) 1);
        assertEquals(ShopResultEnum.ERROR, resultEnum);

        resultEnum = craftService.downgradeWeapon(profile, (short) 2);
        assertEquals(ShopResultEnum.ERROR, resultEnum);

        resultEnum = craftService.downgradeWeapon(profile, (short) 3);
        assertEquals(ShopResultEnum.SUCCESS, resultEnum);
        assertTrue(craftService.recipeIsPresentAndNotApplied(profile, (short) 3));
        assertTrue(craftService.recipeIsPresentAndApplied(profile, (short) 1));

        System.out.println(profile.getReagents());
        System.out.println(Arrays.toString(profile.getRecipes()));
    }

    @Test
    public void testDowngradeWeapon() {
        ShopResultEnum resultEnum;
        UserProfile profile = getProfile(testerProfileId);
        short[] recipes = profile.getRecipes();
        for(int i = 0; i < recipes.length; i++) {
            recipes[i] = (short) -Math.abs(recipes[i]);
        }
        craftService.applyRecipe(profile, (short) 1);
        craftService.applyRecipe(profile, (short) 2);

        ReagentsEntity reagents = new ReagentsEntity(testerProfileId);
        profile.setReagents(reagents);
        profile.setRealMoney(0);

        resultEnum = craftService.downgradeWeapon(profile, (short) 1);
        assertEquals(ShopResultEnum.ERROR, resultEnum);
        assertEquals(0, profile.getRealMoney());

        profile.setRealMoney(0);
        resultEnum = craftService.downgradeWeapon(profile, (short) 2);
        assertEquals(ShopResultEnum.NOT_ENOUGH_MONEY, resultEnum);

        profile.setRealMoney(0);
        resultEnum = craftService.downgradeWeapon(profile, (short) 2);
        assertEquals(ShopResultEnum.SUCCESS, resultEnum);
        assertEquals(0, profile.getRealMoney());
        assertTrue(craftService.recipeIsPresentAndNotApplied(profile, (short) 2));
        assertTrue(craftService.recipeIsPresentAndApplied(profile, (short) 1));

        System.out.println(profile.getReagents());

    }

    @Test
    public void testUpgradeWeaponOnSameLevel() {
        ShopResultEnum resultEnum;
        UserProfile profile = getProfile(testerProfileId);
        short[] recipes = profile.getRecipes();
        for(int i = 0; i < recipes.length; i++) {
            recipes[i] = (short) -Math.abs(recipes[i]);
        }

        profile.setLevel(30);
        profile.setRealMoney(1000);

        ReagentsEntity reagents = new ReagentsEntity(testerProfileId);
        profile.setReagents(reagents);

        BackpackItem backpackItem = new BackpackItem(5);
        backpackItem.setCount((short) -1);
        profile.addBackpackItem(backpackItem);

        resultEnum = craftService.upgradeWeapon(profile, (short) 1);
        assertEquals(ShopResultEnum.SUCCESS, resultEnum);

        resultEnum = craftService.upgradeWeapon(profile, (short) 2);
        assertEquals(ShopResultEnum.SUCCESS, resultEnum);

        resultEnum = craftService.upgradeWeapon(profile, (short) 3);
        assertEquals(ShopResultEnum.ERROR, resultEnum);

        resultEnum = craftService.downgradeWeapon(profile, (short) 2);
        assertEquals(ShopResultEnum.SUCCESS, resultEnum);

        resultEnum = craftService.upgradeWeapon(profile, (short) 3);
        assertEquals(ShopResultEnum.SUCCESS, resultEnum);

        System.out.println(Arrays.toString(profile.getRecipes()));

    }

    @Resource
    private ProfileService profileService;

    @Resource
    private UserProfileCreator userProfileCreator;

    @Test
    public void testWipe() {
        ShopResultEnum resultEnum;
        final UserProfile profile = getProfile(testerProfileId);
        short[] recipes = profile.getRecipes();
        for(int i = 0; i < recipes.length; i++) {
            recipes[i] = (short) -Math.abs(recipes[i]);
        }


        profile.setLevel(30);
        profile.setRealMoney(1000);

        craftService.getReagentsForProfile(testerProfileId);
        profile.getReagents().clean();

        BackpackItem backpackItem = new BackpackItem(5);
        backpackItem.setCount((short) -1);
        profile.addBackpackItem(backpackItem);

        resultEnum = craftService.upgradeWeapon(profile, (short) 1);
        assertEquals(ShopResultEnum.SUCCESS, resultEnum);

        resultEnum = craftService.upgradeWeapon(profile, (short) 2);
        assertEquals(ShopResultEnum.SUCCESS, resultEnum);

        resultEnum = craftService.downgradeWeapon(profile, (short) 2);
        assertEquals(ShopResultEnum.SUCCESS, resultEnum);

        profileService.updateSync(profile);

        softCache.remove(UserProfile.class, profile.getId());

        System.out.println(Arrays.toString(profile.getRecipes()));
        System.out.println(profile.getReagents());

        final UserProfile profileReloaded = getProfile(testerProfileId);
        craftService.getReagentsForProfile(testerProfileId);

        System.out.println(Arrays.toString(profileReloaded.getRecipes()));
        System.out.println(profileReloaded.getReagents());

        assertTrue(craftService.recipeIsPresentAndApplied(profileReloaded, (short) 1));
        assertTrue(craftService.recipeIsPresentAndNotApplied(profileReloaded, (short) 2));

        userProfileCreator.wipeUserProfile(profileReloaded);

        profileService.updateSync(profile);

        final UserProfile profileWiped = getProfile(testerProfileId);
        craftService.getReagentsForProfile(testerProfileId);

        System.out.println(Arrays.toString(profileWiped.getRecipes()));
        System.out.println(profileWiped.getReagents());

        assertTrue(craftService.recipeIsPresentAndNotApplied(profileWiped, (short) 1));
        assertTrue(craftService.recipeIsPresentAndNotApplied(profileWiped, (short) 2));
    }
}
