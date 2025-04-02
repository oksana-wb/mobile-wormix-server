package com.pragmatix.intercom.service;

import com.pragmatix.achieve.award.AchieveAward;
import com.pragmatix.achieve.award.WormixAchieveAward;
import com.pragmatix.achieve.common.GrantAwardResultEnum;
import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.messages.client.BuyResetBonusItems;
import com.pragmatix.achieve.messages.client.ChooseBonusItem;
import com.pragmatix.achieve.messages.client.IncreaseAchievements;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.messages.structures.AchieveAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.intercom.messages.GetIntercomProfileRequest;
import com.pragmatix.intercom.messages.IntercomAchieveRequest;
import com.pragmatix.intercom.structures.UserProfileIntercomStructure;
import com.pragmatix.sessions.AppServerAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.pragmatix.intercom.messages.GetIntercomProfileRequest.GetIntercomProfileRequest;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 10.08.2016 12:08
 */
@Service
public class MainServerAPI extends ServerAPI {

    private AppServerAddress mainAppServerAddress;

    private Map<SocialServiceEnum, AppServerAddress> appServersAddressMap = new EnumMap<>(SocialServiceEnum.class);

    @Value("${MainServerAPI.mainAppServerAddress}")
    public void setMainAppServerAddress(String mainAppServerAddress) {
        this.mainAppServerAddress = new AppServerAddress(mainAppServerAddress);
    }

    @Value("${MainServerAPI.vkontakteAppServerAddress}")
    public void setVkontakteAppServerAddress(String mainAppServerAddress) {
        appServersAddressMap.put(SocialServiceEnum.vkontakte, new AppServerAddress(mainAppServerAddress));
    }

    @Value("${MainServerAPI.odnoklassnikiAppServerAddress}")
    public void setOdnoklassnikiAppServerAddress(String mainAppServerAddress) {
        appServersAddressMap.put(SocialServiceEnum.odnoklassniki, new AppServerAddress(mainAppServerAddress));
    }

    @Value("${MainServerAPI.mailruAppServerAddress}")
    public void setMailruAppServerAddress(String mainAppServerAddress) {
        appServersAddressMap.put(SocialServiceEnum.mailru, new AppServerAddress(mainAppServerAddress));
    }

    @Value("${MainServerAPI.facebookAppServerAddress}")
    public void setFacebookAppServerAddress(String mainAppServerAddress) {
        appServersAddressMap.put(SocialServiceEnum.facebook, new AppServerAddress(mainAppServerAddress));
    }

    // запросить основной сервер на предмет выдачи награды за достижения
    public void grantAchieveAward(ProfileAchievements profileAchievements, IncreaseAchievements msg, List<AchieveAward> needGrantAwards) {
        List<AchieveAwardStructure> achieveAwardStructureList = needGrantAwards.stream()
                .map(achieveAward -> (WormixAchieveAward) achieveAward)
                .map(AchieveAwardStructure::new)
                .collect(Collectors.toList());
        Messages.toServer(
                IntercomAchieveRequest.GrantAwardsRequest(profileAchievements.getProfileId(), msg.timeSequence, achieveAwardStructureList, profileAchievements.remoteServerRequestNum.incrementAndGet()),
                mainAppServerAddress, true
        );
    }

    public GrantAwardResultEnum giveBonusItem(ProfileAchievements profileAchievements, ChooseBonusItem msg, int awardType) {
        IntercomAchieveRequest request = IntercomAchieveRequest.GiveBonusItemRequest(profileAchievements.getProfileId(), msg.itemId, awardType, profileAchievements.remoteServerRequestNum.incrementAndGet());
        return ask(request, profileAchievements.remoteServerRequestQueue, mainAppServerAddress, msg, GrantAwardResultEnum.ERROR);
    }

    public ShopResultEnum buyResetBonusItems(ProfileAchievements profileAchievements, BuyResetBonusItems msg) {
        IntercomAchieveRequest request = IntercomAchieveRequest.BuyResetBonusItems(profileAchievements.getProfileId(), profileAchievements.remoteServerRequestNum.incrementAndGet());
        return ask(request, profileAchievements.remoteServerRequestQueue, mainAppServerAddress, msg, ShopResultEnum.ERROR);
    }

    public Optional<UserProfileIntercomStructure> requestUserProfileIntercomStructure(UserProfile profile, SocialServiceEnum remoteMainServer, long remoteProfileId, boolean banRemoteProfile, String secureToken, Object causeRequest) {
        GetIntercomProfileRequest request = GetIntercomProfileRequest(remoteProfileId, profile, banRemoteProfile, secureToken);
        return ask(request, profile.remoteServerRequestQueue, appServersAddressMap.get(remoteMainServer), causeRequest, Optional.empty(), 2);
    }

}
