package com.pragmatix.pvp.messages.battle.client;

import com.pragmatix.pvp.messages.PvpCommandI;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.06.11 16:10
 */
public interface CountedCommandI extends PvpCommandI {

    short getCommandNum();

    short getTurnNum();

    byte getPlayerNum();

}
