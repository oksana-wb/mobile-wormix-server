package com.pragmatix.pvp.messages.handshake.client;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.annotations.Command;

import java.util.*;

/**
 * Рассширить критерии поиска оппонента/партнера
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 14.05.13 11:26
 *
 * @see com.pragmatix.pvp.controllers.PvpController#onWidenSearch(WidenSearch, com.pragmatix.pvp.model.PvpUser)
 */
@Command(1008)
public class WidenSearch implements PvpCommandI {

    @Override
    public long getBattleId() {
        return 0;
    }

    @Override
    public String toString() {
        return "WidenSearch{}";
    }
}
