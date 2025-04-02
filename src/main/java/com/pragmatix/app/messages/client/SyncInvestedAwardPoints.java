package com.pragmatix.app.messages.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.annotations.Command;

/**
 * Команда скоррктирует вложенные призовые очки достижений на основе призового оружия в рюкзаке
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 31.01.12 14:23
 * @see com.pragmatix.app.controllers.UserProfileController#onSyncInvestedAwardPoints(SyncInvestedAwardPoints, com.pragmatix.app.model.UserProfile)
 * @see com.pragmatix.app.messages.server.SyncInvestedAwardPointsResult
 */
@Command(57)
public class SyncInvestedAwardPoints extends SecuredCommand {

    /**
     * id сессии, для повышения безопастности
     */
    public String sessionKey;

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "SyncInvestedAwardPoints{" +
                "sessionKey='" + sessionKey + '\'' +
                ", secureResult=" + secureResult +
                '}';
    }
}
