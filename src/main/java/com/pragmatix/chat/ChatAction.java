package com.pragmatix.chat;

import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.10.2016 16:02
 */
public enum ChatAction implements TypeableEnum {

    PostToChat(0, /*client*/true, /*canBeRestricted*/true),
    CraftLegendaryItem(1),
    RemoveFromHistory(2),
    PostSticker(3,  /*client*/true, /*canBeRestricted*/false),;

    public final int type;
    public final boolean client;
    public final boolean canBeRestricted;

    ChatAction(int type) {
        this.type = type;
        this.client = false;
        this.canBeRestricted = false;
    }

    ChatAction(int type, boolean client, boolean canBeRestricted) {
        this.type = type;
        this.client = client;
        this.canBeRestricted = canBeRestricted;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }

}
