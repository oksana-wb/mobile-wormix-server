package com.pragmatix.clanserver.messages;

import com.pragmatix.serialization.annotations.EnumKey;

/**
 * коды завершения
 * <p/>
 * Author: Vladimir
 * Date: 05.04.2013 09:15
 */
public enum ServiceResult {
    /**
     * успешное завершение
     */
    OK(0),

    /**
     * неверная версия протокола
     */
    ERR_INCORRECT_PROTOCOL(1),

    /**
     * сессия уже существует.
     * Сессия обрывается, новая тоже обрывается.
     * Клиент после получения этого кода ошибки попытается
     * залогиниться снова через некоторое время.
     */
    ERR_ALREADY_IN_GAME(2),

    /**
     * попытка взлома
     */
    ERR_CHEAT(11),

    /**
     * забанен
     */
    ERR_BAN(12),

    /**
     * ошибка выполнения
     */
    ERR_RUNTIME(14),

    /**
     * неопределенная ошибка
     */
    ERR_INDEFINITE(15),

    /**
     * ошибка блокировки
     */
    ERR_DEADLOCK(16),

    /**
     * ведутся профилактические работы
     */
    PROFILACTIC_WORK(17),

    /**
     * не указан идентификатор игрока в соц. сети
     */
    ERR_EMPTY_SOCIAL_ID(18),

    /**
     * доступ запрещен
     */
    ERR_ACCESS_DENIED(102),

    ERR_INVALID_CLAN_NAME(103),

    /**
     * ошибочный параметр запроса
     */
    ERR_INVALID_ARGUMENT(104),

    /**
     * недостаточно денег на счете
     */
    ERR_NOT_ENOUGH_MONEY(105),

    /**
     * не удовлетворены обязательные требования
     */
    ERR_REQUIREMENTS_FAILURE(106),

    /**
     * объект не найден
     */
    ERR_NOT_FOUND(109),

    ERR_INTEROP(110),

    ERR_INVALID_STATE(111),

    ERR_NOT_ENOUGH_TREAS(112),

    ERR_NOT_ENOUGH_RATING(114),

    ERR_NOT_ENOUGH_LEVEL(115),

    ERR_ALREADY_IN_CLAN(201),

    ERR_NOT_IN_CLAN(202),

    ERR_CLAN_SIZE_LIMIT(203),

    ERR_MEMBER_INVITE_LIMIT(204),

    ERR_PROFILE_INVITE_LIMIT(205),

    ERR_REPEATED_INVITE(206),

    ERR_CLAN_NAME_EXISTS(207),

    ERR_CLAN_LOCKED(208),

    ;

    public final int code;

    ServiceResult(int code) {
        this.code = code;
    }

    @EnumKey
    public int getCode() {
        return code;
    }

    public static ServiceResult valueOf(int code) {
        for(ServiceResult serviceResult : ServiceResult.values()) {
            if(serviceResult.getCode() == code) {
                return serviceResult;
            }
        }
        return null;
    }

    public static String print(int code) {
        ServiceResult serviceResult = valueOf(code);
        return String.format("%s(%s)", serviceResult == null ? "INDEFINITE" : serviceResult.name(), code);
    }

    public boolean isOk() {
        return this.equals(OK);
    }

}
