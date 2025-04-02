package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * Команда пинга сервера, для устранения баги с закрытием соединения
 *
 * User: denver
 * Date: 10.02.2010
 * Time: 22:57:56
 *
 * @see com.pragmatix.app.controllers.PingController#onPing(Ping, com.pragmatix.sessions.IUser)
 *
 */
@Command(16)
public class Ping {

    @Override
    public String toString() {
        return "Ping{}";
    }

}
