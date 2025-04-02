package com.pragmatix.app.messages.client;

import com.pragmatix.app.common.TeamMemberType;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Serialize;

/**
 * Команда от клиента к серверу на добавление червя в группу
 *
 * @author denis
 *         Date: 03.01.2010
 *         Time: 22:22:46
 * @see com.pragmatix.app.controllers.GroupController#onAddToGroup(AddToGroup, com.pragmatix.app.model.UserProfile)
 */
@Command(12)
public class AddToGroup {

    /**
     * id рофайла червя которого хотим добавить в группу
     */
    public int teamMemberId;

    /**
     * тип денег
     */
    public MoneyType moneyType;

    /**
     * тип члена команды
     */
    public TeamMemberType teamMemberType;

    /**
     * id члена команды, которого замещаем
     * если не равен NO_PREV_TEAM_MEMBER , то это команда замены члена команды
     */
    public int prevTeamMemberId = NO_PREV_TEAM_MEMBER;
    public static final int NO_PREV_TEAM_MEMBER = 0;

    /**
     * добавлять нового члена команды включенным или выключенным (на скамейку запасных)
     */
    public boolean active = true;

    public AddToGroup() {
    }

    public AddToGroup(int teamMemberId, MoneyType moneyType, TeamMemberType teamMemberType) {
        this.teamMemberId = teamMemberId;
        this.moneyType = moneyType;
        this.teamMemberType = teamMemberType;
    }

    public AddToGroup(int teamMemberId, MoneyType moneyType, TeamMemberType teamMemberType, int prevTeamMemberId, boolean active) {
        this.teamMemberId = teamMemberId;
        this.moneyType = moneyType;
        this.teamMemberType = teamMemberType;
        this.prevTeamMemberId = prevTeamMemberId;
        this.active = active;
    }

    @Override
    public String toString() {
        return "AddToGroup{" +
                "teamMemberId=" + teamMemberId +
                ", moneyType=" + moneyType +
                ", teamMemberType=" + teamMemberType +
                ", prevTeamMemberId=" + prevTeamMemberId +
                ", active=" + active +
                '}';
    }

}
