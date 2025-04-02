package com.pragmatix.app.services;

import com.pragmatix.app.dao.CallbackFriendDao;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.09.12 17:22
 */
public class CallbackFriendServiceTest extends AbstractSpringTest {

    @Resource
    private CallbackFriendService service;

    @Resource
    private CallbackFriendDao dao;

    @Resource
    private SoftCache softCache;

    @Test
    public void testCallBackFriend() throws Exception {
        dao.deleteCallbacksForProfile(testerProfileId - 1);
        dao.deleteCallbacksForProfile(testerProfileId - 2);
        Thread.sleep(300);

        assertEquals(0, dao.selectCallers(testerProfileId - 1, Integer.MAX_VALUE).size());
        assertEquals(0, dao.selectCallers(testerProfileId - 2, Integer.MAX_VALUE).size());

        UserProfile testerProfile = softCache.get(UserProfile.class, testerProfileId);
        service.callbackFriend(testerProfileId - 1, testerProfile);
        service.callbackFriend(testerProfileId - 2, testerProfile);
        Thread.sleep(300);

        assertEquals(1, dao.selectCallers(testerProfileId - 1, Integer.MAX_VALUE).size());
        assertEquals(1, dao.selectCallers(testerProfileId - 2, Integer.MAX_VALUE).size());

        assertTrue(dao.isCallbackExists(testerProfileId - 1, testerProfileId));
        assertTrue(dao.isCallbackExists(testerProfileId - 2, testerProfileId));

    }


}
