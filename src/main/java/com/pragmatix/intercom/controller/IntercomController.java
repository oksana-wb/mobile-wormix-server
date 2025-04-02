package com.pragmatix.intercom.controller;

import com.pragmatix.achieve.common.GrantAwardResultEnum;
import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.domain.RemoteServerRequestMeta;
import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.achieve.services.AchieveCommandService;
import com.pragmatix.app.achieve.AchieveAwardService;
import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.common.BanType;
import com.pragmatix.app.common.Connection;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.messages.server.AwardGranted;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.BanService;
import com.pragmatix.app.services.CloneProfileService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.PurchaseService;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.intercom.messages.GetIntercomProfileRequest;
import com.pragmatix.intercom.messages.GetIntercomProfileResponse;
import com.pragmatix.intercom.messages.IntercomAchieveRequest;
import com.pragmatix.intercom.messages.IntercomAchieveResponse;
import com.pragmatix.intercom.structures.ProfileAchieveStructure;
import com.pragmatix.intercom.structures.UserProfileIntercomStructure;
import com.pragmatix.sessions.IAppServer;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.pragmatix.intercom.messages.GetIntercomProfileResponse.GetBigProfileIntercomResponse;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 12.08.2016 14:16
 */
@Controller
public class IntercomController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private AchieveCommandService achieveService;

    @Resource
    private AchieveAwardService achieveAwardService;

    @Resource
    private PurchaseService purchaseService;

    @Resource
    private ProfileService profileService;

    @Resource
    private CloneProfileService cloneProfileService;

    @Resource
    private BanService banService;

    @OnMessage(connections = {Connection.INTERCOM})
    public GetIntercomProfileResponse onGetIntercomProfileRequest(GetIntercomProfileRequest msg, IAppServer source) {
        Optional<UserProfile> userProfileOpt = profileService.getUserProfileOpt(msg.profileId);
        Optional<UserProfileIntercomStructure> profileIntercomStructureOpt = userProfileOpt
                .filter(profile -> Sessions.getOpt(profile).map(session -> msg.secureToken.equals(session.getStore().remove(UserProfile.INTERCOM_SECURE_TOKEN))).orElse(false) || msg.secureToken.equals(cloneProfileService.masterSecureToken))
                .map(cloneProfileService::dumpToStructure);
        if(msg.banProfile && profileIntercomStructureOpt.isPresent()) {
            banService.addToBanList(msg.profileId, BanType.PROFILE_MOVED_TO, String.format("профиль был перенесен в %s:%s", msg.recipientNetId.name(), msg.recipientProfileId));
            userProfileOpt.flatMap(Sessions::getOpt).ifPresent(Session::close);
        }
        return GetBigProfileIntercomResponse(msg, profileIntercomStructureOpt.orElse(null));
    }

    @OnMessage(connections = {Connection.INTERCOM})
    public void onGetIntercomProfileResponse(GetIntercomProfileResponse msg, IAppServer source) {
        profileService.getUserProfileOpt(msg.recipientProfileId)
                .flatMap(userProfile -> remoteServerRequestMeta(msg.requestNum, userProfile.remoteServerRequestQueue))
                .ifPresent(requestMeta -> requestMeta.future.complete(Optional.ofNullable(msg.profileIntercomStructure)));
    }

    @OnMessage(connections = {Connection.INTERCOM})
    public void onIntercomAchieveResponse(IntercomAchieveResponse msg, IAppServer source) {
        if(msg.requestType == IntercomAchieveRequest.RequestType.GetAchievements) {
            UserProfile userProfile = profileService.getUserProfile(msg.profileId);
            onAchieveResponse(msg, userProfile.remoteServerRequestQueue, response -> response.profileAchievements);
        } else {
            ProfileAchievements profileAchievements = achieveService.getProfileAchievements(msg.profileId);
            if(profileAchievements != null) {
                if(msg.requestType == IntercomAchieveRequest.RequestType.BuyResetBonusItems) {
                    onAchieveResponse(msg, profileAchievements.remoteServerRequestQueue, response -> ShopResultEnum.valueOf(response.resultEnumType));
                } else if(msg.requestType == IntercomAchieveRequest.RequestType.GiveBonusItem) {
                    onAchieveResponse(msg, profileAchievements.remoteServerRequestQueue, response -> GrantAwardResultEnum.valueOf(response.resultEnumType));
                }
            }
        }
    }

    private Optional<RemoteServerRequestMeta> remoteServerRequestMeta(long requestNum, List<RemoteServerRequestMeta> remoteServerRequestQueue) {
        for(RemoteServerRequestMeta requestMeta : remoteServerRequestQueue) {
            if(System.currentTimeMillis() > requestMeta.timeout) {
                if(log.isDebugEnabled()) log.debug("request is expired {}", requestMeta);
                remoteServerRequestQueue.remove(requestMeta);
            } else if(requestNum == requestMeta.requestNum) {
                remoteServerRequestQueue.remove(requestMeta);
                return Optional.of(requestMeta);
            }
        }
        if(log.isDebugEnabled()) log.debug("request not found by requestNum {}", requestNum);
        return Optional.empty();
    }

    private void onAchieveResponse(IntercomAchieveResponse msg, List<RemoteServerRequestMeta> remoteServerRequestQueue, Function<IntercomAchieveResponse, Object> resultValue) {
        remoteServerRequestMeta(msg.requestNum, remoteServerRequestQueue).ifPresent(requestMeta -> requestMeta.future.complete(resultValue.apply(msg)));
    }

    @OnMessage(connections = {Connection.INTERCOM})
    public IntercomAchieveResponse onIntercomAchieveRequest(IntercomAchieveRequest msg, IAppServer source) {
        if(msg.requestType == IntercomAchieveRequest.RequestType.GrantAwards
                || msg.requestType == IntercomAchieveRequest.RequestType.GiveBonusItem
                || msg.requestType == IntercomAchieveRequest.RequestType.BuyResetBonusItems
                ) {
            //== Main ==
            UserProfile profile = profileService.getUserProfile(msg.profileId);
            if(profile == null) {
                log.error("UserProfile not found by id {}", msg.profileId);
            } else {
                synchronized (profile) {
                    // выдать награду за достижение
                    if(msg.requestType == IntercomAchieveRequest.RequestType.GrantAwards) {
                        Tuple2<GrantAwardResultEnum, List<GenericAwardStructure>> result = achieveAwardService.grantAwards(profile, msg.awards, msg.timeSequence);
                        if(result._1 != GrantAwardResultEnum.OK) {
                            log.error("[{}] Не удалось выдать игроку награду за достижение! timeSequence={} awards={}", profile, msg.timeSequence, msg.awards);
                        } else {
                            AwardGranted awardGranted = new AwardGranted(AwardTypeEnum.ACHIEVE, result._2, "" + msg.timeSequence, Sessions.getKey(profile));
                            Messages.toUser(awardGranted, profile, Connection.MAIN);
                        }
                        // выбрать призовой предмет
                    } else if(msg.requestType == IntercomAchieveRequest.RequestType.GiveBonusItem) {
                        Tuple2<GrantAwardResultEnum, List<GenericAwardStructure>> result = achieveAwardService.grantAwards(profile, msg.awards, msg.timeSequence);
                        return IntercomAchieveResponse.GiveBonusItemResponse(msg, result._1);
                        // сбросить призовые предметы
                    } else if(msg.requestType == IntercomAchieveRequest.RequestType.BuyResetBonusItems) {
                        ShopResultEnum result = purchaseService.buyResetBonusItems(msg.profileId);
                        return IntercomAchieveResponse.BuyResetBonusItemsResponse(msg, result);
                    }
                }
            }
        } else {
            //== Achieve ==
            ProfileAchievements profileAchievements = achieveService.getProfileAchievements(msg.profileId);
            if(profileAchievements != null) {
                synchronized (profileAchievements) {
                    // синхронизировать количество призовых предметов
                    if(msg.requestType == IntercomAchieveRequest.RequestType.SetInvestedAwardPoints) {
                        achieveService.setInvestedAwardPoints(msg.profileId, WormixAchievements.class, msg.profilesBonusItemsCount);
                        // обнулить достижения
                    } else if(msg.requestType == IntercomAchieveRequest.RequestType.WipeAchievements) {
                        achieveService.wipeAchievements(msg.profileId, WormixAchievements.class);
                    } else if(msg.requestType == IntercomAchieveRequest.RequestType.GetAchievements) {
                        return IntercomAchieveResponse.GetAchievementsResponse(msg, new ProfileAchieveStructure(profileAchievements));
                    }
                }
            } else {
                if(msg.requestType == IntercomAchieveRequest.RequestType.GetAchievements) {
                    return IntercomAchieveResponse.GetAchievementsResponse(msg, new ProfileAchieveStructure(new WormixAchievements(msg.profileId)));
                }
            }
        }
        return null;
    }

}
