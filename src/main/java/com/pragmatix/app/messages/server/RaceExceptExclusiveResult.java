package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.messages.client.GetRaceExceptExclusive;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.09.2015 18:04
 * @see com.pragmatix.app.controllers.ProfileController#onGetRaceExceptExclusive(GetRaceExceptExclusive, UserProfile)
 */
@Command(10135)
public class RaceExceptExclusiveResult {

    @Resize(TypeSize.UINT32)
    public long profileId;

    public Race race;

    public byte skinId;

    public RaceExceptExclusiveResult() {
    }

    public RaceExceptExclusiveResult(long profileId, byte race, byte skinId) {
        this.profileId = profileId;
        this.race = Race.valueOf(race);
        this.skinId = skinId;
    }

    @Override
    public String toString() {
        return "RaceExceptExclusiveResult{" +
                "profileId=" + profileId +
                ", race=" + race +
                ", skinId=" + skinId +
                '}';
    }
}
