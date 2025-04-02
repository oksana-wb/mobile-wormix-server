package com.pragmatix.intercom.messages;

import com.pragmatix.app.common.BattleState;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.sessions.IAppServer;

/**
 * Уведомление об остановке PVP сервера
 *
 * @see com.pragmatix.app.controllers.AppIntercomController#onPvpServerStopped(PvpServerStopped, IAppServer)
 */
@Command(4007)
public class PvpServerStopped extends IntercomRequest {

    public PvpServerStopped() {
    }

    @Override
    public String toString() {
        return "PvpServerStopped{}";
    }
}
