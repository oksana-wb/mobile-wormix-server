package com.pragmatix.app.services;

import com.pragmatix.app.messages.client.BuyShopItems;
import com.pragmatix.app.messages.server.ShopResult;
import com.pragmatix.app.messages.server.StuffExpired;
import com.pragmatix.app.messages.structures.ShopItemStructure;
import com.pragmatix.app.messages.structures.TemporalStuffStructure;
import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 13.11.13 18:11
 */
public class TemporalStuffServiceTest extends AbstractSpringTest {

    @Resource
    private StuffService stuffService;

    @Resource
    private TemporalStuffService temporalStuffService;

    @Test
    public void testRemoveExpiredStuff() throws Exception {
        UserProfile profile = new UserProfile(1L);
        profile.setProfileStringId("");
        profile.setTemporalStuff(new byte[0]);
        TemporalStuffService temporalStuffService = new TemporalStuffService();
        short itemId1 = (short) 1001;
        short itemId2 = (short) 1002;

        temporalStuffService.addStuffFor(profile, itemId1, -1, TimeUnit.MINUTES);
        temporalStuffService.addStuffFor(profile, itemId2, 2, TimeUnit.MINUTES);
        System.out.println(new Date());
        System.out.println(TemporalStuffService.toStringTemporalStuff(profile.getTemporalStuff()));

        profile.setTemporalStuff(temporalStuffService.removeExpiredStuff(profile.getTemporalStuff(), null));
        System.out.println(TemporalStuffService.toStringTemporalStuff(profile.getTemporalStuff()));

        temporalStuffService.removeStuffFor(profile, itemId2);
        System.out.println(TemporalStuffService.toStringTemporalStuff(profile.getTemporalStuff()));
    }

    @Test
    public void testExtendTempStuff() throws Exception {
        UserProfile profile = new UserProfile(1L);
        profile.setProfileStringId("");
        profile.setTemporalStuff(new byte[0]);
        short itemId1 = (short) 3001;

        stuffService.addStuff(profile, itemId1, 1, TimeUnit.MINUTES, false);
        System.out.println(TemporalStuffService.toStringTemporalStuff(profile.getTemporalStuff()));
        stuffService.addStuff(profile, itemId1, 10, TimeUnit.MINUTES, false);
        System.out.println(TemporalStuffService.toStringTemporalStuff(profile.getTemporalStuff()));
    }

    @Test
    public void testShopingStuff() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        profile.setTemporalStuff(new byte[0]);
        int startMoney = 100;
        Stuff stuff = stuffService.getStuff((short) 3001);
        assertNotNull(stuff);

        loginMain();

        ShopResult shopResult;
        profile.setMoney(startMoney);

        sendMain(new BuyShopItems(new ShopItemStructure(stuff.getStuffId(), 1, MoneyType.MONEY.getType())));
        shopResult = receiveMain(ShopResult.class);
        assertEquals(ShopResultEnum.SUCCESS, shopResult.result);
        assertEquals(startMoney - stuff.getPrice(), profile.getMoney());

        TemporalStuffStructure temporalStuffStructure = shopResult.temporalStuff.get(0);

        assertTrue(stuffService.isExist(profile, stuff.getStuffId()));
        assertEquals(temporalStuffStructure.expireDate * 1000L, temporalStuffService.getExpireDate(profile, stuff.getStuffId()));

        sendMain(new BuyShopItems(new ShopItemStructure(stuff.getStuffId(), 1, MoneyType.MONEY.getType())));
        shopResult = receiveMain(ShopResult.class);
        assertEquals(ShopResultEnum.SUCCESS, shopResult.result);
        assertEquals(startMoney - stuff.getPrice(), profile.getMoney());
        assertEquals(0, shopResult.temporalStuff.size());
    }

    @Test
    public void testExpireStuff() throws Exception {
        int startMoney = 10000;
        Stuff permatentHat = stuffService.getStuff((short) 1000);
        Stuff temporalHat = stuffService.getStuff((short) 3001);
        assertNotNull(permatentHat);
        assertNotNull(temporalHat);

        loginMain();

        ShopResult shopResult;
        UserProfile profile = getProfile(testerProfileId);
        profile.setTemporalStuff(new byte[0]);
        profile.setStuff(new short[0]);
        profile.setLevel(30);
        profile.setHat((short) 0);
        profile.setKit((short) 0);
        profile.setMoney(startMoney);

        // покупаем обычную шапку
        sendMain(new BuyShopItems(new ShopItemStructure(permatentHat.getStuffId(), 1, MoneyType.MONEY.getType())));
        shopResult = receiveMain(ShopResult.class);
        assertEquals(ShopResultEnum.SUCCESS, shopResult.result);
        assertEquals(startMoney - permatentHat.getPrice(), profile.getMoney());
        assertEquals(permatentHat.getStuffId().shortValue(), profile.getHat());

        // покупаем временную шапку
        sendMain(new BuyShopItems(new ShopItemStructure(temporalHat.getStuffId(), 1, MoneyType.MONEY.getType())));
        shopResult = receiveMain(ShopResult.class);
        assertEquals(ShopResultEnum.SUCCESS, shopResult.result);
        assertEquals(startMoney - temporalHat.getPrice() - permatentHat.getPrice(), profile.getMoney());

        TemporalStuffStructure temporalStuffStructure = shopResult.temporalStuff.get(0);

        assertTrue(stuffService.isExist(profile, temporalHat.getStuffId()));
        assertEquals(temporalStuffStructure.expireDate * 1000L, temporalStuffService.getExpireDate(profile, temporalHat.getStuffId()));
        assertEquals(temporalHat.getStuffId().shortValue(), profile.getHat());

        // ждем пока истечет время шапки
        StuffExpired stuffExpired = receiveMain(StuffExpired.class, 120 * 1000);
        assertArrayEquals(new Short[]{temporalHat.getStuffId()}, stuffExpired.stuff.toArray(new Short[0]));

        // временной нет
        System.out.println(TemporalStuffService.toStringTemporalStuff(profile.getTemporalStuff()));
        assertFalse(stuffService.isExist(profile, temporalHat.getStuffId()));
        assertEquals(0, profile.getTemporalStuff().length);

        // вернулвсь постоянная
        assertEquals(permatentHat.getStuffId().shortValue(), profile.getHat());
    }

}
