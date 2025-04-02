package com.pragmatix.app.messages.server;

import com.pragmatix.serialization.annotations.Command;

/**
 * Ответ на команду Ping
 * 
 * Created by IntelliJ IDEA.
 * User: denver
 * Date: 10.02.2010
 * Time: 22:58:10
 */
@Command(10017)
public class Pong {

    @Override
    public String toString() {
        return "Pong{}";
    }

}
