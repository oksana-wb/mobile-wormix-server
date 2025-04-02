package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;

/**
 * Author: Vladimir
 * Date: 05.04.13 11:33
 */
public interface ClanRepo {
    Clan getClan(Integer id);

    Clan putClan(Clan clan);

    Clan getClanByMember(short socialId, int profileId);

    void putMember(ClanMember member);

    void onMemberRemove(ClanMember member);

    ClanMember getMember(short socialId, int profileId);

    void removeClan(Clan clan);

    ClanMember[] getMembers(short socialId, int[] profilesId);

    Clan[] loadClans(int[] clansId);
}
