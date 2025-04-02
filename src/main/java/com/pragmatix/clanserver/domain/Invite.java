package com.pragmatix.clanserver.domain;

import com.pragmatix.clanserver.utils.Utils;

import java.util.Date;

/**
 * Author: Vladimir
 * Date: 09.04.13 10:14
 *
 * Используется для учета выданных или полученных приглашений
 */
public class Invite {
    public static final Invite[] EMPTY_ARR = new Invite[0];

    /**
     * Идентификатор клана
     */
    public int clanId;

    /**
     * Идентификатор соцсети приглашенной (пригласившей) стороны
     */
    public short socialId;

    /**
     * Идентификатор профиля приглашенной (пригласившей) стороны
     */
    public int profileId;

    /**
     * Дата приглашения
     */
    public Date inviteDate;

    public Invite(int clanId, short socialId, int profileId) {
        this.clanId = clanId;
        this.socialId = socialId;
        this.profileId = profileId;

        inviteDate = new Date();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Invite");
        sb.append("{clanId=").append(clanId);
        sb.append(", socialId=").append(socialId);
        sb.append(", profileId=").append(profileId);
        sb.append(", inviteDate=").append(Utils.LOG_DATE_FORMAT.format(inviteDate));
        sb.append('}');
        return sb.toString();
    }
}
