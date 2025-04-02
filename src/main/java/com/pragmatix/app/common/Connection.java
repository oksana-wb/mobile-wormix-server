package com.pragmatix.app.common;

/**
 * Класс хранит индекся соединений
 *
 * Created by IntelliJ IDEA.
 * User: denver
 * Date: 22.02.2010
 * Time: 1:21:55
 */
public class Connection {
    /**
     * Главное соедиение
     */
    public static final int MAIN = 0;
    /**
     * Соединение для обработки запросов админки
     */
    public static final int ADMIN = 1;
    /**
     * соединение для PVP битв
     */
    public static final int PVP = 2;
    /**
     * соединение для сервера достижений
     */
    public static final int ACHIEVE = 4;
    /**
     *  HTTP соединение
     */
    public static final int HTTP = 5;
    /**
     * соединение для межсерверного взаимодействия
     */
    public static final int INTERCOM = 6;

}
