package com.pragmatix.app.services;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.messages.structures.BundleStructure;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.testcase.AbstractSpringTest;
import io.vavr.Tuple2;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.07.2015 16:23
 */
public class BundleServiceTest extends AbstractSpringTest {

    @Resource
    BundleService bundleService;

    @Resource
    SkinService skinService;

    @Test
    public void issueBundleTest() throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        UserProfile profile = getProfile(testerProfileId);
        profile.setRace(Race.BOXER);
        profile.setRaces(Race.addRace((short)0, Race.BOXER));

        String races = "2 5";
        String skins = "21 22 31 51 52";
        bundleService.persistBundle(11L, "00", 0, new Date(), cal.getTime(), 0, 1, races, skins, "", false);
        BundleStructure validBundle = bundleService.getBundle("00");
        Tuple2<List<GenericAwardStructure>, Integer> tuple2 = bundleService.issueBundle(profile, validBundle);
        println(tuple2);

        Assert.assertTrue(tuple2._2 > 0);
        Assert.assertTrue(profile.inRace(Race.ZOMBIE));

        Assert.assertTrue(skinService.haveSkin(profile, (byte)21));
        Assert.assertTrue(skinService.haveSkin(profile, (byte)22));

        Assert.assertFalse(skinService.haveSkin(profile, (byte)31));

        Assert.assertTrue(skinService.haveSkin(profile, (byte)51));
        Assert.assertTrue(skinService.haveSkin(profile, (byte)52));

        Assert.assertEquals(52, skinService.getSkin(profile));
    }
}