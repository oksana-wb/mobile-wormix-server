package com.pragmatix.clanserver.controllers;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.messages.ServiceResult;
import com.pragmatix.clanserver.messages.request.*;
import com.pragmatix.clanserver.messages.response.*;
import com.pragmatix.clanserver.messages.structures.ClanTO;
import com.pragmatix.clanserver.services.ClanSeasonService;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;

/**
 * Author: Vladimir
 * Date: 12.04.13 16:42
 */
@Controller
public class ClanController extends AbstractController {

    @Resource
    private ClanSeasonService clanSeasonService;

    @Value("${clan.joinClansLimit:20}")
    private int joinClansLimit = 20;

    @OnMessage
    public InviteToClanResponse inviteToClan(InviteToClanRequest request, UserProfile profile) {
        return clanService.inviteToClan(request, getClanMember(profile));
    }

    @OnMessage
    public ExpelFromClanResponse expelFromClan(ExpelFromClanRequest request, UserProfile profile) {
        return clanService.expelFromClan(request, getClanMember(profile));
    }

    @OnMessage
    public QuitClanResponse quitClan(QuitClanRequest request, UserProfile profile) {
        return clanService.quitClan(request, getClanMember(profile));
    }

    @OnMessage
    public PromoteInRankResponse promoteInRank(PromoteInRankRequest request, UserProfile profile) {
        return clanService.promoteInRank(request, getClanMember(profile));
    }

    @OnMessage
    public LowerInRankResponse lowerInRank(LowerInRankRequest request, UserProfile profile) {
        return clanService.lowerInRank(request, getClanMember(profile));
    }

    @OnMessage
    public ExpandClanResponse expandClan(ExpandClanRequest request, UserProfile profile) {
        return clanService.expandClan(request, getClanMember(profile));
    }

    @OnMessage
    public DeleteClanResponse deleteClan(DeleteClanRequest request, UserProfile profile) {
        return clanService.deleteClan(request, getClanMember(profile));
    }

    @OnMessage
    public ClanSummaryResponse getClanSummary(ClanSummaryRequest request, UserProfile profile) {
        return clanService.getClanSummary(request, getClanMember(profile));
    }

    @OnMessage
    public ListClansResponse listClans(ListClansRequest request, UserProfile profile) {
        return clanService.listClans(request);
    }

    @OnMessage
    public TopClansResponse topClans(TopClansRequest request, UserProfile profile) {
        return clanService.topClans(request, getClanMember(profile));
    }

    @OnMessage
    public RenameClanResponse renameClan(RenameClanRequest request, UserProfile profile) {
        return clanService.renameClan(request, getClanMember(profile));
    }

    @OnMessage
    public ChangeClanEmblemResponse changeClanEmblem(ChangeClanEmblemRequest request, UserProfile profile) {
        return clanService.changeClanEmblem(request, getClanMember(profile));
    }

    @OnMessage
    public ChangeClanDescriptionResponse changeClanDescriptions(ChangeClanDescriptionRequest request, UserProfile profile) {
        return clanService.changeClanDescription(request, getClanMember(profile));
    }

    @OnMessage
    public ChangeClanJoinRatingResponse changeClanJoinRating(ChangeClanJoinRatingRequest request, UserProfile profile) {
        return clanService.changeClanJoinRating(request, getClanMember(profile));
    }

    @OnMessage
    public ChangeClanClosedStateResponse changeClanClosedState(ChangeClanClosedStateRequest request, UserProfile profile) {
        return clanService.changeClanClosedState(request, getClanMember(profile));
    }

    @OnMessage
    public ChangeClanMedalPriceResponse changeClanMedalPrice(ChangeClanMedalPriceRequest request, UserProfile profile) {
        return new ChangeClanMedalPriceResponse(clanService.changeClanMedalPrice(request.medalPrice, getClanMember(profile)), request);
    }

    @OnMessage
    public CashMedalsResponse cashMedals(CashMedalsRequest request, UserProfile profile) {
        return new CashMedalsResponse(clanService.cashMedals(request.medals, getClanMember(profile)), request);
    }

    @OnMessage
    public AuditActionsResponse auditActions(AuditActionsRequest request, UserProfile profile) {
        return new AuditActionsResponse(clanService.auditActions(getClanMember(profile)), request);
    }

    @OnMessage
    public SetExpelPermitResponse setExpelPermit(SetExpelPermitRequest request, UserProfile profile) {
        return new SetExpelPermitResponse(clanService.setExpelPermit(request.profileId, request.value, getClanMember(profile)), request);
    }

    @OnMessage
    public SetMuteModeResponse setMuteMode(SetMuteModeRequest request, UserProfile profile) {
        return new SetMuteModeResponse(clanService.setMuteMode(request.profileId, request.value, getClanMember(profile)), request);
    }

    @OnMessage
    public GetJoinClansResponse onGetJoinClans(GetJoinClans request, UserProfile profile) {
        if(clanSeasonService.isDiscard()) {
            return new GetJoinClansResponse();
        } else {
            ClanTO[] clans = clanService.joinClans(profile.getRating(), joinClansLimit);
            return new GetJoinClansResponse(clans);
        }
    }

}
