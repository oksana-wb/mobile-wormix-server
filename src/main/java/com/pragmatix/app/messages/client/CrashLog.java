package com.pragmatix.app.messages.client;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.06.2016 11:16
 * @see com.pragmatix.app.filters.AuthFilter#onCrashLog(CrashLog, UserProfile)
 */
@Command(138)
public class CrashLog {

    public String platform;

    public String userId;

    public String log;

    @Override
    public String toString() {
        return "CrashLog{" +
                ", platform=" + platform + "" +
                ", userId=" + userId + "" +
                ", log(" + log.length() + ")" +
                '}';
    }

}
