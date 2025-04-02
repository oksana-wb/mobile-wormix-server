package com.pragmatix.notify;

import com.pragmatix.app.common.Connection;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.chat.GlobalChatService;
import com.pragmatix.chat.messages.ChatMessage;
import com.pragmatix.chat.messages.JoinToChatResult;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.notify.message.NotifyProfile;
import com.pragmatix.notify.message.RegisterForNotify;
import com.pragmatix.notify.message.SetLocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.validation.constraints.Null;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.08.13 14:43
 */
@Controller
public class NotifyController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private Registrator registrator;

    @Resource
    private NotifyService notifyService;

    @Resource
    private ProfileService profileService;

    @Resource
    private GlobalChatService globalChatService;

    @Null
    @OnMessage(value = RegisterForNotify.class)
    public Object onRegisterForNotify(RegisterForNotify msg, UserProfile profile) {
        if(log.isDebugEnabled()) {
            log.debug("message in: " + msg);
        }

        Long profileId = profile.getId();
        short socialNetId = (short) msg.socialNet.getType();
        String registrationId = msg.registrationId;

        registrator.registrate(profileId, socialNetId, registrationId);

        return null;
    }


    @Null
    @OnMessage(value = NotifyProfile.class)
    public Object onNotifyProfile(NotifyProfile msg, UserProfile profile) {
        UserProfile recipientProfile = profileService.getUserProfile(msg.recipientProfileId);

        if(recipientProfile != null) {
            notifyService.send(msg.recipientProfileId, recipientProfile.getLocale(), msg.delay <= 0 ? 0 : msg.delay * 1000, msg.timeToLive, msg.localizedKey, msg.localizedArguments);
        }

        return null;
    }

    @OnMessage(value = SetLocale.class)
    public JoinToChatResult onSetLocale(SetLocale msg, UserProfile profile) {
        ChatMessage[] chatHistory = globalChatService.setLocaleAndChangeChatRoom(msg, profile);
        return new JoinToChatResult(chatHistory);
    }


}
