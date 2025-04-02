package com.pragmatix.app.services;

import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 31.08.12 9:22
 */
public class BankServiceRegistryTest extends AbstractSpringTest {

    @Resource
    private BankServiceRegistry bankServiceRegistry;

    @Test
    public void testGetBankServiceFor() throws Exception {
        assertEquals(3, bankServiceRegistry.getBankServiceFor((byte) SocialServiceEnum.vkontakte.getType()).getRealMoneByVoites(1));
        assertEquals(600, bankServiceRegistry.getBankServiceFor((byte) SocialServiceEnum.vkontakte.getType()).getMoneByVoites(2));
    }
}
