package com.pragmatix.app.controllers;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.dao.CallbackFriendDao;
import com.pragmatix.app.dao.UserProfileDao;
import com.pragmatix.app.messages.client.Login;
import com.pragmatix.app.messages.server.CallbackersList;
import com.pragmatix.app.messages.server.EnterAccount;
import com.pragmatix.app.messages.structures.EndBattleStructure;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.CallbackFriendService;
import com.pragmatix.app.services.UserRegistry;
import com.pragmatix.app.services.authorize.LoginService;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import com.pragmatix.testcase.handlers.TestcaseSimpleMessageHandler;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.10.12 11:52
 */
public class CallbackFriendControllerTest extends AbstractSpringTest {

    @Resource
    private CallbackFriendDao callbackFriendDao;

    @Resource
    private CallbackFriendService callbackFriendService;

    @Resource
    private UserRegistry userRegistry;

    @Resource
    private LoginService loginService;

    @Resource
    private UserProfileDao userProfileDao;

    @Resource
    private TransactionTemplate transactionTemplate;

    private TestcaseSimpleMessageHandler msgHandler;

    private UserProfile profile;
    private long friendId = testerProfileId - 1;
    private UserProfile friend;

    @Value("${comebackBonusSettings.absetDays}")
    private int absentDays;

    @Value("${comebackBonusSettings.noCallbackDays}")
    private int noCallbackDays;

    @Before
    public void prepare() throws InterruptedException {
        msgHandler = new TestcaseSimpleMessageHandler();
        setExecutionContext(msgHandler);

        friend = getProfile(friendId);
        friend.setLevel(2);
        friend.setLastLoginTime(new Date(0));
        userRegistry.setAbandondedFlag(friend, true);
        callbackFriendDao.deleteCallbacksForProfile(friendId);
        updateSync(friend, new Runnable() {
            @Override
            public void run() {
                userProfileDao.clearMeta(friend.getId()); // очищает lastBeingComebackedTime в базе
            }
        });

        profile = getProfile(testerProfileId);
        profile.setLevel(2);
        profile.setComebackedFriends((short) 0);
        profile.setLastCallbackedFriendId(0);
        updateSync(profile);
    }

    @Test
    public void testOnCallBackFriendNoRefererId() throws Exception {
        assertEquals(SimpleResultEnum.SUCCESS, callbackFriendService.callbackFriend(friendId, profile));
        Thread.sleep(1000);

        profile.setOnline(true);
        onSuccessLogin("sessionKey", friend, new Long[0], 0, new String[]{}, false);

        assertNotNull(getComebackAward(msgHandler));
        assertEquals(0, profile.getComebackedFriends());
        assertTrue(friend.isNeedRewardCallbackers());
        assertTrue(callbackFriendDao.isCallbackExists(friendId, testerProfileId));
        CallbackersList callbackersList = msgHandler.getMessage(CallbackersList.class);
        assertNotNull(callbackersList);
        assertArrayEquals(new long[]{testerProfileId}, callbackersList.callbackers);

        softCache.remove(UserProfile.class, testerProfileId);
        assertEquals(SimpleResultEnum.SUCCESS, callbackFriendService.rewardCallbacker(friend, profile.getId()));
        Thread.sleep(1000);
        assertEquals(0, profile.getComebackedFriends());
        profile = getProfile(testerProfileId);
        assertEquals(1, profile.getComebackedFriends());
        assertFalse(friend.isNeedRewardCallbackers());
        assertFalse(callbackFriendDao.isCallbackExists(friendId, testerProfileId));

    }

    @Test
    public void testOnCallBackFriendUseRefererId() throws Exception {
        assertEquals(0, callbackFriendDao.selectCallers(friendId, 1).size());
        assertEquals(SimpleResultEnum.SUCCESS, callbackFriendService.callbackFriend(friendId, profile));
        Thread.sleep(1000);
        assertEquals(1, callbackFriendDao.selectCallers(friendId, Integer.MAX_VALUE).size());
        assertTrue(callbackFriendDao.isCallbackExists(friendId, testerProfileId));

        profile.setOnline(true);

        boolean isNewProfile = false;
        onSuccessLogin("sessionKey", friend, new Long[0], testerProfileId, new String[]{}, isNewProfile);
        List<Object> incomingMessages = msgHandler.getIncomingMessages();
        System.out.println(incomingMessages);
        Thread.sleep(1000);

        assertNotNull(getComebackAward(msgHandler));
        assertEquals(1, profile.getComebackedFriends());
        assertFalse(friend.isNeedRewardCallbackers());
        assertFalse(callbackFriendDao.isCallbackExists(friendId, testerProfileId));

        msgHandler.clear();
        onSuccessLogin("sessionKey", friend, new Long[0], testerProfileId, new String[]{}, isNewProfile);
        assertNull(getComebackAward(msgHandler));
        assertEquals(1, profile.getComebackedFriends());
        assertEquals(SimpleResultEnum.ERROR, callbackFriendService.callbackFriend(friendId, profile));

        msgHandler.clear();
        onSuccessLogin("sessionKey", profile, new Long[0], 0, new String[]{}, isNewProfile);
        assertNotNull(getComebackerAward(msgHandler));
        assertEquals(0, profile.getComebackedFriends());

    }

    @Test
    public void testCallbackFriendFail() throws Exception {
        // нельзя позвать если друг ещё пока заходил недавно
        userRegistry.setAbandondedFlag(friend, false);
        assertEquals(SimpleResultEnum.ERROR, callbackFriendService.callbackFriend(friendId, profile));

        // нельзя позвать подряд того же друга
        userRegistry.setAbandondedFlag(friend, true);
        assertEquals(SimpleResultEnum.SUCCESS, callbackFriendService.callbackFriend(friendId, profile));
        Thread.sleep(1000);
        assertEquals(SimpleResultEnum.ERROR, callbackFriendService.callbackFriend(friendId, profile));

        // нельзя позвать, если он уже вернулся
        onSuccessLogin("sessionKey", friend, new Long[0], testerProfileId, new String[]{}, false);
        assertEquals(SimpleResultEnum.ERROR, callbackFriendService.callbackFriend(friendId, profile));
    }

    @Test
    // нельзя в течение полугода позвать, если он уже вернулся по приглашению (http://jira.pragmatix-corp.com/browse/WORMIX-4273)
    public void testCallbackFriendLimits() throws Exception {
        // 0. зовём друга
        assertEquals(SimpleResultEnum.SUCCESS, callbackFriendService.callbackFriend(friendId, profile));
        Thread.sleep(1000);
        // 1. друг вернулся
        onSuccessLogin("sessionKey", friend, new Long[0], testerProfileId, new String[]{}, false);
        // 2. я пришел и получил за это заслуженную награду
        Thread.sleep(1000);
        msgHandler.clear();
        onSuccessLogin("sessionKey", profile, new Long[0], 0, new String[]{}, false);
        assertNotNull(getComebackerAward(msgHandler));
        assertEquals(0, profile.getComebackedFriends());
        // 3. пусть как будто уже можно позвать: три недели уже истекло и последний позванный не этот
        profile.setLastCallbackedFriendId(0);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -(absentDays + 1));
        friend.setLastLoginTime(cal.getTime());
        updateSync(friend);
        Thread.sleep(1000);
        userRegistry.init(); // делаю полный init, потому что с incrumentUpdateFromDB не получится проверить пересчет isAbandoned из-за lastRun
        // 4. пытаюсь снова позвать - но не могу
        assertEquals(SimpleResultEnum.ERROR, callbackFriendService.callbackFriend(friendId, profile));
        // 5. друг вернулся - я ничего не получаю
        msgHandler.clear();
        onSuccessLogin("sessionKey", profile, new Long[0], 0, new String[]{}, false);
        assertEquals(0, profile.getComebackedFriends());
        msgHandler.clear();
        onSuccessLogin("sessionKey", profile, new Long[0], 0, new String[]{}, false);
        assertNull(getComebackerAward(msgHandler));
        // 6. но если уже прошло полгода
        profile.setLastCallbackedFriendId(0);
        cal.add(Calendar.DAY_OF_YEAR, -noCallbackDays);
        friend.setLastLoginTime(cal.getTime());
        setLastBeingComebackedTime(friend, cal.getTime());
        System.out.println(cal.getTime());
        updateSync(friend);
        Thread.sleep(2000);
        cal.add(Calendar.DAY_OF_YEAR, -2);
        userRegistry.setLastRun(cal.getTime());
        userRegistry.incrumentUpdateFromDB();
        // 7. теперь могу позвать
        assertEquals(SimpleResultEnum.SUCCESS, callbackFriendService.callbackFriend(friendId, profile));
    }

    private LoginAwardStructure getComebackAward(TestcaseSimpleMessageHandler msgHandler) {
        EnterAccount enterAccount = msgHandler.getMessage(EnterAccount.class);
        LoginAwardStructure comebackAward = null;
        for(LoginAwardStructure loginAward : enterAccount.loginAwards) {
            if(loginAward.awardType == AwardTypeEnum.COMEBACK) {
                comebackAward = loginAward;
                break;
            }
        }
        return comebackAward;
    }

    private LoginAwardStructure getComebackerAward(TestcaseSimpleMessageHandler msgHandler) {
        EnterAccount enterAccount = msgHandler.getMessage(EnterAccount.class);
        LoginAwardStructure comebackAward = null;
        for(LoginAwardStructure loginAward : enterAccount.loginAwards) {
            if(loginAward.awardType == AwardTypeEnum.COMEBACKED_FRIEND) {
                comebackAward = loginAward;
                break;
            }
        }
        return comebackAward;
    }

    public void onSuccessLogin(String sessionKey, UserProfile profile, Long[] ids, long referrerId, String[] params, boolean isNewProfile) {
        Login login = new Login();
        login.params = params;
        login.offlineBattles = new EndBattleStructure[0];
        loginService.onSuccessAuthorize(sessionKey, profile, Arrays.asList(ids), referrerId, login, isNewProfile);
    }

    private void updateSync(final UserProfile profile) {
        updateSync(profile, null);
    }

    private void updateSync(final UserProfile profile, @Null Runnable doInTransaction) {
        profile.setDirty(true); // насильно обновить
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                userProfileDao.updateProfile(profile);
                if(doInTransaction != null) {
                    doInTransaction.run();
                }
            }
        });
    }

    private void setLastBeingComebackedTime(UserProfile profile, Date time) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                userProfileDao.setLastBeingComebackedTime(profile.getId(), time);
            }
        });
    }
}
