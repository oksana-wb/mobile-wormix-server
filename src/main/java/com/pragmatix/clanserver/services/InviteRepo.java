package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.domain.Invite;
import com.pragmatix.clanserver.messages.ServiceResult;

/**
 * Author: Vladimir
 * Date: 26.04.13 16:22
 */
public interface InviteRepo {
    ServiceResult addInvite(Object key, int clanId, short socialId, int profileId);

    Invite[] getInvites(Object key);

    Invite[] removeInvites(Object key);

    ServiceResult addInvite(short hostSocialId, int hostProfileId, int clanId, short socialId, int profileId);

    Invite[] getInvites(short hostSocialId, int hostProfileId);

    Invite[] removeInvites(short hostSocialId, int hostProfileId);
}
