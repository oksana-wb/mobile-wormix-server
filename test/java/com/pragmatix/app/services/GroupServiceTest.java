package com.pragmatix.app.services;

import com.pragmatix.app.messages.client.AddToGroup;
import com.pragmatix.app.messages.client.ReorderGroup;
import com.pragmatix.app.messages.client.SelectStuffs;
import com.pragmatix.app.messages.client.ToggleTeamMember;
import com.pragmatix.app.messages.server.AddToGroupResult;
import com.pragmatix.app.messages.server.ReorderGroupResult;
import com.pragmatix.app.messages.server.SelectStuffResults;
import com.pragmatix.app.messages.server.ToggleTeamMemberResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.group.FriendTeamMember;
import com.pragmatix.app.model.group.MercenaryBean;
import com.pragmatix.app.model.group.MercenaryTeamMember;
import com.pragmatix.app.model.group.SoclanTeamMember;
import com.pragmatix.app.settings.ItemRequirements;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Map;

import static com.pragmatix.app.common.TeamMemberType.*;
import static com.pragmatix.app.messages.client.AddToGroup.NO_PREV_TEAM_MEMBER;
import static com.pragmatix.app.common.MoneyType.MONEY;
import static org.junit.Assert.*;

public class GroupServiceTest extends AbstractSpringTest {

    @Resource
    StuffService stuffService;

    @Resource
    GroupService groupService;

    @Resource
    @Qualifier(value = "friendGroupPriceSettings")
    ItemRequirements friendGroupPriceSettings;

    @Resource
    @Qualifier(value = "soclanGroupPriceSettings")
    ItemRequirements soclanGroupPriceSettings;

    @Value("#{mercenariesConf}")
    Map<Integer, MercenaryBean> mercenariesConf;

    short hatId = 1011;
    short kitId = 2011;

    short memberHatId = 1012;
    short memberKitId = 2012;

    @Ignore("not usable on localhost")
    @Test
    public void toggleOffSoclanTeamMember() throws Exception {
        host = "my.rmart.ru";
        loginMain();
        System.out.println(Arrays.toString(enterAccount.userProfileStructure.wormsGroup));

        ToggleTeamMemberResult toggleTeamMemberResult = requestMain(new ToggleTeamMember(193232279, false), ToggleTeamMemberResult.class);

        System.out.println(Arrays.toString(enterAccount.userProfileStructure.wormsGroup));

        assertEquals(SimpleResultEnum.SUCCESS, toggleTeamMemberResult.result);
    }

    @Test
    public void testAddFriendToGroup() throws Exception {
        loginMain();
        System.out.println(enterAccount);

        int friendProfileId = (int) (testerProfileId - 1);
        UserProfile friendProfile = getProfile(friendProfileId);
        ItemRequirements addToGroupPrice = getAddToGroupPrice(friendProfile.getLevel(), false);
        prepateMemberProfile(friendProfile);

        UserProfile profile = getProfile(testerProfileId);
        profile.setMoney(addToGroupPrice.needMoney());

        prepare(profile);

        AddToGroupResult addToGroupResult = requestMain(new AddToGroup(friendProfileId, MONEY, Friend), AddToGroupResult.class);
        assertEquals(ShopResultEnum.SUCCESS, addToGroupResult.result);
        assertEquals(0, profile.getMoney());

        assertEquals(2, profile.getTeamMembers().length);
        assertEquals(2, profile.getWormsGroup().length);
        assertTrue(profile.getTeamMembers()[1] instanceof FriendTeamMember);
        assertMemberStuff(profile, 0, 0, 1);

        SelectStuffResults selectStuffResults = requestMain(new SelectStuffs(friendProfileId, hatId, kitId), SelectStuffResults.class, Integer.MAX_VALUE);
        assertEquals(1, selectStuffResults.selectStuffResults.length);
        assertEquals(SimpleResultEnum.SUCCESS, selectStuffResults.selectStuffResults[0].resultHat);
        assertEquals(SimpleResultEnum.SUCCESS, selectStuffResults.selectStuffResults[0].resultKit);

        assertMemberStuff(profile, hatId, kitId, 1);

        System.out.println(profile.getUserProfileStructure());
    }

    protected void prepateMemberProfile(UserProfile friendProfile) {
        prepateMemberProfile(friendProfile, memberHatId, memberKitId);
    }

    protected void prepateMemberProfile(UserProfile friendProfile, short memberHatId, short memberKitId) {
        friendProfile.setStuff(new short[0]);
        stuffService.addStuff(friendProfile, memberHatId);
        stuffService.addStuff(friendProfile, memberKitId);
        assertEquals(memberHatId, friendProfile.getHat());
        assertEquals(memberKitId, friendProfile.getKit());
    }

    protected void prepare(UserProfile profile) throws Exception {
        profile.setLevel(30);
        profile.setStuff(new short[0]);
        profile.setHat((short) 0);
        profile.setKit((short) 0);
        cleanTeam(profile);
        stuffService.addStuff(profile, hatId);
        profile.setHat((short) 0);
        stuffService.addStuff(profile, kitId);
        profile.setKit((short) 0);
        profile.setExtraGroupSlotsCount((byte) 0);
    }

    @Test
    public void testAddSoclanToGroup() throws Exception {
        loginMain();

        int soclanProfileId = (int) (testerProfileId - 2);
        UserProfile soclanProfile = getProfile(soclanProfileId);
        ItemRequirements addToGroupPrice = getAddToGroupPrice(soclanProfile.getLevel(), true);
        prepateMemberProfile(soclanProfile);

        UserProfile profile = getProfile(testerProfileId);
        profile.setMoney(addToGroupPrice.needMoney());
        prepare(profile);


        AddToGroupResult addToGroupResult = requestMain(new AddToGroup(soclanProfileId, MONEY, SoclanMember), AddToGroupResult.class);
        assertEquals(ShopResultEnum.SUCCESS, addToGroupResult.result);
        assertEquals(0, profile.getMoney());

        assertEquals(2, profile.getTeamMembers().length);
        assertEquals(2, profile.getWormsGroup().length);
        assertTrue(profile.getTeamMembers()[1] instanceof SoclanTeamMember);
        assertEquals(memberHatId, profile.getUserProfileStructure().wormsGroup[1].hat);
        assertEquals(memberKitId, profile.getUserProfileStructure().wormsGroup[1].kit);

        SelectStuffResults selectStuffResults = requestMain(new SelectStuffs(soclanProfileId, hatId, kitId), SelectStuffResults.class, Integer.MAX_VALUE);
        assertEquals(1, selectStuffResults.selectStuffResults.length);
        assertEquals(SimpleResultEnum.ERROR, selectStuffResults.selectStuffResults[0].resultHat);
        assertEquals(SimpleResultEnum.ERROR, selectStuffResults.selectStuffResults[0].resultKit);

        assertEquals(memberHatId, profile.getUserProfileStructure().wormsGroup[1].hat);
        assertEquals(memberKitId, profile.getUserProfileStructure().wormsGroup[1].kit);

        System.out.println(profile.getUserProfileStructure());
    }

    @Test
    public void testTrimSoclanStuff() throws Exception {
        loginMain();

        int soclanProfileId = (int) (testerProfileId - 2);
        UserProfile soclanProfile = getProfile(soclanProfileId);
        prepateMemberProfile(soclanProfile,
                (short) 1525,  // крафтовая шапка
                (short) 2037); // ачивочный артефакт: суперморгенштерн
        ItemRequirements addToGroupPrice = getAddToGroupPrice(soclanProfile.getLevel(), true);

        UserProfile profile = getProfile(testerProfileId);
        profile.setMoney(addToGroupPrice.needMoney());
        prepare(profile);

        AddToGroupResult addToGroupResult = requestMain(new AddToGroup(soclanProfileId, MONEY, SoclanMember), AddToGroupResult.class);
        assertEquals(ShopResultEnum.SUCCESS, addToGroupResult.result);
        assertEquals(0, profile.getMoney());

        assertEquals(2, profile.getTeamMembers().length);
        assertEquals(2, profile.getWormsGroup().length);
        assertTrue(profile.getTeamMembers()[1] instanceof SoclanTeamMember);
        // крафтовая вещь должна сброситься до минимальной ("добротной")
        assertEquals(1521, profile.getUserProfileStructure().wormsGroup[1].hat);
        // ачивочная вещь должна сброситься до обычной
        assertEquals(2022, profile.getUserProfileStructure().wormsGroup[1].kit);
    }

    @Test
    public void testAddOtherSoclanToGroup() throws Exception {
        loginMain();

        UserProfile profile = getProfile(testerProfileId);
        int soclanProfileId = (int) (testerProfileId - 2);
        int otherSoclanProfileId = (int) (testerProfileId - 3);

        UserProfile soclanProfile1 = getProfile(soclanProfileId);
        UserProfile soclanProfile2 = getProfile(otherSoclanProfileId);
        ItemRequirements addToGroupPrice1 = getAddToGroupPrice(soclanProfile1.getLevel(), true);
        ItemRequirements addToGroupPrice2 = getAddToGroupPrice(soclanProfile2.getLevel(), true);
        profile.setMoney(addToGroupPrice1.needMoney() + addToGroupPrice2.needMoney());
        prepare(profile);

        AddToGroupResult addToGroupResult;
        addToGroupResult = requestMain(new AddToGroup(soclanProfileId, MONEY, SoclanMember), AddToGroupResult.class);
        assertEquals(ShopResultEnum.SUCCESS, addToGroupResult.result);
        assertEquals(addToGroupPrice2.needMoney(), profile.getMoney());

        addToGroupResult = requestMain(new AddToGroup(otherSoclanProfileId, MONEY, SoclanMember), AddToGroupResult.class);
        assertEquals(ShopResultEnum.ERROR, addToGroupResult.result);
        assertEquals(addToGroupPrice2.needMoney(), profile.getMoney());

        // а если купить слот
        profile.setExtraGroupSlotsCount((byte) 1);
        // то второго активного добавить по-прежнему не можем
        addToGroupResult = requestMain(new AddToGroup(otherSoclanProfileId, MONEY, SoclanMember), AddToGroupResult.class);
        assertEquals(ShopResultEnum.ERROR, addToGroupResult.result);
        assertEquals(addToGroupPrice2.needMoney(), profile.getMoney());
        // а вот выключенного - пожалуйста
        addToGroupResult = requestMain(new AddToGroup(otherSoclanProfileId, MONEY, SoclanMember, -1, false), AddToGroupResult.class);
        assertEquals(ShopResultEnum.SUCCESS, addToGroupResult.result);
        assertEquals(0, profile.getMoney());
    }

    @Test
    public void testReplaceAndToggleSoclan() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        prepare(profile);
        loginMain();

        // у нас есть бесплатный наёмник (sanity check)
        assertEquals(2, profile.getActiveTeamMembersCount());
        assertEquals(-1, profile.getWormsGroup()[1]);

        int soclanProfileId = (int) (testerProfileId - 2);
        int otherSoclanProfileId = (int) (testerProfileId - 3);
        UserProfile soclanProfile1 = getProfile(soclanProfileId);
        UserProfile soclanProfile2 = getProfile(otherSoclanProfileId);
        ItemRequirements addToGroupPrice1 = getAddToGroupPrice(soclanProfile1.getLevel(), true);
        ItemRequirements addToGroupPrice2 = getAddToGroupPrice(soclanProfile2.getLevel(), true);

        AddToGroupResult addToGroupResult;
        profile.setMoney(addToGroupPrice1.needMoney());
        addToGroupResult = requestMain(new AddToGroup(soclanProfileId, MONEY, SoclanMember), AddToGroupResult.class);
        assertEquals(ShopResultEnum.SUCCESS, addToGroupResult.result);
        assertEquals(0, profile.getMoney());

        profile.setMoney(addToGroupPrice2.needMoney());
        // заменить наёмника на активного соклановцем не можем
        addToGroupResult = requestMain(new AddToGroup(otherSoclanProfileId, MONEY, SoclanMember, -1, true), AddToGroupResult.class);
        assertEquals(ShopResultEnum.ERROR, addToGroupResult.result);
        assertEquals(addToGroupPrice2.needMoney(), profile.getMoney());

        // даже если есть купленный слот, дающий право на второго запасного
        profile.setExtraGroupSlotsCount((byte) 1);
        addToGroupResult = requestMain(new AddToGroup(otherSoclanProfileId, MONEY, SoclanMember, -1, true), AddToGroupResult.class);
        assertEquals(ShopResultEnum.ERROR, addToGroupResult.result);
        assertEquals(addToGroupPrice2.needMoney(), profile.getMoney());

        // но можем выключить прежнего соклановца
        assertEquals(SimpleResultEnum.SUCCESS, requestMain(new ToggleTeamMember(soclanProfileId, false), ToggleTeamMemberResult.class).result);
        assertFalse("should not be active", profile.getFriendTeamMember(soclanProfileId).isActive());
        // и потом заменить наёмника на соклановца
        addToGroupResult = requestMain(new AddToGroup(otherSoclanProfileId, MONEY, SoclanMember, -1, true), AddToGroupResult.class);
        assertEquals(ShopResultEnum.SUCCESS, addToGroupResult.result);
        assertEquals(0, profile.getMoney());

        // теперь включить первого соклановца обратно не можем
        assertEquals(SimpleResultEnum.ERROR, requestMain(new ToggleTeamMember(soclanProfileId, true), ToggleTeamMemberResult.class).result);
        assertFalse("should not be active", profile.getFriendTeamMember(soclanProfileId).isActive());
        // но можем выключить второго
        assertEquals(SimpleResultEnum.SUCCESS, requestMain(new ToggleTeamMember(otherSoclanProfileId, false), ToggleTeamMemberResult.class).result);
        assertFalse("should not be active", profile.getFriendTeamMember(otherSoclanProfileId).isActive());
        // и тогда включить первого
        assertEquals(SimpleResultEnum.SUCCESS, requestMain(new ToggleTeamMember(soclanProfileId, true), ToggleTeamMemberResult.class).result);
        assertTrue("should be active", profile.getFriendTeamMember(soclanProfileId).isActive());
    }

    protected void cleanTeam(UserProfile profile) throws Exception {
        if(profile.getWormsGroup().length > 1) {
            for(int teamMemberId : profile.getWormsGroup()) {
                if(teamMemberId != profile.getId()) {
                    groupService.removeFromGroup(profile, teamMemberId);
                }
            }
        }
    }


    @Test
    public void testAddMercenaryToGroup() throws Exception {
        loginMain();
        System.out.println(enterAccount);

        UserProfile profile = getProfile(testerProfileId);
        int mercenaryId = -1;
        MercenaryBean mercenaryBean = mercenariesConf.get(mercenaryId);
        profile.setMoney(mercenaryBean.needMoney());
        prepare(profile);
        mercenaryBean.hatId = memberHatId;
        mercenaryBean.kitId = memberKitId;

        AddToGroupResult addToGroupResult = requestMain(new AddToGroup(mercenaryId, MONEY, Merchenary), AddToGroupResult.class);
        assertEquals(ShopResultEnum.SUCCESS, addToGroupResult.result);
        assertEquals(0, profile.getMoney());

        assertEquals(2, profile.getTeamMembers().length);
        assertEquals(2, profile.getWormsGroup().length);
        assertTrue(profile.getTeamMembers()[1] instanceof MercenaryTeamMember);

        assertEquals(0, profile.getTeamMembers()[1].getHat());
        assertEquals(0, profile.getTeamMembers()[1].getKit());

        assertEquals(memberHatId, profile.getUserProfileStructure().wormsGroup[1].hat);
        assertEquals(memberKitId, profile.getUserProfileStructure().wormsGroup[1].kit);

        SelectStuffResults selectStuffResults = requestMain(new SelectStuffs(mercenaryId, hatId, kitId), SelectStuffResults.class, Integer.MAX_VALUE);
        assertEquals(1, selectStuffResults.selectStuffResults.length);
        assertEquals(SimpleResultEnum.SUCCESS, selectStuffResults.selectStuffResults[0].resultHat);
        assertEquals(SimpleResultEnum.SUCCESS, selectStuffResults.selectStuffResults[0].resultKit);

        assertMemberStuff(profile, hatId, kitId, 1);

        requestMain(new SelectStuffs(mercenaryId, 0, 0), SelectStuffResults.class, Integer.MAX_VALUE);

        assertEquals(0, profile.getTeamMembers()[1].getHat());
        assertEquals(0, profile.getTeamMembers()[1].getKit());

        assertEquals(memberHatId, profile.getUserProfileStructure().wormsGroup[1].hat);
        assertEquals(memberKitId, profile.getUserProfileStructure().wormsGroup[1].kit);

        System.out.println(profile.getUserProfileStructure());
    }

    @Test
    public void testAddAndToggleMercenary() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        int[] mercenaries = {-2, -3, -4};
        int needMoney = 0;
        for(int mercenaryId : mercenaries) {
            MercenaryBean mercenaryBean = mercenariesConf.get(mercenaryId);
            needMoney += mercenaryBean.needMoney();
        }
        profile.setMoney(needMoney);
        prepare(profile);
        profile.setLevel(15); // имеем право на 3х юнитов в бою
        profile.setExtraGroupSlotsCount((byte) 2); // плюс два слота

        loginMain(); // получаем бесплатного -1го наёмника, итого нас двое, оба активны, и ещё три слота свободны
        assertEquals(2, profile.getWormsGroup().length);
        assertEquals(2, profile.getTeamMembers().length);

        // добавляем в эти слоты троих наёмников
        for(int mercenaryId : mercenaries) {
            boolean active = mercenaryId == mercenaries[0]; // активный только первый
            assertEquals(ShopResultEnum.SUCCESS,
                    requestMain(new AddToGroup(mercenaryId, MONEY, Merchenary, NO_PREV_TEAM_MEMBER, active), AddToGroupResult.class).result);
        }
        assertEquals(0, profile.getMoney());
        assertEquals(5, profile.getWormsGroup().length);
        assertEquals(5, profile.getTeamMembers().length);

        // но из них только первый становится активным, т.е. всего 3 активных
        assertEquals(3, profile.getActiveTeamMembersCount());
        assertTrue("3'rd is active", profile.getTeamMembers()[2].isActive());
        assertFalse("4'th is inactive", profile.getTeamMembers()[3].isActive());
        assertFalse("5'th is inactive", profile.getTeamMembers()[4].isActive());

        // включить четвёртого мы не можем
        assertEquals(SimpleResultEnum.ERROR, requestMain(new ToggleTeamMember(-3, true), ToggleTeamMemberResult.class).result);

        // но можем сначала выключить, например, второго
        assertEquals(SimpleResultEnum.SUCCESS, requestMain(new ToggleTeamMember(-1, false), ToggleTeamMemberResult.class).result);
        // и потом уже включить четвёртого
        assertEquals(SimpleResultEnum.SUCCESS, requestMain(new ToggleTeamMember(-3, true), ToggleTeamMemberResult.class).result);

        // но себя не можем выключить никогда
        assertEquals(SimpleResultEnum.ERROR, requestMain(new ToggleTeamMember((int) testerProfileId, false), ToggleTeamMemberResult.class).result);

        // и добавить сверх имеющегося числа доступных слотов тоже мы не можем
        int badMercenary = -5;
        profile.setMoney(mercenariesConf.get(badMercenary).needMoney());
        assertEquals(ShopResultEnum.MIN_REQUIREMENTS_ERROR, requestMain(new AddToGroup(badMercenary, MONEY, Merchenary), AddToGroupResult.class).result);
    }

    @Test
    public void testCannotAddTooManyActiveToGroup() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        int mercenaryId = -2;
        MercenaryBean mercenaryBean = mercenariesConf.get(mercenaryId);
        final int needMoney = mercenaryBean.needMoney();
        profile.setMoney(needMoney);
        prepare(profile);
        profile.setLevel(6); // имеем право на 2х юнитов в бою
        profile.setExtraGroupSlotsCount((byte) 2); // плюс два слота

        loginMain(); // получаем бесплатного -1го наёмника, итого нас двое, оба активны, и ещё два слота свободны

        // но добавить активного наёмника не можем: трое не могут быть активны
        assertEquals(ShopResultEnum.MIN_REQUIREMENTS_ERROR,
                requestMain(new AddToGroup(mercenaryId, MONEY, Merchenary, NO_PREV_TEAM_MEMBER, true), AddToGroupResult.class).result);
        assertEquals(needMoney, profile.getMoney()); // деньги не списались
        assertEquals(2, profile.getWormsGroup().length);  // размер команды не изменился
        assertEquals(2, profile.getTeamMembers().length);

        // а неактивного - пожалуйста
        assertEquals(ShopResultEnum.SUCCESS,
                requestMain(new AddToGroup(mercenaryId, MONEY, Merchenary, NO_PREV_TEAM_MEMBER, false), AddToGroupResult.class).result);
        assertEquals(0, profile.getMoney());
        assertEquals(3, profile.getWormsGroup().length);
        assertEquals(3, profile.getTeamMembers().length);
        assertEquals(mercenaryId, profile.getWormsGroup()[2]);
        assertFalse("Mercenary is not active", profile.getTeamMembers()[2].isActive());
    }

    protected void assertMemberStuff(UserProfile profile, int hatId, int kitId, int memberIndex) {
        assertEquals(hatId, profile.getTeamMembers()[memberIndex].getHat());
        assertEquals(kitId, profile.getTeamMembers()[memberIndex].getKit());

        assertEquals(hatId, profile.getUserProfileStructure().wormsGroup[memberIndex].hat);
        assertEquals(kitId, profile.getUserProfileStructure().wormsGroup[memberIndex].kit);
    }

    private ItemRequirements getAddToGroupPrice(int teamMemberLevel, boolean isSonlan) {
        ItemRequirements groupPriceSettings = new ItemRequirements();
        if(isSonlan) {
            groupPriceSettings.needMoney = soclanGroupPriceSettings.needMoney + teamMemberLevel * soclanGroupPriceSettings.needRealMoney;
        } else {
            groupPriceSettings.needMoney = friendGroupPriceSettings.needMoney + teamMemberLevel * friendGroupPriceSettings.needRealMoney;
        }
        groupPriceSettings.needRealMoney = (int) Math.ceil(groupPriceSettings.needMoney / 100);
        return groupPriceSettings;
    }

    @Ignore("removeFromGroup deprecated")
    @Test
    public void testRemoveFromGroup() throws Exception {
    }

    private void arraySwap(int[] arr, int i, int j) {
        int tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    @Test
    public void testReorderGroup() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        prepare(profile);
        profile.setLevel(6); // теперь имеем право на себя + бесплатного наёмника
        profile.setExtraGroupSlotsCount((byte) 1); // плюс один слот

        int friendProfileId = (int) (testerProfileId - 1);
        UserProfile friendProfile = getProfile(friendProfileId);
        ItemRequirements addToGroupPrice = getAddToGroupPrice(friendProfile.getLevel(), false);
        prepateMemberProfile(friendProfile);
        profile.setMoney(addToGroupPrice.needMoney());

        loginMain();
        assertEquals(2, profile.getWormsGroup().length);
        assertEquals(2, profile.getTeamMembers().length);
        assertTrue("We have free mercenary", profile.getTeamMembers()[1] instanceof MercenaryTeamMember);
        assertTrue("Mercenary is active", profile.getTeamMembers()[1].isActive());

        // добавляем в слот неактивного друга
        assertEquals(ShopResultEnum.SUCCESS, requestMain(new AddToGroup(friendProfileId, MONEY, Friend, NO_PREV_TEAM_MEMBER, false), AddToGroupResult.class).result);
        assertEquals(0, profile.getMoney());
        assertEquals(3, profile.getWormsGroup().length);
        assertEquals(3, profile.getTeamMembers().length);
        assertTrue("We have friend", profile.getTeamMembers()[2] instanceof FriendTeamMember);
        assertFalse("Friend is NOT active", profile.getTeamMembers()[2].isActive());

        // меняем себя и наёмника местами
        int[] wormsGroup = profile.getWormsGroup().clone();
        arraySwap(wormsGroup, 0, 1);
        assertEquals(SimpleResultEnum.SUCCESS, requestMain(new ReorderGroup(wormsGroup), ReorderGroupResult.class).result);
        assertArrayEquals(wormsGroup, profile.getWormsGroup());
        assertTrue("First is mercenary", profile.getTeamMembers()[0] instanceof MercenaryTeamMember);
        assertTrue("Mercenary is active", profile.getTeamMembers()[0].isActive());
        assertNull("Second is me", profile.getTeamMembers()[1]);

        // достигаем 15 уровня и друга можно теперь включить
        profile.setLevel(15);
        assertEquals(SimpleResultEnum.SUCCESS, requestMain(new ToggleTeamMember(friendProfileId, true), ToggleTeamMemberResult.class).result);
        assertTrue("Friend is active", profile.getTeamMembers()[2].isActive());

        // меняем всех подряд местами: я, друг, наёмник
        wormsGroup = profile.getWormsGroup().clone();
        arraySwap(wormsGroup, 1, 2);
        arraySwap(wormsGroup, 0, 2);
        assertEquals(SimpleResultEnum.SUCCESS, requestMain(new ReorderGroup(wormsGroup), ReorderGroupResult.class).result);
        assertNull("First is me", profile.getTeamMembers()[0]);
        assertTrue("Second is friend", profile.getTeamMembers()[1] instanceof FriendTeamMember);
        assertTrue("Third is mercenary", profile.getTeamMembers()[1] instanceof MercenaryTeamMember);
        assertEquals(3, profile.getActiveTeamMembersCount());
    }

    @Test // mostly for experiments with WebAdmin
    public void temporaryTestJustToInitializeSomeTeam() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        prepare(profile);
        profile.setLevel(6);
        profile.setMoney(100000);
        profile.setExtraGroupSlotsCount((byte) 1);
        loginMain();

        // replace mercenary with friend
        int previousMercenary = profile.getWormsGroup()[1];
        int friendProfileId = (int) (testerProfileId - 1);
        assertEquals(ShopResultEnum.SUCCESS, requestMain(new AddToGroup(friendProfileId, MONEY, Friend, previousMercenary, true), AddToGroupResult.class).result);

        // add one more mercenary (inactive)
        int curMercenary = previousMercenary - 1;
        assertEquals(ShopResultEnum.SUCCESS, requestMain(new AddToGroup(curMercenary, MONEY, Merchenary, NO_PREV_TEAM_MEMBER, false), AddToGroupResult.class).result);

        // and rename him
        assertEquals(ShopResultEnum.SUCCESS, requestMain(new com.pragmatix.app.messages.client.BuyRename(curMercenary, "Merc#2", MONEY), com.pragmatix.app.messages.server.BuyRenameResult.class).result);

        profile.setLevel(30);
        // now can make active
        assertEquals(SimpleResultEnum.SUCCESS, requestMain(new ToggleTeamMember(curMercenary, true), ToggleTeamMemberResult.class).result);

        System.out.println(profile);
        System.out.println(profile.getUserProfileStructure());
    }

    @After
    public void disconnect() throws Exception {
        disconnectMain();
    }
}