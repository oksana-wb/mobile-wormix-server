package com.pragmatix.app.messages.client;

import com.pragmatix.Commands;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Arrays;

/**
 * Команда на прокачку реакциии другу
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.04.11 10:39
 * @see com.pragmatix.app.controllers.PumpReactionRateController#onPumpReactionRates(PumpReactionRates, com.pragmatix.app.model.UserProfile)
 */
@Command(Commands.PumpReactionRates)
public class PumpReactionRates extends SecuredCommand {
    /**
     * id друзей которым прокачиваем скорость реакции
     */
    @Resize(TypeSize.UINT32)
    public long[] friendIds;

    public String sessionKey;

    public PumpReactionRates() {
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "PumpReactionRates{" +
                "friendIds=" + Arrays.toString(friendIds) +
                ", sessionKey='" + sessionKey + '\'' +
                ", secureResult=" + secureResult +
                '}';
    }

}
