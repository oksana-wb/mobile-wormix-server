package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.app.controllers.GroupController#onToggleTeamMember(com.pragmatix.app.messages.client.ToggleTeamMember, com.pragmatix.app.model.UserProfile)
 */
@Command(10106)
public class ToggleTeamMemberResult {

    public SimpleResultEnum result;

    public int teamMemberId;

    public ToggleTeamMemberResult() {
    }

    public ToggleTeamMemberResult(SimpleResultEnum result, int teamMemberId) {
        this.teamMemberId = teamMemberId;
        this.result = result;
    }

    @Override
    public String toString() {
        return "ToggleTeamMemberResult{" +
                "teamMemberId=" + teamMemberId +
                ", result=" + result +
                '}';
    }

}
