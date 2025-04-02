package com.pragmatix.arena.coliseum;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 15.02.2018 11:30
 */
public class ColiseumServiceTest extends AbstractSpringTest {

    @Autowired
    ColiseumService coliseumService;

    @Test
    public void getColiseumEntityTest() {
        UserProfile profile = profileService.getUserProfile(16210154);
        ColiseumEntity result = coliseumService.coliseumEntity(profile);
        System.out.println(result);
    }
}