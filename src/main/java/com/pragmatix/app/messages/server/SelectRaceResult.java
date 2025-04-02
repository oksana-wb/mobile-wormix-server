package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.messages.client.SelectRace;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.09.2015 18:04
 * @see com.pragmatix.app.controllers.ProfileController#onSelectRace(SelectRace, UserProfile)
 */
@Command(10044)
public class SelectRaceResult {

    public SimpleResultEnum result;

    public Race race;

    public byte skinId;

    public SelectRaceResult() {
    }

    public SelectRaceResult(SimpleResultEnum result, Race race, byte skinId) {
        this.result = result;
        this.race = race;
        this.skinId = skinId;
    }

    @Override
    public String toString() {
        return "SelectRaceResult{" +
                "result=" + result +
                ", race=" + race +
                ", skinId=" + skinId +
                '}';
    }
}
