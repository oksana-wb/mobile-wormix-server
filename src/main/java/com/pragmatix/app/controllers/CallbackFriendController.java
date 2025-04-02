package com.pragmatix.app.controllers;

import com.pragmatix.app.common.Connection;
import com.pragmatix.app.messages.client.CallbackFriend;
import com.pragmatix.app.messages.client.GetAbandondedFriends;
import com.pragmatix.app.messages.client.RewardCallbacker;
import com.pragmatix.app.messages.server.CallbackFriendResult;
import com.pragmatix.app.messages.server.GetAbandondedFriendsResult;
import com.pragmatix.app.messages.server.RewardCallbackerResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.CallbackFriendService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.09.12 11:42
 */
@Controller
public class CallbackFriendController {

    @Resource
    private CallbackFriendService callbackFriendService;

    @Resource
    private ProfileService profileService;

    @OnMessage(value = GetAbandondedFriends.class, connections = {Connection.MAIN})
    public GetAbandondedFriendsResult onGetAbandondedFriends(GetAbandondedFriends msg, UserProfile profile) {

        List<Long> allAbandondedFriends = callbackFriendService.filterFriends(profile);

        List<Long> page = callbackFriendService.getPage(msg.page, allAbandondedFriends);

        // заполняем массив строковых Id
        String[] pageString = new String[page.size()];
        boolean empty = true;
        for(int i = 0; i < page.size(); i++) {
            pageString[i] = profileService.getProfileStringId(page.get(i));
            empty = empty && pageString[i].isEmpty();
        }
        // если все строковые Id пустые - передаем пустой массив
        pageString = empty ? new String[0] : pageString;

        return new GetAbandondedFriendsResult(page.toArray(new Long[page.size()]), pageString, allAbandondedFriends.size());
    }

    @OnMessage(value = CallbackFriend.class, connections = {Connection.MAIN})
    public CallbackFriendResult onCallBackFriend(CallbackFriend msg, UserProfile profile) {
        return new CallbackFriendResult(callbackFriendService.callbackFriend(msg.friendId, profile));
    }

    @OnMessage(value = RewardCallbacker.class, connections = {Connection.MAIN})
    public RewardCallbackerResult onRewardCallbacker(RewardCallbacker msg, UserProfile profile) {
        return new RewardCallbackerResult(callbackFriendService.rewardCallbacker(profile, msg.friendId));
    }

}
