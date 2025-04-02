package com.pragmatix.app.services;

import com.pragmatix.app.messages.server.SearchTheHouseResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;

import static com.pragmatix.app.messages.server.SearchTheHouseResult.ResultEnum;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.05.11 18:14
 */
public class SearchTheHouseServiceTest extends AbstractSpringTest {

    @Resource
    SearchTheHouseService service;

    UserProfile friendProfile;
    UserProfile userProfile;

    @Before
    public void init() {
        friendProfile = new UserProfile(1L);
        userProfile = new UserProfile(2L);
    }

    @Test
    public void testSearchToday() {
        friendProfile.setLastSearchTime(new Date());
        friendProfile.setLastLoginTime(new Date());

        ResultEnum result = service.getSearchResult(friendProfile, userProfile.getId());
        assertEquals(ResultEnum.EMPTY, result);
    }

    @Test
    public void testSearchYesterday() {
        friendProfile.setLastLoginTime(new Date());
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        friendProfile.setLastSearchTime(yesterday.getTime());

        ResultEnum result = service.getSearchResult(friendProfile, userProfile.getId());
        assertEquals(ResultEnum.MONEY, result);
    }

    @Test
    public void testNoSearch() {
        friendProfile.setLastSearchTime(null);
        friendProfile.setLastLoginTime(new Date());

        ResultEnum result = service.getSearchResult(friendProfile, userProfile.getId());
        assertEquals(ResultEnum.REAL_MONEY, result);
    }

    @Test
    public void testAbandoned() {
        friendProfile.setLastSearchTime(null);
        Calendar manyDaysAgoCal = Calendar.getInstance();
        manyDaysAgoCal.add(Calendar.DAY_OF_YEAR, -10);
        friendProfile.setLastLoginTime(manyDaysAgoCal.getTime());

        ResultEnum result = service.getSearchResult(friendProfile, userProfile.getId());
        assertEquals(ResultEnum.ABANDONED, result);
    }

    @Test
    public void testGetNorubyBonus() {
        UserProfile profile = getProfile(testerProfileId);
        profile.setLevel(8);

        SearchTheHouseResult reagentBonus = null;

        for(int i = 0; i < 10; i++) {
            SearchTheHouseResult norubyBonus = service.getNoRubyBonus(profile);
            System.out.println(norubyBonus);
            if(norubyBonus.result == ResultEnum.REAGENT) {
                reagentBonus = norubyBonus;
            }
        }

        assertNotNull(reagentBonus);
    }
}
