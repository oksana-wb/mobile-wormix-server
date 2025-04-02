package com.pragmatix.app.services;

import com.pragmatix.app.common.MoneyType;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.common.TeamMemberType;
import com.pragmatix.app.init.StuffCreator;
import com.pragmatix.app.messages.client.AddToGroup;
import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.persist.TempStuffKeeper;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.02.12 10:36
 */
public class StuffServiceTest extends AbstractSpringTest {

    @Resource
    private StuffService stuffService;

    @Resource
    private StuffCreator stuffCreator;

    @Resource
    private GroupService groupService;

    @Resource
    private PermanentStuffService permanentStuffService;

    @Resource
    private TemporalStuffService temporalStuffService;

    @Resource
    private PersistenceService persistenceService;

    @Test
    public void setStuff() throws Exception {
        ArrayList<Stuff> stuffEntities = new ArrayList<>(stuffCreator.getStuffs());
        Stuff newStuff = new Stuff();
        newStuff.setStuffId((short) 1577);
        stuffEntities.add(newStuff);
        stuffCreator.setStuffEntities(stuffEntities);

        short itemId = 1577;
        UserProfile profile1 = getProfile(testerProfileId);
        for(short stuffId : profile1.getStuff()) {
            stuffService.removeStuff(profile1, stuffId);
        }
        stuffService.addStuff(profile1, itemId);
        profileService.updateSync(profile1);

        short itemId2 = 1576;
        long profileId2 = testerProfileId - 1;
        inTransaction(() -> daoService.getWormGroupDao().deleteWormGroups(profileId2));
        softCache.remove(UserProfile.class, profileId2);

        UserProfile profile2 = getProfile(profileId2);
        for(short stuffId : profile2.getStuff()) {
            stuffService.removeStuff(profile2, stuffId);
        }
        stuffService.addStuff(profile2, itemId2);
        profile2.setHat((short) 0);

        AddToGroup msg = new AddToGroup();
        msg.teamMemberId = (int) testerProfileId;
        msg.moneyType = MoneyType.REAL_MONEY;
        msg.teamMemberType = TeamMemberType.Friend;
        profileService.getUserProfileStructure(profile2);
        Assert.assertEquals(ShopResultEnum.SUCCESS, groupService.addToGroup(msg, profile2));
        profile2.getTeamMembers()[1].setHat(itemId2);
        profile2.setTeamMembersDirty(true);

        profileService.updateSync(profile2);
    }

    @Test
    public void testMigrationToTemporalStuffs() throws Exception {
        String keepFileName = "DailyRegistry.tempStuff";
        Map<Long, Set<Short>> map = new HashMap<>();
        Set<Short> set = new HashSet<>();
        short stuff1 = (short) 1027;
        short stuff2 = (short) 1035;
        set.add(stuff1);
        set.add(stuff2);
        map.put(testerProfileId, set);

        final UserProfile profile = getProfile(testerProfileId);
        profile.setStuff(new short[0]);
        profile.setTemporalStuff(new byte[0]);

        permanentStuffService.addStuffFor(profile, stuff1);
        permanentStuffService.addStuffFor(profile, stuff2);

        profileService.updateSync(profile);

        assertTrue(permanentStuffService.isExist(profile, stuff1));
        assertTrue(permanentStuffService.isExist(profile, stuff1));

        assertFalse(temporalStuffService.isExist(profile, stuff1));
        assertFalse(temporalStuffService.isExist(profile, stuff1));

        softCache.remove(UserProfile.class, testerProfileId);

        UserProfile profile2 = getProfile(testerProfileId);

        assertFalse(permanentStuffService.isExist(profile2, stuff1));
        assertFalse(permanentStuffService.isExist(profile2, stuff1));

        assertFalse(temporalStuffService.isExist(profile2, stuff1));
        assertFalse(temporalStuffService.isExist(profile2, stuff1));

        persistenceService.persistObjectToFile(map, keepFileName, new TempStuffKeeper());

        stuffService.init();

        assertTrue(temporalStuffService.isExist(profile2, stuff1));
        assertTrue(temporalStuffService.isExist(profile2, stuff1));

    }

    @Test
    public void testHat() throws Exception {
        SimpleResultEnum result;
        UserProfile profile = getProfile(testerProfileId);
        short hatId = (short) 1000;
        short kitId = (short) 2001;
        profile.setStuff(new short[]{});
        profile.setHat((short) 0);

        result = stuffService.selectHat(profile, profile.getId().intValue(), hatId);
        assertEquals(SimpleResultEnum.ERROR, result);
        assertEquals(0, profile.getHat());

        stuffService.addStuff(profile, hatId);
        assertTrue(stuffService.isExist(profile, hatId));
        assertEquals(hatId, profile.getHat());

        stuffService.deselectHat(profile, profile.getId().intValue());
        assertTrue(stuffService.isExist(profile, hatId));
        assertEquals(0, profile.getHat());

        stuffService.selectHat(profile, profile.getId().intValue(), hatId);
        assertEquals(hatId, profile.getHat());

        stuffService.removeStuff(profile, hatId);
        assertFalse(stuffService.isExist(profile, hatId));
        assertEquals(0, profile.getHat());

        stuffService.addStuff(profile, kitId);
        assertEquals(kitId, profile.getKit());

        result = stuffService.selectHat(profile, profile.getId().intValue(), kitId);
        assertEquals(SimpleResultEnum.ERROR, result);
    }

}
