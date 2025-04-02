package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * Разблокировать миссию. Цена всегда в реалах.
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 11.03.12 15:59
 *
 * @see com.pragmatix.app.controllers.ShopController#onBuyUnlockMission(BuyUnlockMission, com.pragmatix.app.model.UserProfile)
 *
 * @see com.pragmatix.app.messages.server.BuyUnlockMissionResult
 */
@Command(58)
public class BuyUnlockMission {

    /**
     * id разблокируемой миссии. Стоимость операции будет тем больше чем больше missionId от currentMissionId игрока
     */
    public short missionId;


    @Override
    public String toString() {
        return "BuyUnlockMission{" +
                "missionId=" + missionId +
                '}';
    }

}
