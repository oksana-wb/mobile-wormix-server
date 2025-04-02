package com.pragmatix.app.services;

import com.pragmatix.app.common.ItemCheck;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.init.StuffCreator;
import com.pragmatix.app.messages.server.GetSpecialDealResponse;
import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SpecialDealServiceTest extends AbstractSpringTest {

    @Resource
    SpecialDealService specialDealService;

    @Resource
    PaymentService paymentService;

    @Resource
    WeaponService weaponService;

    @Resource
    StuffCreator stuffCreator;

    UserProfile profile;

    @Before
    public void init() {
        profile = getProfile(testerProfileId);
    }

    @Test
    public void fillSpecialDealItems() {
        String items = stuffCreator.getStuffs().stream()
                .filter(s -> ItemCheck.hasRealPrice(s) && s.getRealprice() > 30 && !s.isTemporal())
                .filter(s -> (s.getStuffId() >= 1000 && s.getStuffId() < 1500) || (s.getStuffId() >= 2000 && s.getStuffId() < 2500))
                .sorted(Comparator.comparingInt(Stuff::getStuffId))
                .map(s -> String.format("        <value>%s</value><!-- %s (%s)  -->", s.getStuffId(), s.getName(), s.getRealprice()))
                .collect(Collectors.joining("\n"));
        println(items);
    }

    @Test
    public void GetSpecialDealResponse_Success() {
        GetSpecialDealResponse response = getSpecialDealResponse_Success();

        System.out.println(response);
    }

    @Test
    public void GetSpecialDealResponse_Failure_Donat() {
        paymentService.getDonaters().add((int) testerProfileId);

        weaponService.wipeBackpackConf(profile);

        setNeedRealmoney();

        GetSpecialDealResponse response = specialDealService.getSpecialDeal(profile);
        assertEquals(0, response.itemId);
        assertEquals(0, response.rubyPrice);
    }

    @Test
    public void GetSpecialDealResponse_Failure_MakedPaymentTiday() {
        paymentService.getDonaters().clear();
        weaponService.wipeBackpackConf(profile);
        dailyRegistry.makePayment(profile.getId());

        setNeedRealmoney();

        GetSpecialDealResponse response = specialDealService.getSpecialDeal(profile);
        assertEquals(0, response.itemId);
        assertEquals(0, response.rubyPrice);
    }

    @Test
    public void GetSpecialDealResponse_Failure_HaveWeapons() {
        paymentService.getDonaters().clear();
        for(Integer itemId : specialDealService.getSpecialDealItems()) {
            if(ItemCheck.isWeapon(itemId)) {
                weaponService.addOrUpdateWeapon(profile, itemId, -1);
            } else {
                stuffService.addStuff(profile, itemId.shortValue());
            }
        }

        setNeedRealmoney();

        GetSpecialDealResponse response = specialDealService.getSpecialDeal(profile);
        assertEquals(0, response.itemId);
        assertEquals(0, response.rubyPrice);
    }

    @Test
    public void GetSpecialDealResponse_Failure_NoRealmoney() {
        paymentService.getDonaters().clear();
        weaponService.wipeBackpackConf(profile);

        profile.setRealMoney(0);

        GetSpecialDealResponse response = specialDealService.getSpecialDeal(profile);
        assertEquals(0, response.itemId);
        assertEquals(0, response.rubyPrice);
    }

    @Test
    public void GetSpecialDealResponse_Failure_TwiceADay() {
        ExecuteSpecialDealResponse_Success();

        paymentService.getDonaters().clear();
        weaponService.wipeBackpackConf(profile);
        setNeedRealmoney();

        GetSpecialDealResponse response = specialDealService.getSpecialDeal(profile);
        assertEquals(0, response.itemId);
        assertEquals(0, response.rubyPrice);
    }

    @Test
    public void ExecuteSpecialDealResponse_Success() {
        GetSpecialDealResponse response = getSpecialDealResponse_Success();

        println(response);

        profile.setRealMoney(response.rubyPrice);
        dailyRegistry.makePayment(profile.getId());

        executeSpecialDealResponse_Success(response);
    }

    @Test
    public void ExecuteSpecialDealResponse_Failure_WrongWeaponId() throws InterruptedException {
        GetSpecialDealResponse response = getSpecialDealResponse_Success();

        profile.setRealMoney(response.rubyPrice);
        response.itemId = 99;

        executeSpecialDealResponse_Failure(response);
    }

    public void executeSpecialDealResponse_Success(GetSpecialDealResponse response) {
        ShopResultEnum resultEnum = specialDealService.executeSpecialDeal(profile, response.itemId, response.rubyPrice);
        assertEquals(ShopResultEnum.SUCCESS, resultEnum);
        assertEquals(0, profile.getRealMoney());
        if(ItemCheck.isWeapon(response.itemId)) {
            assertTrue(weaponService.isPresentInfinitely(profile, response.itemId));
        } else {
            assertTrue(stuffService.isExist(profile, response.itemId));
        }
    }

    public void executeSpecialDealResponse_Failure(GetSpecialDealResponse response) {
        ShopResultEnum resultEnum = specialDealService.executeSpecialDeal(profile, response.itemId, response.rubyPrice);
//        assertNotEquals(ShopResultEnum.SUCCESS, resultEnum);
//        assertNotEquals(0, profile.getRealMoney());
        assertFalse(weaponService.isPresentInfinitely(profile, response.itemId));
    }

    public GetSpecialDealResponse getSpecialDealResponse_Success() {
        paymentService.getDonaters().clear();
        weaponService.wipeBackpackConf(profile);
        profile.setStuff(new short[0]);

        setNeedRealmoney();

        GetSpecialDealResponse response = specialDealService.getSpecialDeal(profile);
        assertTrue(response.itemId > 0);
        assertTrue(response.rubyPrice > profile.getRealMoney());

        return response;
    }

    public void setNeedRealmoney() {
        List<Integer> list = new ArrayList<>();
        for(Integer itemId : specialDealService.getSpecialDealItems()) {
            float realprice = ItemCheck.isWeapon(itemId)
                    ? weaponService.getWeapon(itemId).getRealprice()
                    : stuffService.getStuff(itemId).getRealprice();
            list.add((int) Math.round(realprice * 0.2) - 3);
        }

        profile.setRealMoney(list.get(new Random().nextInt(list.size())));
    }

}