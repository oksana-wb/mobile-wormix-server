package com.pragmatix.chat;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.chat.messages.*;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 06.10.2016 18:06
 */
@Controller
public class GlobalChatController {

    @Resource
    private GlobalChatService chatService;

    @OnMessage
    public JoinToChatResult onJoinToChat(JoinToChat msg, UserProfile profile) {
        return new JoinToChatResult(chatService.joinToChat(profile));
    }

    @OnMessage
    public void onLeaveFromChat(LeaveFromChat msg, UserProfile profile) {
        chatService.leaveFromChat(profile);
    }

    @OnMessage
    public ChatMessageEvent onPostToChat(PostToChat msg, UserProfile profile) {
        return chatService.postToChat(profile, msg.action, msg.name, msg.message);
    }

}
