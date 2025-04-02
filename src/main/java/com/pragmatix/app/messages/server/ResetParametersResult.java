package com.pragmatix.app.messages.server;

import com.pragmatix.serialization.annotations.Command;

/**
 * Результат сброса параметров игрока
 * 
 * Created by IntelliJ IDEA.
 * User: denver
 * Date: 10.02.2010
 * Time: 1:03:02
 */
@Command(10016)
public class ResetParametersResult {

    /**
     * Успешный статус
     */
    public static final short SUCCESS = 0;

    /**
     * ошибка
     */
    public static final short ERROR = 1;

     /**
     * если недостаточно средств для совершения покупки
     */
    public static final short NOT_ENOUGH_MONEY = 3;

    public short result;

    public ResetParametersResult() {
    }

    public ResetParametersResult(short result) {
        this.result = result;
    }
}
