package com.pragmatix.app.services.authorize;

import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.server.EnterAccount;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.common.utils.VarInt;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.sessions.Connections;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 16.05.2016 17:24
 */
@Service
public class LoginService {

    @Resource
    private FriendsListService friendsListService;

    @Resource
    private ProfileService profileService;

    @Resource
    private SearchTheHouseService searchTheHouseService;

    @Resource
    private CraftService craftService;

    @Resource
    private SuccessAuthorizeObserver successAuthorizeObserver;

    @Resource
    private RestrictionService restrictionService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Value("${LoginService.debugMode:false}")
    private boolean debugMode = false;

    @Value("${LoginService.logLoginAndLogoutEvents:true}")
    private boolean logLoginLogoutEvents = true;

    public void onSuccessAuthorize(String sessionKey, UserProfile profile, List<Long> ids, long referrerId, ILogin login, boolean isNewProfile) {

        profile.setSocialId((byte) login.getSocialNet().getType());
        profile.version = login.getVersion();

        if(debugMode) {
            String value = getParam(login.getParams(), ILogin.DEBUG_LOGIN_AWARDS);
            if(!value.isEmpty()) {
                Map<String, String> awardParams = Arrays.stream(value.split(" ")).map((s) -> s.split("=")).collect(Collectors.toMap((ss) -> ss[0], (ss) -> ss[1]));
                Connections.get().getStore().put(ILogin.DEBUG_LOGIN_AWARDS, awardParams);
            }
        }

        profile.setSocialId((byte) login.getSocialNet().getType());
        profile.version = login.getVersion();

        EnterAccount enterAccount = new EnterAccount();
        ArrayList<LoginAwardStructure> loginAwards = new ArrayList<>();

        successAuthorizeObserver.beforeSendEnterAccount(profile, login, enterAccount, loginAwards);

        VarInt firstLoginByDay = new VarInt();
        successAuthorizeObserver.fillLoginAwards(profile, referrerId, login.getParams(), isNewProfile, loginAwards, firstLoginByDay);

        fillEnterAccount(enterAccount, sessionKey, profile, friendsListService.getFirstPage(profile, ids), loginAwards);
        if(logLoginLogoutEvents && firstLoginByDay.value == 0) {
            profileEventsService.fireEvent(ProfileEventsService.ProfileEventEnum.LOGIN, profile, new Date(profile.getLogoutTime() * 1000L), login.getParams());
        }
        Messages.toUser(enterAccount);

        successAuthorizeObserver.afterSentEnterAccount(profile);

        profile.setLastLoginTime(new Date());
    }

    private void fillEnterAccount(EnterAccount enterAccount, String sessionKey, UserProfile profile, FriendsListService.FirstPageBean firstPageBean, List<LoginAwardStructure> loginAwards) {
        profile.setUserProfileStructure(null);
        UserProfileStructure profileStructure = profileService.getUserProfileStructure(profile);
        //перестраиваем кешь группы т.к. друзья могли прокачать своих червей в группе пока тебя неболо в игре
        profile.getUserProfileStructure().wormsGroup = profileService.createWormGroupStructures(profile);

        enterAccount.userProfileStructure = profileStructure;
        enterAccount.profileStructures = profileService.getUserProfileStructures(firstPageBean.getFirstPage());
        enterAccount.loginAwards = loginAwards;
        enterAccount.onlineFriends = firstPageBean.getOnlineFriends();
        enterAccount.friends = firstPageBean.getFriends();
        enterAccount.sessionKey = sessionKey;
        enterAccount.availableSearchKeys = searchTheHouseService.getAvailableSearchKeys(profile);
        enterAccount.reagents = craftService.getReagentsForProfile(profile.getId()).getValues();
        enterAccount.currentMission = profile.getCurrentMission();
        enterAccount.currentNewMission = profile.getCurrentNewMission();
        enterAccount.loginSequence = profile.getLoginSequence();
        enterAccount.races = profile.getRaces();
        enterAccount.skins = profile.getSkins();
        enterAccount.selectRaceTime = profile.getSelectRaceTime();
        enterAccount.lastPaymentTime = profile.getLastPaymentTime();
        enterAccount.restrictions = RestrictionService.transformToStructures(restrictionService.getRestrictions(profile.getId()));

        enterAccount.serverTime = AppUtils.currentTimeSeconds();
    }

    public static Optional<String> getParamOpt(String[] params, String extractedParamName) {
        String value = getParam(params, extractedParamName);
        return StringUtils.isEmpty(value) ? Optional.empty() : Optional.of(value);
    }

    public static String getParam(String[] params, String extractedParamName) {
        for(int i = 0; i < params.length; i++) {
            String paramName = params[i];
            if(paramName.equals(extractedParamName)) {
                if(i + 1 < params.length) {
                    return params[i + 1];
                } else {
                    return "";
                }
            }
        }
        return "";
    }

    public static boolean hasParam(String[] params, String extractedParamName) {
        for(String paramName : params) {
            if(paramName.equals(extractedParamName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isLogLoginLogoutEvents() {
        return logLoginLogoutEvents;
    }

}
