package com.pragmatix.arena.coliseum.messages;

import com.pragmatix.serialization.annotations.Command;

@Command(111)
public class AddTeamMember {

    public int teamMemberIndex;

    public AddTeamMember() {
    }

    public AddTeamMember(int teamMemberIndex) {
        this.teamMemberIndex = teamMemberIndex;
    }

    @Override
    public String toString() {
        return "AddTeamMember{" +
                "teamMemberIndex=" + teamMemberIndex +
                '}';
    }

}
