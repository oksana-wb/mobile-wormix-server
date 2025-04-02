package com.pragmatix.app.controllers;

import com.pragmatix.app.messages.client.SearchTheHouse;
import com.pragmatix.app.messages.server.SearchTheHouseResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.CheatersCheckerService;
import com.pragmatix.app.services.DailyRegistry;
import com.pragmatix.app.services.SearchTheHouseService;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.08.11 17:20
 */
public class SearchTheHouseControllerTest extends AbstractSpringTest {

    @Resource
    private SearchTheHouseController searchTheHouseController;

    @Resource
    private SearchTheHouseService searchTheHouseService;

    @Resource
    private DailyRegistry dailyRegistry;

    protected UserProfile friendProfile;

    @Before
    public void setUp() {
        searchTheHouseController.setCheatersCheckerService(new CheatersCheckerService() {
            @Override
            public boolean checkSearchHouseDelay(UserProfile profile) {
                return false;
            }
        });

        friendProfile = createUserProfile();
    }

    @Test
    public void testOnSearchTheHouse() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        dailyRegistry.setSearchKeys(profile.getId(), (byte) 10);

        int realMoney = profile.getRealMoney();

        friendProfile.setLevel(5);
        searchTheHouseService.fireLevelUp(friendProfile);

        SearchTheHouse msg = new SearchTheHouse();
        msg.friendId = friendProfile.getId();
        msg.keyNum = 10;

        SearchTheHouseResult result = searchTheHouseController.onSearchTheHouse(msg, profile);

        assertEquals(SearchTheHouseResult.ResultEnum.REAL_MONEY.getType(), result.result);
        assertEquals(9, result.availableSearchKeys);
        assertNotNull(friendProfile.getLastSearchTime());
        assertEquals(realMoney + 1, profile.getRealMoney());

        realMoney = profile.getRealMoney();
        //============

        msg.keyNum = 9;

        result = searchTheHouseController.onSearchTheHouse(msg, profile);

        assertEquals(SearchTheHouseResult.ResultEnum.EMPTY.getType(), result.result);
        assertEquals(8, result.availableSearchKeys);
        assertEquals(realMoney, profile.getRealMoney());

        //============

        dailyRegistry.setSearchKeys(profile.getId(), (byte) 0);

        result = searchTheHouseController.onSearchTheHouse(msg, profile);

        assertEquals(SearchTheHouseResult.ResultEnum.KEY_LIMIT_EXCEED.getType(), result.result);
        assertEquals(0, result.availableSearchKeys);
        assertEquals(realMoney, profile.getRealMoney());
    }

}
