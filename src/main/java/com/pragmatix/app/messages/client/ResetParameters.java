package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * Команда для сброса параметров игрока
 *
 * Created by IntelliJ IDEA.
 * User: denver
 * Date: 10.02.2010
 * Time: 0:58:45
 *
 * @see com.pragmatix.app.controllers.UserParametersController#onResetParameters(ResetParameters, com.pragmatix.app.model.UserProfile)
 */
@Command(15)
public class ResetParameters {

    /**
     * покупка за реалы
     */
    public static final int REAL_MONEY = 0;

    /**
     * покупка за игровую валюту
     */
    public static final int MONEY = 1;

    /**
     * тип денег
     */
    public int moneyType;

    public ResetParameters() {
    }
}
