package com.pragmatix.app.services;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.testcase.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 02.09.2016 14:02
 */
public class RaceServiceTest extends AbstractTest {

    @Test
    public void getRaceExceptExclusive() throws Exception {
        UserProfile profile = new UserProfile(1L);
        profile.setRaces(Race.setRaces(Race.BOXER, Race.BOAR, Race.RHINO, Race.ALIEN));

        profile.setRace(Race.ALIEN);
        Assert.assertEquals(Race.RHINO.getByteType(), RaceService.getRaceExceptExclusive(profile));

        profile.setRace(Race.RHINO);
        Assert.assertEquals(Race.RHINO.getByteType(), RaceService.getRaceExceptExclusive(profile));

        profile.setRace(Race.BOXER);
        Assert.assertEquals(Race.BOXER.getByteType(), RaceService.getRaceExceptExclusive(profile));
    }

}