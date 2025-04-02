package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * команда отпровляеться клиентом на сервер для распределения поинтов
 * 
 * @author denis
 *         Date: 10.01.2010
 *         Time: 19:30:35
 *
 * @see com.pragmatix.app.controllers.UserParametersController#onDistributePoints(DistributePoints, com.pragmatix.app.model.UserProfile)
 */
@Command(14)
public class DistributePoints {

    /**
     * на сколько увеличить броню червя
     */
    public int armor;

    /**
     * на сколько увеличить атаку червя
     */
    public int attack;

    public DistributePoints() {
    }
}
