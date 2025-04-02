package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.app.controllers.GroupController#onToggleTeamMember(ToggleTeamMember, com.pragmatix.app.model.UserProfile)
 */
@Command(106)
public class ToggleTeamMember {
    /**
     * id рофайла червя которого хотим включить/выключить
     */
    public int teamMemberId;

    public boolean active;

    public ToggleTeamMember() {
    }

    public ToggleTeamMember(int teamMemberId, boolean active) {
        this.teamMemberId = teamMemberId;
        this.active = active;
    }

    @Override
    public String toString() {
        return "ToggleTeamMember{" +
                "teamMemberId=" + teamMemberId +
                ", active=" + active +
                '}';
    }

}
