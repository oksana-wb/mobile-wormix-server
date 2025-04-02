package com.pragmatix.app.controllers;

import com.pragmatix.app.dao.WormGroupsDao;
import com.pragmatix.app.domain.WormGroupsEntity;
import com.pragmatix.app.messages.client.AddToGroup;
import com.pragmatix.app.messages.client.RemoveFromGroup;
import com.pragmatix.app.messages.client.ReorderGroup;
import com.pragmatix.app.messages.server.AddToGroupResult;
import com.pragmatix.app.messages.server.RemoveFromGroupResult;
import com.pragmatix.app.messages.server.ReorderGroupResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 16.08.12 11:52
 */
public class GroupControllerTest extends AbstractSpringTest {

    @Resource
    private GroupController groupController;

    @Resource
    private WormGroupsDao wormGroupsDao;

    @Resource
    private ProfileService profileService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Test
    public void testOnAddToGroup() throws Exception {
        final int testerProfileId = (int) AbstractSpringTest.testerProfileId;
        int teamMemberId2 = testerProfileId - 1;
        int teamMemberId3 = testerProfileId - 2;
        int teamMemberId4 = testerProfileId - 3;
        UserProfile profile = getProfile(testerProfileId);
        profile.setLevel(30);
        profile.setMoney(1000);
        profile.setRealMoney(100);
        profile.initWormGroup(null);
        profileService.getUserProfileStructure(profile);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                wormGroupsDao.deleteWormGroups((long) testerProfileId);
            }
        });
        AddToGroup msg;
        AddToGroupResult addToGroupResult;
        WormGroupsEntity wormGroups;

        loginMain();
        Thread.sleep(300);

        // первый пошел
        msg = newAddToGroup(teamMemberId2);

        sendMain(msg);
        addToGroupResult = receiveMain(AddToGroupResult.class);

        assertEquals(ShopResultEnum.SUCCESS, addToGroupResult.result);

        wormGroups = wormGroupsDao.getWormGroupsByProfileId((long) testerProfileId);

        assertEquals(testerProfileId, wormGroups.getTeamMember1());
        assertEquals(teamMemberId2, wormGroups.getTeamMember2());

        // пытаемся добавить его снова. должна быть ошибка
        sendMain(msg);
        addToGroupResult = receiveMain(AddToGroupResult.class);

        assertEquals(ShopResultEnum.ERROR, addToGroupResult.result);

        // добавляем второго
        msg.teamMemberId = teamMemberId3;

        sendMain(msg);
        addToGroupResult = receiveMain(AddToGroupResult.class);

        assertEquals(ShopResultEnum.SUCCESS, addToGroupResult.result);
        wormGroups = wormGroupsDao.getWormGroupsByProfileId((long) testerProfileId);
        assertEquals(testerProfileId, wormGroups.getTeamMember1());
        assertEquals(teamMemberId2, wormGroups.getTeamMember2());
        assertEquals(teamMemberId3, wormGroups.getTeamMember3().longValue());

        // добавляем третьего
        msg.teamMemberId = teamMemberId4;

        sendMain(msg);
        addToGroupResult = receiveMain(AddToGroupResult.class);

        assertEquals(ShopResultEnum.SUCCESS, addToGroupResult.result);
        wormGroups = wormGroupsDao.getWormGroupsByProfileId((long) testerProfileId);
        assertEquals(testerProfileId, wormGroups.getTeamMember1());
        assertEquals(teamMemberId2, wormGroups.getTeamMember2());
        assertEquals(teamMemberId3, wormGroups.getTeamMember3().longValue());
        assertEquals(teamMemberId4, wormGroups.getTeamMember4().longValue());

        // добавляем четвертого - ошибка
        msg.teamMemberId = teamMemberId4 - 1;

        sendMain(msg);
        addToGroupResult = receiveMain(AddToGroupResult.class);

        assertEquals(ShopResultEnum.MIN_REQUIREMENTS_ERROR, addToGroupResult.result);

    }

    @Test
    public void testOnRemoveFromGroup() throws Exception {
        final int testerProfileId = (int) AbstractSpringTest.testerProfileId;
        int teamMemberId2 = testerProfileId - 1;
        int teamMemberId3 = testerProfileId - 2;
        int teamMemberId4 = testerProfileId - 3;
        UserProfile profile = getProfile(testerProfileId);
        profile.setLevel(30);
        profile.setMoney(1000);
        profile.setRealMoney(100);
        profile.initWormGroup(null);
        profileService.getUserProfileStructure(profile);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                wormGroupsDao.deleteWormGroups((long) testerProfileId);
            }
        });
        RemoveFromGroupResult removeFromGroupResult;
        WormGroupsEntity wormGroups;

        groupController.onAddToGroup(newAddToGroup(teamMemberId2), profile);
        groupController.onAddToGroup(newAddToGroup(teamMemberId3), profile);
        groupController.onAddToGroup(newAddToGroup(teamMemberId4), profile);

        wormGroups = wormGroupsDao.getWormGroupsByProfileId((long) testerProfileId);
        assertEquals(testerProfileId, wormGroups.getTeamMember1());
        assertEquals(teamMemberId2, wormGroups.getTeamMember2());
        assertEquals(teamMemberId3, wormGroups.getTeamMember3().longValue());
        assertEquals(teamMemberId4, wormGroups.getTeamMember4().longValue());

        // удаляем 4-го
        removeFromGroupResult = groupController.onRemoveFromGroup(newRemoveFromGroup(teamMemberId4), profile);
        assertEquals(SimpleResultEnum.SUCCESS, removeFromGroupResult.result);

        wormGroups = wormGroupsDao.getWormGroupsByProfileId((long) testerProfileId);
        assertEquals(testerProfileId, wormGroups.getTeamMember1());
        assertEquals(teamMemberId2, wormGroups.getTeamMember2());
        assertEquals(teamMemberId3, wormGroups.getTeamMember3().longValue());
        assertNull(wormGroups.getTeamMember4());

        // удаляем 3-го
        removeFromGroupResult = groupController.onRemoveFromGroup(newRemoveFromGroup(teamMemberId3), profile);
        assertEquals(SimpleResultEnum.SUCCESS, removeFromGroupResult.result);

        wormGroups = wormGroupsDao.getWormGroupsByProfileId((long) testerProfileId);
        assertEquals(testerProfileId, wormGroups.getTeamMember1());
        assertEquals(teamMemberId2, wormGroups.getTeamMember2());
        assertNull(wormGroups.getTeamMember3());
        assertNull(wormGroups.getTeamMember4());

        // удаляем 2-го и всю запись целиком
        removeFromGroupResult = groupController.onRemoveFromGroup(newRemoveFromGroup(teamMemberId2), profile);
        assertEquals(SimpleResultEnum.SUCCESS, removeFromGroupResult.result);

        wormGroups = wormGroupsDao.getWormGroupsByProfileId((long) testerProfileId);
        assertNull(wormGroups);
    }

    @Test
    public void testOnReorderGroup() throws Exception {
        final int testerProfileId = (int) AbstractSpringTest.testerProfileId;
        int teamMemberId2 = testerProfileId - 1;
        int teamMemberId3 = testerProfileId - 2;
        int teamMemberId4 = testerProfileId - 3;
        UserProfile profile = getProfile(testerProfileId);
        profile.setLevel(30);
        profile.setMoney(1000);
        profile.setRealMoney(100);
        profile.initWormGroup(null);
        profileService.getUserProfileStructure(profile);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                wormGroupsDao.deleteWormGroups((long) testerProfileId);
            }
        });
        ReorderGroupResult reorderGroupResult;
        WormGroupsEntity wormGroups;

        groupController.onAddToGroup(newAddToGroup(teamMemberId2), profile);
        groupController.onAddToGroup(newAddToGroup(teamMemberId3), profile);
        groupController.onAddToGroup(newAddToGroup(teamMemberId4), profile);

        wormGroups = wormGroupsDao.getWormGroupsByProfileId((long) testerProfileId);
        assertEquals(testerProfileId, wormGroups.getTeamMember1());
        assertEquals(teamMemberId2, wormGroups.getTeamMember2());
        assertEquals(teamMemberId3, wormGroups.getTeamMember3().longValue());
        assertEquals(teamMemberId4, wormGroups.getTeamMember4().longValue());

        // перестраиваем неудачно

        reorderGroupResult = groupController.onReorderGroup(newReorderGroup(testerProfileId), profile);
        assertEquals(SimpleResultEnum.ERROR, reorderGroupResult.result);

        // перестраиваем неудачно снова

        reorderGroupResult = groupController.onReorderGroup(newReorderGroup(testerProfileId, testerProfileId, testerProfileId, testerProfileId), profile);
        assertEquals(SimpleResultEnum.ERROR, reorderGroupResult.result);

        // перестраиваем

        reorderGroupResult = groupController.onReorderGroup(newReorderGroup(teamMemberId4, teamMemberId3, teamMemberId2, testerProfileId), profile);
        assertEquals(SimpleResultEnum.SUCCESS, reorderGroupResult.result);

        wormGroups = wormGroupsDao.getWormGroupsByProfileId((long) testerProfileId);
        assertEquals(teamMemberId4, wormGroups.getTeamMember1());
        assertEquals(teamMemberId3, wormGroups.getTeamMember2());
        assertEquals(teamMemberId2, wormGroups.getTeamMember3().longValue());
        assertEquals(testerProfileId, wormGroups.getTeamMember4().longValue());
    }

    private AddToGroup newAddToGroup(int teamMemberId) {
        AddToGroup msg;
        msg = new AddToGroup();
        msg.moneyType = MoneyType.REAL_MONEY;
        msg.teamMemberId = teamMemberId;
        return msg;
    }

    private RemoveFromGroup newRemoveFromGroup(int teamMemberId) {
        RemoveFromGroup msg;
        msg = new RemoveFromGroup();
        msg.teamMemberId = teamMemberId;
        return msg;
    }

    private ReorderGroup newReorderGroup(int... teamMemberIds) {
        ReorderGroup msg;
        msg = new ReorderGroup();
        msg.reorderedWormGroup = teamMemberIds;
        return msg;
    }

}
