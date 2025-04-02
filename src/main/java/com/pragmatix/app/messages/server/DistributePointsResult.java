package com.pragmatix.app.messages.server;

import com.pragmatix.serialization.annotations.Command;

/**
 * ответ от сервера на команду DistributePoints
 * @author denis
 *         Date: 10.01.2010
 *         Time: 19:38:10
 */
@Command(10015)
public class DistributePointsResult {

    /**
     * Успешный статус
     */
    public static final short SUCCESS = 0;

    /**
     * ошибка
     */
    public static final short ERROR = 1;

    /**
     * если недостаточно поинтов для распределения
     */
    public static final short NOT_ENOUGH_POINTS = 2;

    public short result;

    public DistributePointsResult() {

    }

    public DistributePointsResult(short result) {
        this.result = result;
    }
}
