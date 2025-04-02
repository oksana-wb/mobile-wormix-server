package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 14.04.2016 12:54
 *         <p>
 * Команда сброса имени к социальному для игрока (либо его перса).
 * Отменяет действие {@link BuyRename} бесплатно.
 *
 * @see com.pragmatix.app.controllers.ProfileController#onClearName(ClearName, com.pragmatix.app.model.UserProfile)
 * @see com.pragmatix.app.messages.server.ClearNameResult
 */
@Command(131)
public class ClearName {

    public int teamMemberId;

    public ClearName() {
    }

    public ClearName(int teamMemberId) {
        this.teamMemberId = teamMemberId;
    }

    @Override
    public String toString() {
        return "ClearName{" +
                "teamMemberId=" + teamMemberId +
                '}';
    }
}
