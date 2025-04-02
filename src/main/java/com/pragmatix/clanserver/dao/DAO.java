package com.pragmatix.clanserver.dao;

import com.pragmatix.clanserver.domain.*;
import com.pragmatix.clanserver.messages.structures.ClanAuditActionTO;

import java.util.List;

/**
 * Author: Vladimir
 * Date: 04.04.13 15:26
 */
public interface DAO {
    Clan createClan(Clan clan, ClanMember leader);

    Clan getClan(Integer id);

    ClanMember createClanMember(ClanMember clanMember);

    ClanMember updateClanMember(ClanMember clanMember);

    void deleteClanMember(short socialId, int profileId);

    void deleteClan(Integer clanId);

    void expandClan(Integer clanId, int level);

    Clan getClanByMember(short socialId, int profileId);

    List<RatingItem> getTopRatings(int limit);

    boolean clanExists(String name, int clanId);

    List<Clan> listClansByName(String searchPhrase, int limit);

    void updateClanName(Integer id, String name, ReviewState reviewState);

    void updateClanAggregates(Clan clan);

    void runInTransaction(Runnable task);

    void updateClanEmblem(Integer id, byte[] emblem);

    void updateClanDescription(Integer id, String description, ReviewState reviewState);

    List<Clan> getClans(int[] clansId);

    List<Clan> getClans(short socialId, int[] profilesId);

    void updateClanReviewState(Integer id, ReviewState reviewState);

    void updateClanClosedState(Integer id, boolean closed);

    void updateClanMedalPrice(Integer id, byte medalPrice);

    List<Clan> listClansOrderByName(String searchPhrase, ReviewState[] reviewStates, int offset, int limit);

    void updateClan(Clan clan);

    void updateClanJoinRating(Integer id, int joinRating);

    Season selectCurrentOpenSeason();

    int insertNewSeason(Season season);

    int backupMember(int clanId, ClanMember member);

    void logClanAction(int clanId, int action, int publisherId, int memberId, int param, int treas);

    List<ClanAuditActionTO> selectClanActions(int clanId);

}
