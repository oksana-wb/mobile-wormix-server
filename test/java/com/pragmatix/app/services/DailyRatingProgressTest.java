package com.pragmatix.app.services;

import com.pragmatix.app.services.rating.RatingService;
import com.pragmatix.testcase.AbstractSpringTest;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.07.13 19:41
 */
public class DailyRatingProgressTest extends AbstractSpringTest {

    @Resource
    DailyRegistry dailyRegistry;

    @Resource
    RatingService ratingService;

    @Resource
    ProfileService profileService;

//    @Test
//    public void getTopTest() throws InterruptedException {
//        dailyRegistry.getDailyTask().runServiceTask();
//
//        UserProfile user = getProfile(testerProfileId);
//
//        for(int i = 0; i < RatingService.PROGRESS_DEEP; i++) {
//            for(int ii = 0 ; ii < RatingService.PROGRESS_MAX ; ii++) {
//                long profileId = (long) ii + 1;
//                UserProfile profile = getProfile(profileId);
//                ratingService.updateDailyTop(profile, Math.abs(new Random().nextInt(RatingService.PROGRESS_MAX)), BattleWager.NO_WAGER);
//            }
//            ratingService.updateDailyTop(user, Math.abs(new Random().nextInt(RatingService.PROGRESS_MAX)), BattleWager.NO_WAGER);
//
//            ratingService.storeTopPositions();
//            printProgress();
//            System.out.println(ratingService. getDailyProgressMap().size());
//        }
//
//        loginMain();
//
//        sendMain(new GetRating(RatingType.Daily, BattleWager.NO_WAGER));
//        GetRatingResult getRatingResult = receiveMain(GetRatingResult.class, 1000);
//        System.out.println(getRatingResult);
//    }
//
//    @Test
//    public void maxFillTest() throws InterruptedException {
//        dailyRegistry.getDailyTask().runServiceTask();
//
//        for(int i = 0; i < RatingService.PROGRESS_DEEP; i++) {
//            int offset = i * RatingService.PROGRESS_MAX;
//
//            for(int ii = 1 + offset; ii <= RatingService.PROGRESS_MAX + offset; ii++) {
//                long profileId = (long) ii;
//                UserProfile profile = getProfile(profileId);
//                ratingService.updateDailyTop(profile, ii);
//            }
//
//            ratingService.storeTopPositions();
//            printProgress();
//            System.out.println(ratingService.getDailyProgressMap().size());
//
//            assertEquals(RatingService.PROGRESS_MAX + offset, ratingService.getDailyProgressMap().size());
//        }
//
////        ratingService.persistToDisk();
//
//        UserProfile profile = getProfile(testerProfileId);
//        int offset = (RatingService.PROGRESS_DEEP - 1) * RatingService.PROGRESS_MAX;
//        ratingService.updateDailyTop(profile, offset + Math.abs(new Random().nextInt(RatingService.PROGRESS_MAX)));
//        ratingService.storeTopPositions();
//
//        loginMain();
//
//        sendMain(new GetDailyRating(false));
//        GetRatingResult getRatingResult = receiveMain(GetRatingResult.class, 1000);
//        System.out.println(getRatingResult);
//    }
//
//    @Test
//    public void test2() {
//        dailyRegistry.getDailyTask().runServiceTask();
//
//        for(int i = 1; i <= 260; i++) {
//            long profileId = (long) i;
//            UserProfile profile = getProfile(profileId);
//            ratingService.updateDailyTop(profile, i);
//        }
//
//        ratingService.storeTopPositions();
//        printProgress();
//
//        assertEquals(255, ratingService.getDailyProgressMap().size());
//
//        UserProfile profile = getProfile(1l);
//        ratingService.updateDailyTop(profile, 100);
//
//        ratingService.storeTopPositions();
//        printProgress();
//
//        assertEquals(256, ratingService.getDailyProgressMap().size());
//        assertEquals(0, ratingService.getDailyProgressMap().get(6l)[RatingService.PROGRESS_DEEP - 1]);
//
//        for(int i = 0; i < RatingService.PROGRESS_DEEP - 1; i++) {
//            ratingService.storeTopPositions();
//        }
//        printProgress();
//
//        assertEquals(255, ratingService.getDailyProgressMap().size());
//        assertNull(ratingService.getDailyProgressMap().get(6l));
//
//        ratingService.persistToDisk();
//    }
//
//    private void printProgress() {
//        Map<Long, short[]> map = new TreeMap<>(ratingService.getDailyProgressMap());
//        for(Map.Entry<Long, short[]> entry : map.entrySet()) {
//            System.out.println(entry.getKey() + ": " + shortArrToIntArr(entry.getValue()));
//        }
//    }
//
//    @Test
//    public void test() {
//        UserProfile profile1 = getProfile(testerProfileId);
//        UserProfile profile2 = getProfile(testerProfileId - 1);
//        UserProfile profile3 = getProfile(testerProfileId - 2);
//
//        ratingService.updateDailyTop(profile1, 10);
//        ratingService.updateDailyTop(profile2, 20);
//        ratingService.updateDailyTop(profile3, 30);
//
//        ratingService.storeTopPositions();
//
//        for(Map.Entry<Long, short[]> entry : ratingService.getDailyProgressMap().entrySet()) {
//            System.out.println(entry.getKey() + ": " + shortArrToIntArr(entry.getValue()));
//        }
//
//        ratingService.updateDailyTop(profile1, 35);//45
//        ratingService.updateDailyTop(profile2, 20);//40
//        ratingService.updateDailyTop(profile3, 5);//35
//
//
//        ratingService.storeTopPositions();
//
//        for(Map.Entry<Long, short[]> entry : ratingService.getDailyProgressMap().entrySet()) {
//            System.out.println(entry.getKey() + ": " + shortArrToIntArr(entry.getValue()));
//        }
//
//        ratingService.updateDailyTop(profile1, 5);//50
//        ratingService.updateDailyTop(profile2, 20);//60
//        ratingService.updateDailyTop(profile3, 20);//55
//
//
//        ratingService.storeTopPositions();
//
//        for(Map.Entry<Long, short[]> entry : ratingService.getDailyProgressMap().entrySet()) {
//            System.out.println(entry.getKey() + ": " + shortArrToIntArr(entry.getValue()));
//        }
//
//    }

    private String shortArrToIntArr(short[] value) {
        int[] res = new int[value.length];
        for(int i = 0; i < value.length; i++) {
            res[i] = value[i] & 0x0000FFFF;

        }
        return Arrays.toString(res);
    }

}
