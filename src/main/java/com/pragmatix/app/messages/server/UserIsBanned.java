package com.pragmatix.app.messages.server;

import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Date;

/**
 * Author: Oksana Shevchenko
 * Date: 21.12.2010
 * Time: 12:07:53
 */
@Command(10023)
public class UserIsBanned {

    public int reason;

    @Resize(TypeSize.UINT32)
    public Long endDate;

    /**
     * id пользователя
     */
    @Resize(TypeSize.UINT32)
    public long profileId;

    public UserIsBanned() {
    }

    public UserIsBanned(int reason, Long endDate, Long profileId) {
        this.reason = reason;
        this.profileId = profileId;
        this.endDate = (endDate == null ? 0 : endDate / 1000);
    }

    @Override
    public String toString() {
        return "UserIsBanned{" +
                "profileId=" + profileId +
                ", reason=" + reason +
                ", endDate=" + new Date(endDate) +
                '}';
    }
}
