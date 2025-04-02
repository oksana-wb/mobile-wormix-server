package com.pragmatix.app.filters;

import com.pragmatix.app.messages.client.LoginByProfileStringId;
import com.pragmatix.app.messages.client.Ping;
import com.pragmatix.app.messages.server.Pong;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.security.annotations.Authenticate;
import com.pragmatix.gameapp.security.annotations.Filter;
import com.pragmatix.sessions.ISessionContainer;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.06.2017 14:57
 */
@Filter
public class TestFilter {

    @Authenticate(Ping.class)
    public ISessionContainer onPing(Ping msg) {
        Messages.toUser(new Pong());
        return null;
    }

}
