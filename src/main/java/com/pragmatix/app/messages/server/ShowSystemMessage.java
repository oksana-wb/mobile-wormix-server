package com.pragmatix.app.messages.server;

import com.pragmatix.serialization.annotations.Command;

/**
 * Команда клиенту о том, чтобы он отобразил системное сообщение
 * 
 * User: denis
 * Date: 13.12.2009
 * Time: 22:45:22
 */
@Command(10011)
public class ShowSystemMessage {

    public String msg;

    public ShowSystemMessage() {
    }

    public ShowSystemMessage(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "ShowSystemMessage{" +
                "msg='" + msg + '\'' +
                '}';
    }
}
