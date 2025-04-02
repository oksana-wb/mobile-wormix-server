package com.pragmatix.craft.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 07.07.12 14:24
 *
 * @see com.pragmatix.craft.cotrollers.CraftController#onGetReagentsForProfile(GetReagentsForProfile, UserProfile)
 */
@Command(85)
public class GetReagentsForProfile {

    @Resize(TypeSize.UINT32)
    public long profileId;

    public GetReagentsForProfile() {
    }

    public GetReagentsForProfile(long profileId) {
        this.profileId = profileId;
    }

    @Override
    public String toString() {
        return "GetReagentsForProfile{}";
    }
}
