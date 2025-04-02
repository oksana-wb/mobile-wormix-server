package com.pragmatix.arena.mercenaries.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.02.2016 13:59
 *
 * @see GetMercenariesDefs
 */
@Command(10124)
public class MercenariesDefsResponse {

    public MercenariesTeamMember[] mercenariesDefs;

    public MercenariesDefsResponse() {
    }

    public MercenariesDefsResponse(MercenariesTeamMember[] mercenariesDefs) {
        this.mercenariesDefs = mercenariesDefs;
    }

    @Override
    public String toString() {
        return "MercenariesDefsResponse{" +
                "mercenariesDefs=" + Arrays.toString(mercenariesDefs) +
                '}';
    }
}
