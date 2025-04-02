package com.pragmatix.clanserver.controllers;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clan.ClanInteropServiceImpl;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.messages.ServiceResult;
import com.pragmatix.clanserver.messages.request.LoginCreateRequest;
import com.pragmatix.clanserver.messages.request.LoginJoinRequest;
import com.pragmatix.clanserver.messages.request.LoginRequest;
import com.pragmatix.clanserver.messages.response.LoginErrorResponse;
import com.pragmatix.clanserver.services.ClanSeasonService;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.messages.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Author: Vladimir
 * Date: 08.04.13 10:12
 */
@Controller
public class ClanAuthController extends AbstractController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ClanInteropServiceImpl interopService;

    @Autowired
    ClanSeasonService clanSeasonService;

    @OnMessage
    public Object onLoginRequest(LoginRequest loginRequest, UserProfile profile) {
        if(clanSeasonService.isDiscard()) {
            return new LoginErrorResponse(ServiceResult.PROFILACTIC_WORK, loginRequest, "Ведутся профилактические работы");
        }
        loginRequest.name = profile.getName();
        ClanMember user = getClanMember(profile);
        if(user == null || user.isNew()) {
            if(profile.getClanId() != 0) {
                if(user == null) {
                    user = new ClanMember();
                    user.setNew(true);
                    user.profileId = profile.getId().intValue();
                }

                interopService.refreshClan(user);
            }
            return new LoginErrorResponse(ServiceResult.ERR_NOT_IN_CLAN, loginRequest, "Пользователь не существует");
        } else {
            if(!user.name.equals(loginRequest.name)) {
                user.name = loginRequest.name;
                user.setDirty(true);
            }
        }
        user.rating = profile.getRating();
        return clanService.onLogin(loginRequest, user);
    }

    @OnMessage
    public Object onLoginCreateRequest(LoginCreateRequest loginRequest, UserProfile profile) {
        if(clanSeasonService.isDiscard()) {
            return new LoginErrorResponse(ServiceResult.PROFILACTIC_WORK, loginRequest, "Ведутся профилактические работы");
        }
        loginRequest.name = profile.getName();
        ClanMember user = getClanMember(profile);
        if(user == null) {
            user = clanService.createClanMember((short)0, profile.getId().intValue(), "", loginRequest.name);
        } else if(!user.isNew()) {
            return new LoginErrorResponse(ServiceResult.ERR_ALREADY_IN_CLAN, loginRequest, "Пользователь уже в клане #" + user.getClanId());
        }
        user.rating = profile.getRating();
        return clanService.onLogin(loginRequest, user);
    }

    @OnMessage
    public Object onLoginJoinRequest(LoginJoinRequest loginRequest, UserProfile profile) {
        if(clanSeasonService.isDiscard()) {
            Messages.toUser(new LoginErrorResponse(ServiceResult.PROFILACTIC_WORK, loginRequest, "Ведутся профилактические работы"));
            return null;
        }
        loginRequest.name = profile.getName();
        ClanMember user = getClanMember(profile);
        if(user == null) {
            user = clanService.createClanMember((short)0, profile.getId().intValue(), "", loginRequest.name);
        } else if(!user.isNew()) {
            return new LoginErrorResponse(ServiceResult.ERR_ALREADY_IN_CLAN, loginRequest, "Пользователь уже в клане #" + user.getClanId());
        }
        user.rating = profile.getRating();
        return clanService.onLogin(loginRequest, user);
    }

}
