package com.pragmatix.app.services;

import com.pragmatix.app.init.UserProfileCreator;
import com.pragmatix.app.messages.client.SetBackpackConf;
import com.pragmatix.app.messages.server.SetBackpackConfResult;
import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.Weapon;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import com.pragmatix.testcase.AbstractTest;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class WeaponServiceTest extends AbstractSpringTest {

    @Resource
    WeaponService weaponService;

    @Resource
    ProfileService profileService;

    @Resource
    UserProfileCreator userProfileCreator;

    UserProfile profile;

    int weaponId;


    public static class WeaponTest extends AbstractTest {
        @Test
        public void test(){
            Weapon infiniteWeapon = new Weapon();

            Weapon seasonalWeapon = new Weapon();
            seasonalWeapon.setSellPrice(5);

            Weapon complexWeapon = new Weapon();
            complexWeapon.setMaxWeaponLevel(4);

            Weapon weapon;
            int weaponCount;
            int count;

            weapon = infiniteWeapon;

            weaponCount = 0;
            count = -1;
            assertEquals(1, weapon.difference(weapon.increment(weaponCount, count), weaponCount));
            weaponCount = 1;
            count = -1;
            assertEquals(1, weapon.difference(weapon.increment(weaponCount, count), weaponCount));
            weaponCount = -1;
            count = -1;
            assertEquals(0, weapon.difference(weapon.increment(weaponCount, count), weaponCount));
            weaponCount = 1;
            count = 1;
            assertEquals(0, weapon.difference(weapon.increment(weaponCount, count), weaponCount));

            weapon = seasonalWeapon;

            weaponCount = 0;
            count = 1;
            assertEquals(1, weapon.difference(weapon.increment(weaponCount, count), weaponCount));
            weaponCount = 1;
            count = 1;
            assertEquals(1, weapon.difference(weapon.increment(weaponCount, count), weaponCount));
            weaponCount = 1;
            count = 2;
            assertEquals(2, weapon.difference(weapon.increment(weaponCount, count), weaponCount));

            weapon = complexWeapon;

            weaponCount = 0;
            count = 1;
            assertEquals(1, weapon.difference(weapon.increment(weaponCount, count), weaponCount));
            weaponCount = 1;
            count = 1;
            assertEquals(1, weapon.difference(weapon.increment(weaponCount, count), weaponCount));
            weaponCount = -11;
            count = 1;
            assertEquals(1, weapon.difference(weapon.increment(weaponCount, count), weaponCount));
            weaponCount = 1;
            count = 4;
            assertEquals(4, weapon.difference(weapon.increment(weaponCount, count), weaponCount));
            weaponCount = -13;
            count = 2;
            assertEquals(1, weapon.difference(weapon.increment(weaponCount, count), weaponCount));
        }
    }

    @Test
    public void testAddOrUpdateComplexWeapon() throws Exception {
        profile = new UserProfile(testerProfileId);
        testService.cleanBackpack(profile);
        profileService.getUserProfileStructure(profile);

        //<wormix:weapon weaponId="12" name="Сверлящая ракета                   " price=" 2000" realPrice=" 20" level=" 7" maxWeaponLevel="4"/>
        weaponId = 12;
        profile.addBackpackItem(new BackpackItem(weaponId, 3, true));
        System.out.println(profile.getBackpack());

        modifyWeaponCount(-1, 2, false);
        modifyWeaponCount(-2, 0, false);
        modifyWeaponCount(-1, 0, false);
        modifyWeaponCount(1, -11, false);
        modifyWeaponCount(-1, -11, false);
        modifyWeaponCount(3, -14, true);
        modifyWeaponCount(-1, -14, true);
        modifyWeaponCount(1, -14, true);

        //<wormix:weapon weaponId="11" name="Сверлящая ракета (один выстрел)    " price="   60" realPrice="  -" level=" 6" type="CONSUMABLE"/>
        weaponId = 11;
        profile.addBackpackItem(new BackpackItem(weaponId, 3, true));
        System.out.println(profile.getBackpack());

        modifyWeaponCount(-1, 2, false);
        modifyWeaponCount(-2, 0, false);
        modifyWeaponCount(-1, 0, false);
        modifyWeaponCount(1, 1, false);
        modifyWeaponCount(-1, 0, false);
        modifyWeaponCount(3, 3, false);
        modifyWeaponCount(-1, 2, false);
        modifyWeaponCount(1, 3, false);

        //<wormix:weapon weaponId=" 9" name="Мортира                            " price=" 1100" realPrice=" 12" level=" 4"/>
        weaponId = 9;
        profile.addBackpackItem(new BackpackItem(weaponId, 3, true));
        System.out.println(profile.getBackpack());

        modifyWeaponCount(-1, 2, false);
        modifyWeaponCount(-2, 0, false);
        modifyWeaponCount(-1, 0, false);
        modifyWeaponCount(1, 1, false);
        modifyWeaponCount(Integer.MAX_VALUE, -1, true);
        modifyWeaponCount(-1, -1, true);
        modifyWeaponCount(3, -1, true);
        modifyWeaponCount(Integer.MAX_VALUE, -1, true);

        profileService.updateSync(profile);
    }

    private void modifyWeaponCount(int count, int expectedItemCount, boolean mustBeIndefinite) {
        if(count > 0) {
            if(count == Integer.MAX_VALUE){
                weaponService.addOrUpdateWeapon(profile, weaponId, -1);
            }else{
                weaponService.addOrUpdateWeapon(profile, weaponId, count);
            }
        } else {
            weaponService.removeOrUpdateWeaponSilent(profile, weaponId, -count);
        }
        System.out.println(profile.getBackpack());
        assertEquals(expectedItemCount, profile.getBackpackItemByWeaponId(weaponId).getCount());
        assertEquals(mustBeIndefinite, weaponService.isPresentInfinitely(profile.getBackpackItemByWeaponId(weaponId)));
    }

    @Test
    public void upsSyncTest() {
        UserProfile profile = getProfile(testerProfileId);
        testService.cleanBackpack(profile);
        profileService.getUserProfileStructure(profile);

        int weaponId = 110;
        int i;

        weaponService.addOrUpdateWeapon(profile, weaponId, 1);
        assertNotNull(profile.getBackpackItemByWeaponId(weaponId));
        assertEquals(1, profile.getBackpackItemByWeaponId(weaponId).getCount());

        i = ArrayUtils.indexOf(profile.getUserProfileStructure().backpack, (short) weaponId);
        assertTrue(i > 0);
        assertEquals(1, profile.getUserProfileStructure().backpack[i + 1]);


        weaponService.addOrUpdateWeapon(profile, weaponId, 1);
        assertNotNull(profile.getBackpackItemByWeaponId(weaponId));
        assertEquals(2, profile.getBackpackItemByWeaponId(weaponId).getCount());

        i = ArrayUtils.indexOf(profile.getUserProfileStructure().backpack, (short) weaponId);
        assertTrue(i > 0);
        assertEquals(2, profile.getUserProfileStructure().backpack[i + 1]);


        weaponService.removeOrUpdateWeaponSilent(profile, weaponId, 1);
        assertNotNull(profile.getBackpackItemByWeaponId(weaponId));
        assertEquals(1, profile.getBackpackItemByWeaponId(weaponId).getCount());

        i = ArrayUtils.indexOf(profile.getUserProfileStructure().backpack, (short) weaponId);
        assertTrue(i > 0);
        assertEquals(1, profile.getUserProfileStructure().backpack[i + 1]);
    }

    @Test
    public void testBackpackConf() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        System.out.println(profile.getBackpack());

        loginMain();
        System.out.println(profile.getBackpackConfs());
        short[] conf1 = new short[]{40, 55};
        short[] conf2 = new short[]{41, 56};
        short[] conf3 = new short[]{42, 57};
        sendMain(new SetBackpackConf(conf1, conf2, conf3, 2));
        SetBackpackConfResult result = receiveMain(SetBackpackConfResult.class);
        assertEquals(SimpleResultEnum.SUCCESS, result.result);

        System.out.println(profile.getBackpackConfs());
        disconnectMain();

        Thread.sleep(1000);
    }

    private void initBackpack(UserProfile profile) {
        profile.setBackpack(userProfileCreator.initBackpack(new ArrayList<>()));
    }

}
