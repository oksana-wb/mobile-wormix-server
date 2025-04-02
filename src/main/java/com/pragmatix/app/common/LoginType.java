package com.pragmatix.app.common;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.11.2015 12:31
 */
public enum LoginType {
    regular, // обычный логин
    comeback,// логин после долгого перерыва (30 дней)
    registration, // логин с регистрацией нового пользователя
}
