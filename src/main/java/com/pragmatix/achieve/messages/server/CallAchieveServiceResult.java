package com.pragmatix.achieve.messages.server;

import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 10.11.11 14:31
 */
@Command(13008)
public class CallAchieveServiceResult {

    public String profileId;

    public boolean result;

    public CallAchieveServiceResult() {
    }

    public CallAchieveServiceResult(String profileId, boolean result) {
        this.profileId = profileId;
        this.result = result;
    }

    @Override
    public String toString() {
        return "CallAchieveServiceResult{" +
                "profileId='" + profileId + '\'' +
                ", result=" + result +
                '}';
    }
}
