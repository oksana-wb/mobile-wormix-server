package com.pragmatix.app.messages.client;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Команда покупки расы
 *
 * @see com.pragmatix.app.controllers.ProfileController#onSelectRace(SelectRace, UserProfile)
 * @see com.pragmatix.app.messages.server.SelectRaceResult
 */
@Command(44)
public class SelectRace {

    public Race race;

    public byte skinId;

    public SelectRace() {
    }

    public SelectRace(Race race, byte skinId) {
        this.race = race;
        this.skinId = skinId;
    }

    @Override
    public String toString() {
        return "SelectRace{" +
                "race=" + race +
                ", skinId=" + skinId +
                '}';
    }
}
