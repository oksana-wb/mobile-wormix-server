package com.pragmatix.arena.mercenaries.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * @see com.pragmatix.arena.mercenaries.MercenariesController#onSetTeamMember(SetMercenariesTeamMembers, UserProfile)
 */
@Command(127)
public class SetMercenariesTeamMembers {

    public byte[] teams;

    public SetMercenariesTeamMembers() {
    }

    public SetMercenariesTeamMembers(byte[] teams) {
        this.teams = teams;
    }

    @Override
    public String toString() {
        return "SetMercenariesTeamMembers{" +
                "teams=" + Arrays.toString(teams) +
                '}';
    }

}
