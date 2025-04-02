package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Позвать друга, который давно не заходил в игру, обратно
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.09.12 13:13
 *
 * @see com.pragmatix.app.controllers.CallbackFriendController#onRewardCallbacker(RewardCallbacker, com.pragmatix.app.model.UserProfile)
 */
@Command(92)
public class RewardCallbacker {

    @Resize(TypeSize.UINT32)
    public long friendId;

    @Override
    public String toString() {
        return "RewardCallbacker{" +
                "friendId=" + friendId +
                '}';
    }

}
