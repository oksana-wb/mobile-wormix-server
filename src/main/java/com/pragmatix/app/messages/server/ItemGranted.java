package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 04.02.14 14:54
 */
@Command(10104)
public class ItemGranted implements SecuredResponse {

    public int itemId;

    public int itemCount;

    public String sessionKey;

    public ItemGranted() {
    }

    public ItemGranted(int itemId, int itemCount, String sessionKey) {
        this.itemId = itemId;
        this.itemCount = itemCount;
        this.sessionKey = sessionKey;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "ItemGranted{" +
                "itemId=" + itemId +
                ", itemCount=" + itemCount +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
