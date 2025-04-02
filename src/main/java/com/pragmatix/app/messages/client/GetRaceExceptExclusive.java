package com.pragmatix.app.messages.client;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 13.09.2016 11:14
 *  @see com.pragmatix.app.controllers.ProfileController#onGetRaceExceptExclusive(GetRaceExceptExclusive, UserProfile)
 *
 */
@Command(135)
public class GetRaceExceptExclusive {

    @Resize(TypeSize.UINT32)
    public long profileId;

    public GetRaceExceptExclusive() {
    }

    public GetRaceExceptExclusive(long profileId) {
        this.profileId = profileId;
    }

    @Override
    public String toString() {
        return "GetRaceExceptExclusive{" +
                "profileId=" + profileId +
                '}';
    }

}
