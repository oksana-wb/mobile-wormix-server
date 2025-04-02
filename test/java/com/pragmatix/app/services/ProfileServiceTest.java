package com.pragmatix.app.services;

import com.google.gson.Gson;
import com.pragmatix.app.domain.BanEntity;
import com.pragmatix.app.domain.TrueSkillEntity;
import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.social.SocialUserIdMapService;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.serialization.SerializeContext;
import com.pragmatix.testcase.AbstractSpringTest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 10.05.11 9:55
 */
public class ProfileServiceTest extends AbstractSpringTest {

    @Resource
    BanService banService;

    @Resource
    TrueSkillService trueSkillService;

    @Resource
    private SocialUserIdMapService socialUserIdMap;

    @Test
    public void assignStingIdToProfileTest() {
        daoService.doInTransactionWithoutResult(() -> jdbcTemplate.execute("TRUNCATE wormswar.social_id"));
        socialUserIdMap.init();

        UserProfile profileOne = profileService.getUserProfile(testerProfileId);
        UserProfile profileTwo = profileService.getProfileOrCreate(testerProfileId - 2, new String[0])._1;

        profileService.assignStingIdToProfile(profileOne, "first", SocialServiceEnum.vkontakte);
        profileService.assignStingIdToProfile(profileTwo, "second", SocialServiceEnum.vkontakte);

        profileService.assignStingIdToProfile(profileTwo, "first", SocialServiceEnum.vkontakte);
    }

    @Test
    public void profileToJsonTest() {
        UserProfile profile = profileService.getUserProfile(testerProfileId);
        UserProfileStructure userProfileStructure = profileService.getUserProfileStructure(profile);
        TrueSkillEntity trueSkillEntity = trueSkillService.getTrueSkillFor(profile);

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("id", profile.getId());
        resultMap.put("socialId", profile.getProfileStringId());
        resultMap.put("banned", banService.isBanned(profile.getId()));
        resultMap.put("level", profile.getLevel());
        resultMap.put("money", profile.getMoney());
        resultMap.put("realMoney", profile.getRealMoney());
        resultMap.put("rating", profile.getRating());
        resultMap.put("lastLoginTime", AppUtils.formatDate(profile.getLastLoginTime()));
        resultMap.put("reactionRate", profile.getReactionRate());
        resultMap.put("boss", profile.getCurrentMission());
        resultMap.put("superBoss", profile.getCurrentNewMission());
        resultMap.put("skill", Math.round((trueSkillEntity.getMean() - trueSkillEntity.getStandardDeviation() * (double) 3) * (double) 500));
        resultMap.put("team", userProfileStructure.wormsGroup);
        resultMap.put("clanId", profile.getClanId());
        resultMap.put("rank", profile.getRankInClan());

        String resultJson = new Gson().toJson(resultMap);
        println(resultJson);
    }

    @Test
    public void bansToJsonTest() {
        UserProfile profile = profileService.getUserProfile(testerProfileId);

        List<BanEntity> banEntities = daoService.getBanDao().selectBanEntities(profile.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for(BanEntity banEntity : banEntities) {
            Map<String, Object> resultMap = new LinkedHashMap<>();

            resultMap.put("startDate", AppUtils.formatDate(banEntity.getStartDate()));
            resultMap.put("endDate", AppUtils.formatDate(banEntity.getEndDate()));
            resultMap.put("note", banEntity.getNote());
            resultMap.put("admin", banEntity.getAdmin());
            resultMap.put("type", banEntity.getType());

            result.add(resultMap);
        }

        String resultJson = new Gson().toJson(result);
        println(resultJson);
    }

    @Test
    public void logReferrerTest() throws InterruptedException {
        long id = Math.abs(new Random().nextInt());
        String referrerValue = "development";
        String[] params = new String[]{ILogin.REFERRER_PARAM_NAME, referrerValue};

        profileService.getProfileOrCreate(id, params);

        Thread.sleep(2000);

        String result = jdbcTemplate.queryForObject("select referrer from wormswar.creation_date where id = " + id, String.class);

        assertEquals(referrerValue, result);
    }

    @Test
    public void logReferrerTest2() throws Exception {
        List<Integer> profiles = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            profiles.add(new Random().nextInt(100000) + 1);
        }
        for(int profileId : profiles) {
            loginMain(profileId);
            Thread.sleep(2000);
            disconnectMain();
        }
        for(int profileId : profiles) {
            loginMain(profileId);
            Thread.sleep(2000);
            disconnectMain();
        }
    }

    @Test
    public void test() throws IOException {
        UserProfile userProfile = profileService.getUserProfile(testerProfileId);
        UserProfileStructure userProfileStructure = profileService.getUserProfileStructure(userProfile);
        ByteBuf buffer = Unpooled.buffer();
        binarySerializer.getStructureSerializer(com.pragmatix.app.messages.structures.UserProfileStructure.class, com.pragmatix.app.messages.structures.UserProfileStructure.class)
                .serializeStructure(userProfileStructure, buffer, new SerializeContext());
        userProfileStructure = binarySerializer.getStructureSerializer(com.pragmatix.app.messages.structures.UserProfileStructure.class, com.pragmatix.app.messages.structures.UserProfileStructure.class)
                .deserializeStructure(buffer, new SerializeContext());
        Gson gson = new Gson();
        String json = gson.toJson(userProfileStructure);
        System.out.println(json);
    }

}
