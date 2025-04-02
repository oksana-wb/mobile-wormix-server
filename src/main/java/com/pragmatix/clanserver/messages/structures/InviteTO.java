package com.pragmatix.clanserver.messages.structures;

import com.pragmatix.clanserver.domain.Invite;
import com.pragmatix.clanserver.utils.Utils;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Structure;

/**
 * Author: Vladimir
 * Date: 25.04.13 12:02
 */
@Structure
public class InviteTO {
    public static final InviteTO[] EMPTY_ARR = new InviteTO[0];

    /**
     * Идентификатор клана
     */
    public int clanId;

    /**
     * Идентификатор соцсети приглашенной (пригласившей) стороны
     */
    @Ignore
    public short socialId;

    /**
     * Идентификатор профиля приглашенной (пригласившей) стороны
     */
    public int profileId;

    /**
     * Дата приглашения
     */
    public int inviteDate;

    public InviteTO() {
    }

    public InviteTO(Invite invite) {
        clanId = invite.clanId;
        socialId = invite.socialId;
        profileId = invite.profileId;
        inviteDate = Utils.toSeconds(invite.inviteDate);
    }

    public static InviteTO[] convert(Invite[] invites) {
        if (invites.length == 0) {
            return EMPTY_ARR;
        }

        InviteTO[] res = new InviteTO[invites.length];
        int i = 0;
        for (Invite invite: invites) {
            res[i++] = new InviteTO(invite);
        }

        return res;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Invite");
        sb.append("{clanId=").append(clanId);
        sb.append(", profileId=").append(profileId);
        sb.append(", inviteDate=").append(Utils.LOG_DATE_FORMAT.format(Utils.toDate(inviteDate)));
        sb.append('}');
        return sb.toString();
    }
}
