package com.pragmatix.app.controllers;

import com.pragmatix.app.common.ItemType;
import com.pragmatix.app.common.Race;
import com.pragmatix.app.domain.CookiesEntity;
import com.pragmatix.app.messages.client.*;
import com.pragmatix.app.messages.server.*;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import io.vavr.Tuple4;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ProfileController {

    @Resource
    private ProfileService profileService;

    @Resource
    private SkinService skinService;

    @Resource
    private FriendsListService friendsListService;

    @Resource
    private WeaponService weaponService;

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private CookiesService cookiesService;

    @Resource
    private PurchaseService purchaseService;

    @OnMessage
    public ProfilesResult onGetProfiles(GetProfiles msg, UserProfile profile) {
        Set<Long> ids = new HashSet<>();
        int i = 0;
        for(String socialProfileId : msg.ids) {
            Long profileId = profileService.getProfileLongId(socialProfileId);
            if(profileId != null) {
                ids.add(profileId);
                i++;
            }
            if(i > GetProfiles.MAX_PROFILES) {
                break;
            }
        }
        Collection<UserProfile> userProfiles = profileService.loadProfiles(ids, false);
        return new ProfilesResult(profileService.getUserProfileStructures(userProfiles));
    }

    @OnMessage
    public ConnectResult onConnect(Connect msg, UserProfile profile) {
        if(msg.socialNetId.isMobileOS() && msg.socialNetId != SocialServiceEnum.steam) {
            profileService.assignStingIdToProfile(profile, msg.socialProfileId, msg.socialNetId);
            profile.setProfileStringId(profileService.getProfileStringId(profile.getId()));
        }

        List<Long> ids = msg.ids.stream()
                .map(stringId -> profileService.getProfileLongId(stringId, msg.socialNetId, false))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        FriendsListService.FirstPageBean firstPageBean = friendsListService.getFirstPage(profile, ids);

        List<UserProfileStructure> friendsProfileStructures = profileService.getUserProfileStructures(firstPageBean.getFirstPage());
        friendsProfileStructures.forEach(friendsProfileStructure ->
                friendsProfileStructure.profileStringId = profileService.getProfileStringId(friendsProfileStructure.id)
        );
        return new ConnectResult(friendsProfileStructures, firstPageBean);
    }

    @OnMessage
    public void onDisconnect(Disconnect msg, UserProfile profile) {
        if(msg.socialNetId.isMobileOS()) {
            return;
        }
        profileService.dissociateStingIdFromProfile(profile, msg.socialNetId);
    }

    @OnMessage
    public SetNameResult onSetName(SetName msg, UserProfile profile) {
        SimpleResultEnum result = SimpleResultEnum.ERROR;

        if(msg.name == null) msg.name = "";
        else msg.name = msg.name.trim();
        if(msg.name.isEmpty() || msg.name.equals(profile.getName())) {
            return new SetNameResult(SimpleResultEnum.ERROR, profile.getRenameAct() + profile.getRenameVipAct(), profile.getName());
        }
        if(profile.getRenameVipAct() > 0) {
            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.EXTRA, profile,
                    Param.eventType, "renameByVip",
                    "renameVipAct", -1,
                    "oldName", profile.getName(),
                    "newName", msg.name,
                    "profile#renameVipAct", (profile.getRenameVipAct() - 1)
            );
            profileService.setName(profile, msg.name);
            profile.setRenameVipAct(profile.getRenameVipAct() - 1);
            result = SimpleResultEnum.SUCCESS;
        } else if(profile.getRenameAct() > 0) {
            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PURCHASE, profile,
                    Param.eventType, ItemType.RENAME,
                    "renameAct", -1,
                    "oldName", profile.getName(),
                    "newName", msg.name,
                    "profile#renameAct", (profile.getRenameAct() - 1)
            );
            profileService.setName(profile, msg.name);
            profile.setRenameAct(profile.getRenameAct() - 1);
            result = SimpleResultEnum.SUCCESS;
        }
        return new SetNameResult(result, profile.getRenameAct() + profile.getRenameVipAct(), profile.getName());
    }

    @OnMessage
    public ClearNameResult onClearName(ClearName msg, UserProfile profile) {
        boolean result = profileService.clearName(msg.teamMemberId, profile);
        return new ClearNameResult(result ? SimpleResultEnum.SUCCESS : SimpleResultEnum.ERROR);
    }

    @OnMessage
    public SetBackpackConfResult onSetBackpackConf(SetBackpackConf msg, UserProfile profile) {
        boolean result = weaponService.setBackpackConfs(profile, msg.config1, msg.config2, msg.config3, msg.activeConfig);
        Tuple4<short[], short[], short[], Byte> backpackConfs = weaponService.getBackpackConfs(profile);
        SimpleResultEnum simpleResultEnum = result ? SimpleResultEnum.SUCCESS : SimpleResultEnum.ERROR;
        return new SetBackpackConfResult(simpleResultEnum, backpackConfs._1, backpackConfs._2, backpackConfs._3, backpackConfs._4);
    }

    @OnMessage
    public void onSetHotkeys(SetHotkeys msg, UserProfile profile) {
        weaponService.setHotkeys(profile, msg.hotkeys);
    }

    @OnMessage
    public GetDailyBonusResult onGetDailyBonus(GetDailyBonus msg, UserProfile profile) {
        ArrayList<GenericAwardStructure> awards = new ArrayList<>();
        return new GetDailyBonusResult(profileBonusService.pickUpDailyBonus(profile, awards), awards);
    }

    @OnMessage
    public SelectRaceResult onSelectRace(SelectRace msg, UserProfile profile) {
        boolean result = profileService.selectRaceAndSkin(profile, msg.race, msg.skinId);
        return new SelectRaceResult(SimpleResultEnum.valueOf(result), Race.valueOf(profile.getRace()), skinService.getSkin(profile));
    }

    @OnMessage
    public RaceExceptExclusiveResult onGetRaceExceptExclusive(GetRaceExceptExclusive msg, UserProfile none) {
        return Optional.ofNullable(profileService.getUserProfile(msg.profileId)).map(targetProfile -> {
            byte race = RaceService.getRaceExceptExclusive(targetProfile);
            byte skin = skinService.getSkin(targetProfile, race);
            return new RaceExceptExclusiveResult(msg.profileId, race, skin);
        }).orElse(null);
    }

    @OnMessage
    public void onSetCookies(SetCookies msg, UserProfile profile) {
        CookiesEntity cookiesEntity = cookiesService.getCookiesFor(profile);
        for(int i = 0; i < msg.names.length; i++) {
            cookiesEntity.setValue(msg.names[i], msg.values[i]);
        }
    }

    @OnMessage
    public SellStuffResult onSellStuff(SellStuff msg, UserProfile profile) {
        List<Integer> soldItems = new ArrayList<>();
        List<GenericAwardStructure> resultAwards = new ArrayList<>();
        for(int stuffId : msg.itemsToSell) {
            List<GenericAwardStructure> awards = purchaseService.sellStuff(profile, (short) stuffId);
            if(awards.size() > 0) {
                soldItems.add(stuffId);
                resultAwards.addAll(awards);
            }
        }
        return new SellStuffResult(soldItems, resultAwards, Sessions.getKey());
    }

    @OnMessage
    public MoveProfileSecureToken onGetMoveProfileSecureToken(GetMoveProfileSecureToken msg, UserProfile profile) {
        String secureToken = CloneProfileService.generateIntercomSecureToken();
        Session session = Sessions.get(profile);
        if(!secureToken.isEmpty()) {
            session.getStore().put(UserProfile.INTERCOM_SECURE_TOKEN, secureToken);
        }
        return new MoveProfileSecureToken(secureToken, session.getKey());
    }

}
