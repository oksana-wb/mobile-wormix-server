package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * Команда говорит серверу о начале битвы
 * 
 * User: denis
 * Date: 05.12.2009
 * Time: 2:32:24
 *
 * @see com.pragmatix.app.controllers.BattleController#onStartBattle(StartBattle, com.pragmatix.app.model.UserProfile)
 */
@Command(6)
public class StartBattle {
    /**
     *  Id миссии которую будет проходить Игрок
     *  или 0 - если это бой с ботом
     */
    public short missionId;
    
    public StartBattle() {
    }

    public StartBattle(int missionId) {
        this.missionId = (short) missionId;
    }

    @Override
    public String toString() {
        return "StartBattle{" +
                "missionId=" + missionId +
                '}';
    }
}
