package com.pragmatix.app.controllers;

import com.pragmatix.achieve.common.BonusItem;
import com.pragmatix.app.achieve.AchieveAwardService;
import com.pragmatix.app.messages.client.SendWipeConfirmCode;
import com.pragmatix.app.messages.client.SyncInvestedAwardPoints;
import com.pragmatix.app.messages.client.WipeProfile;
import com.pragmatix.app.messages.server.SendWipeConfirmCodeResult;
import com.pragmatix.app.messages.server.SyncInvestedAwardPointsResult;
import com.pragmatix.app.messages.server.WipeProfileResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.TestService;
import com.pragmatix.app.services.WipeProfileService;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.intercom.service.AchieveServerAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 14.07.11 10:07
 */
@Controller
public class UserProfileController {

    private static final Logger log = LoggerFactory.getLogger(UserProfileController.class);

    @Resource
    private WipeProfileService wipeProfileService;

    @Resource
    private ProfileService profileService;

    @Resource
    private AchieveServerAPI achieveServerAPI;

    @Resource
    private AchieveAwardService achieveAwardService;

    @Resource
    private TestService testService;

    @Value("${debug.verifyWiping:true}")
    private boolean debugVerifyWiping = true;

    // призовае оружие, выдываемое за очки достижений
    @Value("#{achieveBonusItemsMap}")
    private Map<Integer, BonusItem> achieveBonusItems;

    @OnMessage
    public SendWipeConfirmCodeResult onSendWipeConfirmCode(SendWipeConfirmCode msg, UserProfile profile) throws Exception {
        return wipeProfileService.sendWipeConfirmCode(msg, profile);
    }

    @OnMessage
    public SyncInvestedAwardPointsResult onSyncInvestedAwardPoints(SyncInvestedAwardPoints msg, UserProfile profile) throws Exception {
        int profilesBonusItemsCount = achieveAwardService.countBonusItemInBackpack(profile);

        if(log.isDebugEnabled()) {
            log.debug("user has {} bonus items", profilesBonusItemsCount);
        }

        String profileStringId = profileService.getProfileSocialId(profile);
        achieveServerAPI.syncInvestedAwardPoints(profileStringId, profilesBonusItemsCount);
        return new SyncInvestedAwardPointsResult((byte) profilesBonusItemsCount);
    }

    @OnMessage
    public WipeProfileResult onWipeProfile(WipeProfile msg, UserProfile profile) throws Exception {
        if(!debugVerifyWiping) {
            return testService.applyTestWipeCode(msg, profile);
        } else {
            // обнуление на основе
            return wipeProfileService.validateAndWipe(msg, profile);
        }
    }

}
