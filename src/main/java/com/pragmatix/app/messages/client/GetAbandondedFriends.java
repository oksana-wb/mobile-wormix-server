package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * Команда запроса друзей, которые давно не заходили в игру
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.09.12 11:35
 *
 *  @see com.pragmatix.app.controllers.CallbackFriendController#onGetAbandondedFriends(GetAbandondedFriends, com.pragmatix.app.model.UserProfile)
 */
@Command(89)
public class GetAbandondedFriends {

    public int page;

    @Override
    public String toString() {
        return "GetAbandondedFriends{" +
                "page=" + page +
                '}';
    }
}
