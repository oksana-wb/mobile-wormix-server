package com.pragmatix.app.messages.client;

import com.pragmatix.Commands;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Команда на прокачку реакциии другу
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.04.11 10:39
 * @see com.pragmatix.app.controllers.PumpReactionRateController#onPumpReactionRate(PumpReactionRate, com.pragmatix.app.model.UserProfile)
 */
@Command(Commands.PumpReactionRate)
public class PumpReactionRate extends SecuredCommand {
    /**
     * id друга которому прокачиваем скорость реакции
     */
    @Resize(TypeSize.UINT32)
    public long friendId;

    public String sessionKey;

    public PumpReactionRate() {
    }

    public PumpReactionRate(long friendId) {
        this.friendId = friendId;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "PumpReactionRate{" +
                "friendId=" + friendId +
                ", sessionKey='" + sessionKey + '\'' +
                ", secureResult=" + secureResult +
                '}';
    }


}
