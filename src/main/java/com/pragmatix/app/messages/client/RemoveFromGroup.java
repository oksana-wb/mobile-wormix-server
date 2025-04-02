package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * Команда для удаления червя из группы
 *
 * @author denis
 *         Date: 03.01.2010
 *         Time: 22:23:05
 * @see com.pragmatix.app.controllers.GroupController#onRemoveFromGroup(RemoveFromGroup, com.pragmatix.app.model.UserProfile)
 */
@Command(13)
public class RemoveFromGroup {

    /**
     * id рофайла червя которого хотим удалить из группы
     */
    public int teamMemberId;

    public RemoveFromGroup(int teamMemberId) {
        this.teamMemberId = teamMemberId;
    }

    public RemoveFromGroup() {
    }

    @Override
    public String toString() {
        return "RemoveFromGroup{" +
                "teamMemberId=" + teamMemberId +
                '}';
    }
}
