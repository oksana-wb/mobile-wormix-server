package com.pragmatix.app.services;

import com.pragmatix.app.common.BoostFamily;
import com.pragmatix.app.messages.structures.BundleStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.testcase.AbstractSpringTest;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.pragmatix.app.services.TemporalStuffService.toStringTemporalStuff;
import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 13.11.13 18:11
 */
public class BoostStuffServiceTest extends AbstractSpringTest {

    @Resource
    private StuffService stuffService;

    @Resource
    private TemporalStuffService temporalStuffService;

    @Resource
    private BundleService bundleService;

    @Test
    public void issueBundleTest() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        profile.setTemporalStuff(new byte[0]);

        BundleStructure bundle = bundleService.getValidBundle("80");
        assertNotNull(bundle);
        bundleService.issueBundle(profile, bundle);
        println(toStringTemporalStuff(profile.getTemporalStuff()));

        Stream.of(3020, 3021, 3022, 3023).forEach(boosterId -> {
            assertTrue(stuffService.isExist(profile, boosterId.shortValue()));
            assertEquals(AppUtils.currentTimeSeconds() + TimeUnit.DAYS.toSeconds(1), temporalStuffService.getExpireDateInSeconds(profile, boosterId.shortValue()));
        });

        bundle = bundleService.getValidBundle("82");
        assertNotNull(bundle);
        bundleService.issueBundle(profile, bundle);
        println(toStringTemporalStuff(profile.getTemporalStuff()));

        Stream.of(3020, 3021, 3022, 3023).forEach(boosterId -> {
            assertTrue(stuffService.isExist(profile, boosterId.shortValue()));
            assertEquals(AppUtils.currentTimeSeconds() + TimeUnit.DAYS.toSeconds(1 + 7), temporalStuffService.getExpireDateInSeconds(profile, boosterId.shortValue()));
        });

    }

    @Test
    public void testAddAndExtendBoost() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        profile.setTemporalStuff(new byte[0]);

        int boostId_1_1 = 3001;
        int boostId_1_2 = 3021;

        addNewBuster(profile, boostId_1_1);
        extendPresentBuster(profile, boostId_1_1, boostId_1_2);

        int boostId_3_1 = 3011;
        int boostId_3_2 = 3023;

        addNewBuster(profile, boostId_3_1);
        extendPresentBuster(profile, boostId_3_1, boostId_3_2);
    }

    public void addNewBuster(UserProfile profile, int boosterId) {
        int expireTimeInSeconds = stuffService.addBooster(profile, (short) boosterId);
        println(toStringTemporalStuff(profile.getTemporalStuff()));

        assertTrue(stuffService.isExist(profile, (short) boosterId));
        assertEquals(AppUtils.currentTimeSeconds() + stuffService.getStuff(boosterId).getExpireTimeInSeconds(), temporalStuffService.getExpireDateInSeconds(profile, (short) boosterId));
        assertEquals(expireTimeInSeconds, temporalStuffService.getExpireDateInSeconds(profile, (short) boosterId));
    }

    public void extendPresentBuster(UserProfile profile, int presentBoosterId, int newBoosterId) {
        int fromDate = temporalStuffService.getExpireDateInSeconds(profile, (short) presentBoosterId);

        int expireTimeInSeconds = stuffService.addBooster(profile, (short) newBoosterId);
        println(toStringTemporalStuff(profile.getTemporalStuff()));

        assertFalse(stuffService.isExist(profile, (short) presentBoosterId));
        assertTrue(stuffService.isExist(profile, (short) newBoosterId));

        assertEquals(fromDate + stuffService.getStuff(newBoosterId).getExpireTimeInSeconds(), temporalStuffService.getExpireDateInSeconds(profile, (short) newBoosterId));
        assertEquals(expireTimeInSeconds, temporalStuffService.getExpireDateInSeconds(profile, (short) newBoosterId));
    }

//    @Test
//    public void testBoostValue() throws Exception {
//        UserProfile profile = getProfile(testerProfileId);
//        profile.setTemporalStuff(new byte[0]);
//        short boostId_1_1 = (short) 3001;
//        short boostId_1_2 = (short) 3002;
//        short boostId_1_3 = (short) 3003;
//        short boostId_2_1 = (short) 3004;
//        short boostId_2_2 = (short) 3005;
//        short boostId_2_3 = (short) 3006;
//
//        addSuccess(profile, boostId_1_1, new short[0]);
//        assertEquals(2, stuffService.getBoostValue(profile, BoostFamily.MultiplyExperience));
//
//        addSuccess(profile, boostId_2_1, new short[0]);
//        assertEquals(4, stuffService.getBoostValue(profile, BoostFamily.MultiplyExperience));
//
//        addSuccess(profile, boostId_1_2, new short[0]);
//        assertEquals(2, stuffService.getBoostValue(profile, BoostFamily.MultiplyExperience));
//
//        addSuccess(profile, boostId_1_3, new short[0]);
//        assertEquals(2, stuffService.getBoostValue(profile, BoostFamily.MultiplyExperience));
//
//        addSuccess(profile, boostId_2_2, new short[0]);
//        assertEquals(4, stuffService.getBoostValue(profile, BoostFamily.MultiplyExperience));
//
//        addSuccess(profile, boostId_2_3, new short[0]);
//        assertEquals(4, stuffService.getBoostValue(profile, BoostFamily.MultiplyExperience));
//    }
//
//    public void addSuccess(UserProfile profile, short boostId, short[] allFamily) {
//        stuffService.addBooster(profile, stuffService.getStuff(boostId));
//        assertTrue(stuffService.isExist(profile, boostId));
//        for(short stuffId : ArrayUtils.removeElement(allFamily, boostId)) {
//            assertFalse(stuffService.isExist(profile, stuffId));
//        }
//    }
//
//    public void addFailure(UserProfile profile, short boostId) {
//        stuffService.addBooster(profile, stuffService.getStuff(boostId));
//        assertFalse(stuffService.isExist(profile, boostId));
//    }

//    @Test
//    public void testExtendTempStuff() throws Exception {
//        UserProfile profile = new UserProfile();
//        profile.setId(1l);
//        profile.setProfileStringId("");
//        profile.setTemporalStuff(new byte[0]);
//        short itemId1 = (short) 3001;
//
//        stuffService.addStuff(profile, itemId1, 1, TimeUnit.MINUTES, false);
//        System.out.println(TemporalStuffService.toStringTemporalStuff(profile.getTemporalStuff()));
//        stuffService.addStuff(profile, itemId1, 10, TimeUnit.MINUTES, false);
//        System.out.println(TemporalStuffService.toStringTemporalStuff(profile.getTemporalStuff()));
//    }
//
//    @Test
//    public void testShopingStuff() throws Exception {
//        UserProfile profile = getProfile(testerProfileId);
//        profile.setTemporalStuff(new byte[0]);
//        int startMoney = 100;
//        Stuff stuff = stuffService.getStuff((short) 3001);
//        assertNotNull(stuff);
//
//        loginMain();
//
//        ShopResult shopResult;
//        profile.setMoney(startMoney);
//
//        sendMain(new BuyShopItems(new ShopItemStructure(stuff.getStuffId(), 1, MoneyType.MONEY.getType())));
//        shopResult = receiveMain(ShopResult.class);
//        assertEquals(ShopResultEnum.SUCCESS.getType(), shopResult.result);
//        assertEquals(startMoney - stuff.getPrice(), profile.getMoney());
//
//        TemporalStuffStructure temporalStuffStructure = shopResult.temporalStuff[0];
//
//        assertTrue(stuffService.isExist(profile, stuff.getStuffId()));
//        assertEquals(temporalStuffStructure.expireDate * 1000l, temporalStuffService.getExpireDate(profile, stuff.getStuffId()));
//
//        sendMain(new BuyShopItems(new ShopItemStructure(stuff.getStuffId(), 1, MoneyType.MONEY.getType())));
//        shopResult = receiveMain(ShopResult.class);
//        assertEquals(ShopResultEnum.SUCCESS.getType(), shopResult.result);
//        assertEquals(startMoney - stuff.getPrice(), profile.getMoney());
//        assertEquals(0, shopResult.temporalStuff.length);
//    }
//
//    @Test
//    public void testExpireStuff() throws Exception {
//        int startMoney = 10000;
//        Stuff permatentHat = stuffService.getStuff((short) 1000);
//        Stuff temporalHat = stuffService.getStuff((short) 3001);
//        assertNotNull(permatentHat);
//        assertNotNull(temporalHat);
//
//        loginMain();
//
//        ShopResult shopResult;
//        UserProfile profile = getProfile(testerProfileId);
//        profile.setTemporalStuff(new byte[0]);
//        profile.setStuff(new short[0]);
//        profile.setLevel(30);
//        profile.setHat((short) 0);
//        profile.setKit((short) 0);
//        profile.setMoney(startMoney);
//
//        // покупаем обычную шапку
//        sendMain(new BuyShopItems(new ShopItemStructure(permatentHat.getStuffId(), 1, MoneyType.MONEY.getType())));
//        shopResult = receiveMain(ShopResult.class);
//        assertEquals(ShopResultEnum.SUCCESS.getType(), shopResult.result);
//        assertEquals(startMoney - permatentHat.getPrice(), profile.getMoney());
//        assertEquals(permatentHat.getStuffId().shortValue(), profile.getHat());
//
//        // покупаем временную шапку
//        sendMain(new BuyShopItems(new ShopItemStructure(temporalHat.getStuffId(), 1, MoneyType.MONEY.getType())));
//        shopResult = receiveMain(ShopResult.class);
//        assertEquals(ShopResultEnum.SUCCESS.getType(), shopResult.result);
//        assertEquals(startMoney - temporalHat.getPrice() - permatentHat.getPrice(), profile.getMoney());
//
//        TemporalStuffStructure temporalStuffStructure = shopResult.temporalStuff[0];
//
//        assertTrue(stuffService.isExist(profile, temporalHat.getStuffId()));
//        assertEquals(temporalStuffStructure.expireDate * 1000l, temporalStuffService.getExpireDate(profile, temporalHat.getStuffId()));
//        assertEquals(temporalHat.getStuffId().shortValue(), profile.getHat());
//
//        // ждем пока истечет время шапки
//        StuffExpired stuffExpired = receiveMain(StuffExpired.class, 120 * 1000);
//        assertTrue(Arrays.equals(new short[]{temporalHat.getStuffId()}, stuffExpired.stuff));
//
//        // временной нет
//        System.out.println(TemporalStuffService.toStringTemporalStuff(profile.getTemporalStuff()));
//        assertFalse(stuffService.isExist(profile, temporalHat.getStuffId()));
//        assertEquals(0, profile.getTemporalStuff().length);
//
//        // вернулвсь постоянная
//        assertEquals(permatentHat.getStuffId().shortValue(), profile.getHat());
//    }

}
