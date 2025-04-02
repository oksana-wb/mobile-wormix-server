package com.pragmatix.clan;

import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.services.ClanRepoImpl;
import com.pragmatix.clanserver.services.RatingServiceImpl;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.08.13 16:25
 */
public class RatingServiceTest extends AbstractSpringTest {

    @Resource
    private RatingServiceImpl ratingService;

    @Resource
    private ClanRepoImpl clanRepo;

    @Test
    public void clanMembersProgressTest() {
        Clan clan = clanRepo.getClan(420);
        List<ClanMember> members = new ArrayList<>(clan.members());
        Collections.sort(members, new Comparator<ClanMember>() {
            @Override
            public int compare(ClanMember o1, ClanMember o2) {
                return o1.profileId - o2.profileId;
            }
        });
        for(ClanMember member : members) {
            System.out.println(member + ": " + member.oldPlace);
        }
    }

 /*
 420 9096957: [11, 11, 11, 11, 11, 11]
 420 89941395: [6, 6, 6, 6, 6, 6]
 420 91974390: [5, 5, 5, 5, 5, 5]
 420 99437845: [2, 2, 2, 2, 2, 2]
 420 100650918: [7, 7, 7, 7, 7, 7]
 420 137178797: [12, 12, 12, 12, 12, 12]
 420 139105129: [9, 9, 9, 9, 9, 9]
 420 141642205: [4, 4, 4, 4, 4, 4]
 420 164703440: [8, 8, 8, 8, 8, 8]
 420 173935912: [10, 10, 10, 10, 10, 10]
 420 182867297: [3, 3, 3, 3, 3, 3]
 420 197413481: [1, 1, 1, 1, 1, 1]
  */

    @Test
    public void membersProgressTest() {
        ratingService.storeMembersTopPositions();

        TreeMap<Long, byte[]> progressMap = new TreeMap<>(ratingService.getClanMembersProgressMap());
        for(Map.Entry<Long, byte[]> entry : progressMap.entrySet()) {
            int clanId = (int) (entry.getKey() >>> 40);
            int profileId = entry.getKey().intValue();
            System.out.printf("%s %s: %s\n", clanId, profileId, Arrays.toString(entry.getValue()));
        }
    }

    @Test
    public void compressTest() {
        int clanId = 123;
        byte socialId = 1;
        long profileId = testerProfileId;

        long memberId = ((long) clanId) << 40 | ((long) socialId) << 32 | profileId;

        assertEquals((int) (memberId >>> 40), clanId);
        assertEquals((int) (memberId), profileId);
    }

}
