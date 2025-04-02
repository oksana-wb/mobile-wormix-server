package com.pragmatix.app.services;

import com.pragmatix.app.messages.server.EnterAccount;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 10.05.11 18:29
 */
public class FriendsListServiceTest extends AbstractSpringTest {

    @Test
    public void testStubProfiles() throws Exception {
        loginMain(testerProfileId, testerProfileId - 1, 1L);
        EnterAccount enterAccount = receiveMain(EnterAccount.class);
        UserProfileStructure friend1 = enterAccount.profileStructures.get(0);
        UserProfileStructure friend2 = enterAccount.profileStructures.get(1);

        assertEquals(testerProfileId - 1, friend1.id);
        assertTrue(friend1.wormsGroup[0].level > 0);

        assertEquals(1L, friend2.id);
        assertTrue(friend2.wormsGroup.length == 0);
    }

}
