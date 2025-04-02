package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.messages.request.*;
import com.pragmatix.clanserver.messages.response.*;
import com.pragmatix.clanserver.messages.structures.ClanTO;

/**
 * Author: Vladimir
 * Date: 05.04.13 13:22
 */
public interface ClanService {
    InviteToClanResponse inviteToClan(InviteToClanRequest request, ClanMember user);

    CommonResponse<LoginBase> onLogin(LoginBase login, ClanMember user);

    void onLogout(ClanMember clanMember, boolean broadcast);

    ClanMember getClanMember(short socialId, int profileId);

    ClanMember createClanMember(short socialId, int profileId, String socialProfileId, String name);

    PromoteInRankResponse promoteInRank(PromoteInRankRequest request, ClanMember user);

    LowerInRankResponse lowerInRank(LowerInRankRequest request, ClanMember user);

    ExpandClanResponse expandClan(ExpandClanRequest request, ClanMember user);

    Clan getClan(Integer clanId);

    ExpelFromClanResponse expelFromClan(ExpelFromClanRequest request, ClanMember user);

    QuitClanResponse quitClan(QuitClanRequest request, ClanMember user);

    Clan getClanByMember(short socialId, int profileId);

    UpdateRatingResponse updateRating(UpdateRatingRequest request);

    DeleteClanResponse deleteClan(DeleteClanRequest request, ClanMember user);

    ClanSummaryResponse getClanSummary(ClanSummaryRequest request, ClanMember user);

    ListClansResponse listClans(ListClansRequest request);

    RenameClanResponse renameClan(RenameClanRequest request, ClanMember user);

    ChangeClanEmblemResponse changeClanEmblem(ChangeClanEmblemRequest request, ClanMember user);

    ChangeClanDescriptionResponse changeClanDescription(ChangeClanDescriptionRequest request, ClanMember user);

    TopClansResponse topClans(TopClansRequest request, ClanMember user);

    ClanMember[] getMembers(short socialId, int[] profilesId);

    ChangeClanReviewStateResponse changeClanReviewState(ChangeClanReviewStateRequest request);

    ListClansResponse listClansOrderByName(ListClansRequest request);

    CommonResponse updateClan(ClanTO source);

    ChangeClanJoinRatingResponse changeClanJoinRating(ChangeClanJoinRatingRequest request, ClanMember user);

    ClanTO[] joinClans(int rating, int limit);

    void postToChat(short socialId, int profileId, int actionId, String text);
}
