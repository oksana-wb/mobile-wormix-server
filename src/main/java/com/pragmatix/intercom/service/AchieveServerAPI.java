package com.pragmatix.intercom.service;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.intercom.messages.IntercomAchieveRequest;
import com.pragmatix.intercom.structures.ProfileAchieveStructure;
import com.pragmatix.sessions.AppServerAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 10.08.2016 13:01
 */
@Service
public class AchieveServerAPI extends ServerAPI {

    @Resource
    private ProfileService profileService;

    private AppServerAddress achieveAppServerAddress;

    @Value("${AchieveServerAPI.achieveAppServerAddress}")
    public void setAchieveAppServerAddress(String mainAppServerAddress) {
        this.achieveAppServerAddress = new AppServerAddress(mainAppServerAddress);
    }

    public void syncInvestedAwardPoints(String profileStringId, int profilesBonusItemsCount) {
        Messages.toServer(IntercomAchieveRequest.SetInvestedAwardPoints(profileStringId, (byte) profilesBonusItemsCount, -1), achieveAppServerAddress, true);
    }

    public void wipeAchievements(String profileStringId) {
        Messages.toServer(IntercomAchieveRequest.WipeAchievements(profileStringId, -1), achieveAppServerAddress, true);
    }

    public ProfileAchieveStructure getAchievements(UserProfile profile) {
        String profileStringId = profileService.getProfileAchieveId(profile.getProfileId());
        IntercomAchieveRequest request = IntercomAchieveRequest.GetAchievements(profileStringId, profile.remoteServerRequestNum.incrementAndGet());
        return ask(request, profile.remoteServerRequestQueue, achieveAppServerAddress, request, (ProfileAchieveStructure) null);
    }

}
