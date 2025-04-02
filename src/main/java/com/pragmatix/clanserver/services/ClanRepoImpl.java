package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.dao.DAO;
import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.gameapp.cache.SoftCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: Vladimir
 * Date: 05.04.13 11:55
 */
@Service
public class ClanRepoImpl implements ClanRepo {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    DAO dao;

    @Autowired
    ClanSeasonService clanSeasonService;

    @Autowired
    SoftCache softCache;

    final ConcurrentHashMap<Object, Integer> memberCache = new ConcurrentHashMap<>();

    final Integer nullClanId = 0;

    @Resource
    private ClanDailyRegistry clanDailyRegistry;

    public Clan getClanFromCache(Integer id) {
        return softCache.get(Clan.class, id, false);
    }

    @Override
    public Clan getClan(Integer id) {
        Clan clan = getClanFromCache(id);

        if (clan == null) {
            clan = getDao().getClan(id);

            if (clan != null) {
                clan = putClan(clan);
            }
        }

        return clan;
    }

    protected DAO getDao() {
        if(clanSeasonService.isDiscardDAO()) {
           throw new IllegalStateException("сезон в процессе закрытия!");
        } else {
            return dao;
        }
    }

    @Override
    public Clan getClanByMember(short socialId, int profileId) {
        Long memberId = ClanMember.getId(socialId, profileId);

        Integer clanId = memberCache.get(memberId);

        Clan clan = null;

        if (clanId == null) {
            clan = getDao().getClanByMember(socialId, profileId);

            if (clan != null) {
                clan = putClan(clan);
            } else {
                memberCache.put(memberId, nullClanId);
            }
        } else if (clanId > nullClanId) {
            clan = getClan(clanId);
        }

        return clan;
    }

    @Override
    public Clan putClan(Clan clan) {
        Clan c = softCache.putIfAbsent(Clan.class, clan.id, clan);
        if (clan == c) {
            for (ClanMember member: clan.members()) {
                memberCache.put(member.getId(), clan.id);
            }
            return clan;
        } else {
            return c;
        }
    }

    @Override
    public ClanMember getMember(short socialId, int profileId) {
        Clan clan = getClanByMember(socialId, profileId);

        return clan != null ? clan.getMember(socialId, profileId) : null;
    }

    @Override
    public ClanMember[] getMembers(short socialId, int[] profilesId) {
        Long[] membersId = new Long[profilesId.length];

        for (int i = 0; i < profilesId.length; i++) {
            membersId[i] = ClanMember.getId(socialId, profilesId[i]);
        }

        int[] missingClansId = new int[profilesId.length];
        int missingClansOffset = 0;

        for (Long memberId: membersId) {
            Integer clanId = memberCache.get(memberId);

            if (clanId != null && !clanId.equals(nullClanId) && getClanFromCache(clanId) == null) {
                missingClansId[missingClansOffset++] = clanId;
            }
        }

        if (missingClansOffset > 0) {
            List<Clan> clans = getDao().getClans(Arrays.copyOf(missingClansId, missingClansOffset));

            for (Clan clan: clans) {
                putClan(clan);
            }
        }

        int[] missingMembersId = new int[profilesId.length];
        int missingMembersOffset = 0;

        for (int i = 0; i < profilesId.length; i++) {
            Integer clanId = memberCache.get(membersId[i]);

            if (clanId == null) {
                missingMembersId[missingMembersOffset++] = profilesId[i];
            }
        }

        if (missingMembersOffset > 0) {
            List<Clan> clans = getDao().getClans(socialId, Arrays.copyOf(missingMembersId, missingMembersOffset));
            for (Clan clan: clans) {
                putClan(clan);
            }
        }

        ClanMember[] res = new ClanMember[profilesId.length];

        for (int i = 0; i < res.length; i++) {
            Long memberId = membersId[i];
            Integer clanId = memberCache.get(memberId);
            if (clanId != null && !clanId.equals(nullClanId)) {
                Clan clan = getClanFromCache(clanId);
                if (clan != null) {
                    res[i] = clan.getMember(memberId);
                }
            }
        }

        return res;
    }

    @Override
    public Clan[] loadClans(int[] clansId) {
        int[] missingClansId = new int[clansId.length];
        int missingClansOffset = 0;

        Clan[] res = new Clan[clansId.length];

        int i = 0;

        for (int clanId: clansId) {
            Clan clan = getClanFromCache(clanId);

            if (clan != null) {
                res[i++] = clan;
            } else {
                missingClansId[missingClansOffset++] = clanId;
            }
        }

        if (missingClansOffset > 0) {
            List<Clan> clans = getDao().getClans(Arrays.copyOf(missingClansId, missingClansOffset));

            for (Clan clan: clans) {
                res[i++] = putClan(clan);
            }
        }

        if (i < res.length) {
            res = Arrays.copyOf(res, i);
        }

        return res;
    }

    @Override
    public void putMember(ClanMember member) {
        memberCache.put(member.getId(), member.clan != null ? member.getClanId() : nullClanId);
    }

    @Override
    public void removeClan(Clan clan) {
        getDao().deleteClan(clan.id);
        softCache.remove(Clan.class, clan.id);
        for (ClanMember member: clan.members()) {
            memberCache.put(member, nullClanId);
//            clanDailyRegistry.clearFor(member.getClanMemberId());
            member.clan = null;
        }
    }

    @Override
    public void onMemberRemove(ClanMember member) {
        member.clan = null;

        memberCache.put(member.getId(), nullClanId);

//        clanDailyRegistry.clearFor(member.getClanMemberId());
    }

    public ConcurrentHashMap<Object, Integer> getMemberCache() {
        return memberCache;
    }

}
