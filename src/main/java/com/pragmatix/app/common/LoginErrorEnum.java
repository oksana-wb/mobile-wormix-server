package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.07.11 10:36
 */
public enum LoginErrorEnum implements TypeableEnum {
    INDEFINITE(-1),
    /**
     * ведутся профилактические работы
     */
    PROPHYLACTIC_WORK(0),
    /**
     * ошибка ключа ( для логина по ключу )
     */
    INCORRECT_KEY(1),
    /**
     * сессия уже существует.
     * Сессия обрывается, новая тоже обрывается.
     * Клиент после получения этого кода ошибки попытается
     * залогиниться снова через некоторое время.
     */
    ALREADY_IN_GAME(2),
    /**
     * внутреняя ошибка сервера
     */
    INTERNAL_SERVER_ERROR(3),
    /**
     * игрок забанен
     */
    BANNED(4),
    /**
     * превышено время ожидания отпущенное на переподключение к бою
     */
    RECONNECT_TIMEOUT(5),
    /**
     *  некорректная версия протокола
     */
    INCORRECT_PROTOCOL_VERSION(6),
    /**
     *  сервер находится в состоянии за пуска
     */
    SERVER_IS_STARTING(7),
    /**
     *  на сервере включен белый список
     */
    NOT_IN_WHITE_LIST(8);

    int type;

    LoginErrorEnum(int type) {
        this.type = type;
    }

    @Override
    public int getType() {
        return type;
    }

    public static LoginErrorEnum valueOf(int type) {
        for(LoginErrorEnum loginErrorEnum : values()) {
            if(loginErrorEnum.getType() == type) {
                return loginErrorEnum;
            }
        }
        return INDEFINITE;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }
}