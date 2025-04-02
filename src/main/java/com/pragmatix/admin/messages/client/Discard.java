package com.pragmatix.admin.messages.client;

import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.sessions.IAppServer;

/**
 * команда по которой сервер будет выкидывать
 * всех кто находится в онлайне и больше никого не пускать
 * 
 * User: denis
 * Date: 13.12.2009
 * Time: 22:02:42
 * <command><commandId>9</commandId><event>0</event></comand>
 * <command><commandId>9</commandId><event>1</event></comand>
 *
 * @see com.pragmatix.pvp.controllers.PvpIntercomController#onDiscard(Discard, com.pragmatix.sessions.IAppServer)
 */
@Command(9)
public class Discard {
    /**
     * выкинуть всех из онлайна и больше не пускать
     */
    public static int DISCARD = 0;
    /**
     * снова разрешить вход на сервер
     */
    public static int UNDISARD = 1;

    public int event;

    public SocialServiceEnum socialService = SocialServiceEnum.steam;

    public Discard() {
    }

    public Discard(int event) {
        this.event = event;
//        this.socialService = socialService;
    }

    @Override
    public String toString() {
        return "Discard{" +
                "event=" + (event == DISCARD ? "DISCARD" : "UNDISARD") +
                '}';
    }

}
