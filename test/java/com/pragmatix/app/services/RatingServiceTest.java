package com.pragmatix.app.services;

import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.rating.Division;
import com.pragmatix.app.services.rating.RatingServiceImpl;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Created: 25.04.11 15:11
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class RatingServiceTest extends AbstractSpringTest {

    @Resource
    private RatingServiceImpl ratingService;

    @Test
    public void updateRating() {
        Division rubyDivision = ratingService.getRubyDivision();
        rubyDivision.init(new ArrayList<>());
        Division seasonRubyDivision = ratingService.getSeasonRubyDivision();
        seasonRubyDivision.init(new ArrayList<>());

        UserProfile profile1 = getProfile(testerProfileId);
        UserProfile profile2 = getProfile(testerProfileId - 1);
        int ratingPoints;

        ratingPoints = 25;
        ratingService.addRating(profile1, ratingPoints);
        printTopStrusture(rubyDivision);

        ratingPoints = 30;
        ratingService.addRating(profile2, ratingPoints);
        printTopStrusture(rubyDivision);

        ratingPoints = 10;
        ratingService.addRating(profile1, ratingPoints);
        printTopStrusture(rubyDivision);

        ratingPoints = 25;
        ratingService.addSeasonRating(profile1, ratingPoints);
        printTopStrusture(seasonRubyDivision);

        ratingPoints = 30;
        ratingService.addSeasonRating(profile2, ratingPoints);
        printTopStrusture(seasonRubyDivision);

        ratingPoints = 10;
        ratingService.addSeasonRating(profile1, ratingPoints);
        printTopStrusture(seasonRubyDivision);

        printTopStrusture(rubyDivision);
    }


    public void printTopStrusture(Division division) {
        System.out.println(division.getRatingList());
    }

//    @Before
//    public void init() {
//        ArrayList<League> leagues = new ArrayList<League>();
//        leagues.add(new League(Integer.MIN_VALUE, 1000, 10)); // лига новичков
//        leagues.add(new League(1000, 3000, 1));              // лига ветеранов
//        leagues.add(new League(3000, 5000, 1));              // лига мастеров
//        leagues.add(new League(5000, 10000, 1));             // лига асов
//        leagues.add(new League(10000, Integer.MAX_VALUE, 1));// рубиновая лига
//        ratingService.setLeagues(leagues);
//        ratingService.createDivisions();
//
//        RatingService.MAX_TOP = 5;
//
//        List<UserProfile> profiles = new ArrayList<UserProfile>();
//        addProfile(profiles, 10, 101);
//        addProfile(profiles, 20, 102);
//        addProfile(profiles, 30, 103);
//        addProfile(profiles, 40, 104);
//        addProfile(profiles, 50, 105);
//
//        addProfile(profiles, 110, 1101);
//        addProfile(profiles, 120, 1102);
//        addProfile(profiles, 130, 1103);
//        addProfile(profiles, 140, 1104);
//        addProfile(profiles, 150, 1105);
//
//        for(UserProfile profile : profiles) {
//            Division division = ratingService.getDivisionForUser(profile);
//            division.addUser(profile, new ClanMemberI() {
//                @Override
//                public Rank getRank() {
//                    return Rank.SOLDIER;
//                }
//
//                @Override
//                public int getClanId() {
//                    return 0;
//                }
//
//                @Override
//                public String getClanName() {
//                    return "";
//                }
//
//                @Override
//                public byte[] getClanEmblem() {
//                    return new byte[0];
//                }
//
//                @Override
//                public int getClanRating() {
//                    return 0;
//                }
//
//                @Override
//                public int getSeasonClanRating() {
//                    return 0;
//                }
//
//                @Override
//                public ReviewState getReviewState() {
//                    return ReviewState.APPROVED;
//                }
//
//                @Override
//                public int getPrevSeasonTopPlace() {
//                    return 0;
//                }
//            });
//        }
//
//        ratingService.reinit();
//    }
//
//    @Test
//    public void test1() {
//        int incRatinf = 0;
//        ratingService.checkAndAddInRating(newProfile(60, 106), true, incRatinf, BattleWager.NO_WAGER);
//        RatingProfileStructure ratingProfileStructure = ratingService.getRatingProfileStructure(newProfile(10, 101));
//        assertNull(ratingProfileStructure);
//
//        RatingProfileStructure[] list = ratingService.getRatingList(newProfile(100, 107));
//        assertArrayEquals(new long[]{60l, 50l, 40l, 30l, 20l}, getIdArray(list));
//
//        ratingService.checkAndAddInRating(newProfile(150, 102), true, incRatinf, BattleWager.NO_WAGER);
//        ratingProfileStructure = ratingService.getRatingProfileStructure(newProfile(150, 102));
//        assertNull(ratingProfileStructure);
//
//        ratingService.checkAndAddInRating(newProfile(150, 10002), true, incRatinf, BattleWager.NO_WAGER);
//        list = ratingService.getRatingList(newProfile(150, 10002));
//        assertArrayEquals(new long[]{150l}, getIdArray(list));
//
//        ratingService.removeUser(newProfile(150, 10002));
//        assertFalse(ratingService.contains(newProfile(150, 10002)));
//
//        ratingService.checkAndAddInRating(newProfile(15, 102), true, incRatinf, BattleWager.NO_WAGER);
//        list = ratingService.getRatingList(newProfile(15, 102));
//        assertArrayEquals(new long[]{15l}, getIdArray(list));
//    }

    private void addProfile(List<UserProfile> profiles, long id, int rating) {
        UserProfile profile = newProfile(id, rating);
        profiles.add(profile);
    }


    private UserProfile newProfile(long id, int rating) {
        UserProfile profile = new UserProfile(id);
        profile.setRating(rating);
        return profile;
    }

    private long[] getIdArray(RatingProfileStructure[] list) {
        long[] result = new long[list.length];
        int i = 0;
        for(RatingProfileStructure structure : list) {
            result[i++] = structure.id;
        }
        return result;
    }

}
