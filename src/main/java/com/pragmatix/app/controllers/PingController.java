package com.pragmatix.app.controllers;

import com.pragmatix.app.common.Connection;
import com.pragmatix.app.messages.client.Ping;
import com.pragmatix.app.messages.server.Pong;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnIdleConnection;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.sessions.IUser;


/**
 * Контроллер для пинга сервера со стороны клиента
 * это необходимо тк часто обрывается соединение с сервером
 * <p/>
 * User: denver
 * Date: 10.02.2010
 * Time: 23:02:52
 */
@Controller
public class PingController {

    private Pong pong = new Pong();

    private Ping ping = new Ping();

    @OnMessage(value = Ping.class, connections = {Connection.MAIN, Connection.PVP, Connection.ADMIN, Connection.ACHIEVE})
    public Pong onPing(Ping msg, IUser profile) {
        return pong;
    }

    /**
     * пингуем соединение если оно простаивает в течении 10 сек
     *
     * @param profile профайл игрока у которого простаивает соединение
     */
    @OnIdleConnection(connections = {Connection.MAIN})
    public void onIdleMainConection(IUser profile) {
        Messages.toUser(ping, profile, Connection.MAIN);
    }

    /**
     * пингуем соединение если оно простаивает в течении 10 сек
     *
     * @param profile профайл игрока у которого простаивает соединение
     */
    @OnIdleConnection(connections = {Connection.PVP})
    public void onIdlePvpConection(IUser profile) {
        Messages.toUser(ping, profile, Connection.PVP);
    }

}
