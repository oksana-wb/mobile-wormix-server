package com.pragmatix.app.messages.client;

import com.pragmatix.Commands;
import com.pragmatix.serialization.annotations.Command;

/**
 * запрос арены с клиента
 * 
 * User: denis
 * Date: 03.12.2009
 * Time: 1:48:02
 *
 * @see com.pragmatix.app.controllers.BattleController#onGetArena(GetArena, com.pragmatix.app.model.UserProfile)

 * @see com.pragmatix.app.messages.server.ArenaResult
 * @see com.pragmatix.app.messages.server.UserIsBanned
 */
@Command(Commands.GetArena)
public class GetArena {

    @Override
    public String toString() {
        return "GetArena{}";
    }

}
