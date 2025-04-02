package com.pragmatix.app.messages.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Позвать друга, который давно не заходил в игру, обратно
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.09.12 13:13
 *
 * @see com.pragmatix.app.controllers.CallbackFriendController#onCallBackFriend(CallbackFriend, com.pragmatix.app.model.UserProfile)
 */
@Command(90)
public class CallbackFriend extends SecuredCommand {

    @Resize(TypeSize.UINT32)
    public long friendId;

    public String sessionKey;

    public CallbackFriend() {
    }

    public CallbackFriend(long friendId) {
        this.friendId = friendId;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "CallbackFriend{" +
                "friendId=" + friendId +
                '}';
    }

}
