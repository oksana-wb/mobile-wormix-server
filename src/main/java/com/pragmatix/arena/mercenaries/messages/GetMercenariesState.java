package com.pragmatix.arena.mercenaries.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.arena.mercenaries.MercenariesController#onGetMercenariesState(GetMercenariesState, UserProfile)
 */
@Command(126)
public class GetMercenariesState {

    @Override
    public String toString() {
        return "GetMercenariesState{}";
    }
}
