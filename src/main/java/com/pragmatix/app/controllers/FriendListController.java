package com.pragmatix.app.controllers;

import com.pragmatix.app.messages.client.GetFriendListPage;
import com.pragmatix.app.messages.client.GetFriendsForMission;
import com.pragmatix.app.messages.server.GetFriendListPageResult;
import com.pragmatix.app.messages.server.GetFriendsForMissionResult;
import com.pragmatix.app.messages.structures.SimpleProfileStructure;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.FriendsListService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * контроллер для обработки запросов на взятие списка друзей
 * <p/>
 * User: denis
 * Date: 17.04.2010
 * Time: 17:27:39
 */
@Controller
public class FriendListController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private FriendsListService friendsListService;

    @Resource
    private ProfileService profileService;

    @OnMessage
    public Object onGetFriendListPage(GetFriendListPage msg, UserProfile profile) {
        Collection<UserProfile> friendProfiles = friendsListService.getPage(profile, msg.pageIndex);
        List<UserProfileStructure> friendsProfileStructures = profileService.getUserProfileStructures(friendProfiles);
        friendsProfileStructures.stream().peek(friendsProfileStructure ->
                friendsProfileStructure.profileStringId = profileService.getProfileStringId(friendsProfileStructure.id)
        );
        return new GetFriendListPageResult(friendsProfileStructures);
    }

    @OnMessage
    public GetFriendsForMissionResult onGetFriendsForMission(GetFriendsForMission msg, UserProfile profile) {
        Pair<List<UserProfile>, List<GetFriendsForMissionResult.FriendState>> resultPair;
        if(msg.missionIds.length == 1) {
            resultPair = friendsListService.getFriendsForMission(profile, msg.missionIds[0]);
        } else if(msg.missionIds.length == 2) {
            resultPair = friendsListService.getFriendsForSuperBossMission(profile, msg.missionIds);
        } else {
            log.error("невалидное поле missionIds в команде команде {}", msg);
            return null;
        }

        GetFriendsForMissionResult result = new GetFriendsForMissionResult();
        List<UserProfile> userProfiles = resultPair.getLeft();
        result.friends = userProfiles.stream()
                .map(userProfile -> new SimpleProfileStructure(userProfile, profileService.clanMember_rank_skin(userProfile)))
                .collect(Collectors.toList());
        result.states = resultPair.getRight();

        return result;
    }

}
