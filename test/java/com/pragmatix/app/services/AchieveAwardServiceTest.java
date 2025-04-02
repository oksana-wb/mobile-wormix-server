package com.pragmatix.app.services;

import com.pragmatix.app.achieve.AchieveAwardService;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.09.2015 11:35
 */
public class AchieveAwardServiceTest extends AbstractSpringTest {

    @Resource
    AchieveAwardService achieveAwardService;

    UserProfile profile;

    @Test
    public void countBonusItemInBackpackTest() throws Exception {
        profile = getProfile(159004029);
        //profile.cleanGrantedAchieveAwards();
        int bonusItemsCount = achieveAwardService.countBonusItemInBackpack(profile);
        println(bonusItemsCount);
    }

//    @Test
//    public void requiresFailureGrantAwardsTest() throws Exception {
//        int weaponId = 77;
//        int replacedWeaponId = 115;
//        weaponService.removeWeapon(profile, weaponId);
//        weaponService.removeWeapon(profile, replacedWeaponId);
//        assertEquals(GrantAwardResultEnum.MIN_REQUIREMENTS_ERROR, achieveAwardService.grantAwards("" + testerProfileId, new AwardStructure[]{newItemAwardStructure(weaponId)}));
//    }
//
//    @Test
//    public void requiresSuccessGrantAwardsTest() throws Exception {
//        int weaponId = 77;
//        int replacedWeaponId = 115;
//        weaponService.removeWeapon(profile, weaponId);
//        weaponService.removeWeapon(profile, replacedWeaponId);
//
//        assertTrue(weaponService.addOrUpdateWeapon(profile, replacedWeaponId, -1));
//        assertEquals(GrantAwardResultEnum.OK, achieveAwardService.grantAwards("" + testerProfileId, new AwardStructure[]{newItemAwardStructure(weaponId)}));
//        assertFalse(weaponService.isPresentInfinitely(profile, replacedWeaponId));
//        assertTrue(weaponService.isPresentInfinitely(profile, weaponId));
//    }
//
//    @Test
//    public void replacesSuccessGrantAwardsTest() throws Exception {
//        short replacedStuffId = (short) 1034;
//        short stuffId = (short) 1144;
//        stuffService.addStuff(profile, replacedStuffId);
//        stuffService.removeStuff(profile, stuffId);
//
//        assertTrue(stuffService.isExistPermanent(profile, replacedStuffId));
//        assertFalse(stuffService.isExistPermanent(profile, stuffId));
//
//        assertEquals(GrantAwardResultEnum.OK, achieveAwardService.grantAwards("" + testerProfileId, new AwardStructure[]{newItemAwardStructure(stuffId)}));
//
//        assertTrue(stuffService.isExistPermanent(profile, stuffId));
//        assertFalse(stuffService.isExistPermanent(profile, replacedStuffId));
//    }
//
//    @Test
//    public void replacedDisabledGrantAwardsTest() throws Exception {
//        short replacedStuffId = (short) 1034;
//        short stuffId = (short) 1144;
//        stuffService.removeStuff(profile, replacedStuffId);
//        stuffService.addStuff(profile, stuffId);
//
//        assertEquals(GrantAwardResultEnum.ERROR, achieveAwardService.grantAwards("" + testerProfileId, new AwardStructure[]{newItemAwardStructure(replacedStuffId)}));
//
//        assertTrue(stuffService.isExistPermanent(profile, stuffId));
//        assertFalse(stuffService.isExistPermanent(profile, replacedStuffId));
//    }
//
//    private AwardStructure newItemAwardStructure(int itemId) {
//        return new AwardStructure(0, 0, 0, itemId, 0);
//    }
//
//    private AwardStructure newMoneyAwardStructure(int value) {
//        return new AwardStructure(value, 0, 0, 0, 0);
//    }
//
//    private AwardStructure newRealMoneyAwardStructure(int value) {
//        return new AwardStructure(0, value, 0, 0, 0);
//    }
//
//    private AwardStructure newReactionAwardStructure(int value) {
//        return new AwardStructure(0, 0, value, 0, 0);
//    }
//
//    private AwardStructure newAwardStructure(int money, int realmoney, int reaction) {
//        return new AwardStructure(money, realmoney, reaction, 0, 0);
//    }
//
//    @Test
//    public void simpleAwardsTest() throws Exception {
//        profile.setMoney(0);
//        profile.setRealMoney(0);
//        profile.setReactionRate(0);
//
//        AwardStructure[] awardStructures = {
//                newMoneyAwardStructure(100),
//                newRealMoneyAwardStructure(200),
//                newReactionAwardStructure(300),
//        };
//
//        assertEquals(GrantAwardResultEnum.OK, achieveAwardService.grantAwards("" + testerProfileId, awardStructures));
//        assertEquals(100, profile.getMoney());
//        assertEquals(200, profile.getRealMoney());
//        assertEquals(300, profile.getReactionRate());
//    }
//
//    @Test
//    public void simpleAwardsTest2() throws Exception {
//        profile.setMoney(0);
//        profile.setRealMoney(0);
//        profile.setReactionRate(0);
//
//        AwardStructure[] awardStructures = {
//                newAwardStructure(100, 200, 300),
//        };
//
//        assertEquals(GrantAwardResultEnum.OK, achieveAwardService.grantAwards("" + testerProfileId, awardStructures));
//        assertEquals(100, profile.getMoney());
//        assertEquals(200, profile.getRealMoney());
//        assertEquals(300, profile.getReactionRate());
//    }
}