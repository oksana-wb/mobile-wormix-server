import com.pragmatix.achieve.domain.WormixAchievements

import com.pragmatix.app.dao.UserProfileDao
import com.pragmatix.app.domain.BanEntity
import com.pragmatix.app.init.UserProfileCreator
import com.pragmatix.app.messages.server.ShowSystemMessage
import com.pragmatix.app.model.BanItem
import com.pragmatix.app.model.UserProfile
import com.pragmatix.app.services.BanService
import com.pragmatix.app.services.DailyRegistry
import com.pragmatix.app.services.DaoService
import com.pragmatix.app.services.ProfileService
import com.pragmatix.app.services.PromoService
import com.pragmatix.app.services.StatisticService
import com.pragmatix.app.services.StuffService
import com.pragmatix.gameapp.cache.SoftCache
import com.pragmatix.gameapp.sessions.Session
import com.pragmatix.gameapp.sessions.Sessions
import com.pragmatix.gameapp.threads.Execution
import com.pragmatix.intercom.service.AchieveServerAPI
import com.pragmatix.webadmin.ExecAdminScriptException
import com.pragmatix.wormix.webadmin.interop.InteropSerializer
import com.pragmatix.wormix.webadmin.interop.ServiceResult
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import com.pragmatix.wormix.webadmin.interop.CommonResponse
import org.springframework.transaction.support.TransactionCallbackWithoutResult
import org.springframework.transaction.support.TransactionTemplate

def kickOut(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    String profileId = map.get("profileId");
    UserProfile profile = getUserProfile(context, profileId)

    Session session = Sessions.get(profile);
    if(session == null) {
        return new CommonResponse(ServiceResult.ERR_RESPONSE, "Session for profile [${profileId}] not found", "1");
    }

    session.close();

    return "[${profileId}] successfully kickOutted"
}

def clearCache(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    def banService = context.getBean(BanService.class)
    def daoService = context.getBean(DaoService.class)
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    String profileId = map.get("profileId");
    UserProfile profile = getUserProfile(context, profileId)

    def softCache = context.getBean(SoftCache.class);
    def profileService = context.getBean(ProfileService.class);

    if(Sessions.get(profile) != null) {
        return new CommonResponse(ServiceResult.ERR_RESPONSE, "Can't reset cache for profile [${profileId}] He's online", "1");
    }

    softCache.remove(UserProfile.class, profile.getId())
    softCache.remove(WormixAchievements.class, profileService.getProfileAchieveId(profile.getId()));
    
    updateBans(banService, daoService, profile.getId())

    return "[${profileId}] successfully cleaned"
}

def updateBans(BanService banService, DaoService daoService, Long profileId) {
    List<BanEntity> banEntities = daoService.getBanDao().getActualBanList().findAll{it.profileId == profileId}
    Map<Long, BanItem> banList = [:]
    for(BanEntity banEntity : banEntities) {
        Long banKey = banEntity.getProfileId();
        // дополнительная проверка на случай если у игрока больше одного действующего бана
        if (!banList.containsKey(banKey)) {
            banList.put(banKey, new BanItem(banEntity));
        } else {
            BanItem banItem = banList.get(banKey);
            if (banItem.getEndDate() == null) {
                // вечный бан - остальные игнориуются
                continue;
            } else if (banEntity.getEndDate() == null || banEntity.getEndDate().getTime() > banItem.getEndDate()) {
                // затираем прежний бан
                banList.put(banKey, new BanItem(banEntity));
            }
        }
    }
    banService.init(banList);
}

def wipeProfile(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    String profileId = map.get("profileId");
    UserProfile profile = getUserProfile(context, profileId)

    def achieveServerAPI = context.getBean(AchieveServerAPI.class);
    def profileService = context.getBean(ProfileService.class);
    def profileCreator = context.getBean(UserProfileCreator.class);
    def statisticService = context.getBean(StatisticService.class);

    try {
        context.getBean(PromoService.class).deactivatePromoKeys(profile);
    } catch (Exception e) {
    }

    try {
        // обнуляем достижения и статистику
        def stringId = profileService.getProfileAchieveId(profile.getId())
        achieveServerAPI.wipeAchievements(stringId);

        final Session session = Sessions.get(profile);
        // проверяем в online ли он
        if(session != null) {
            session.close();
            Thread.sleep(300)
        }

        statisticService.wipeStatistic(profile, "", request.adminUser);
        profileCreator.wipeUserProfile(profile);
    } catch (Exception e) {
        LoggerFactory.getLogger(this.getClass()).error(e.toString(), e)
        throw e
    }

    return "[${profileId}] successfully wiped"
}

def sendMessage(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer();
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class);
    String profileId = map.get("profileId");
    String msg = map.get("message");

    UserProfile profile = getUserProfile(context, profileId)
    if(profile.isOnline()) {
        com.pragmatix.gameapp.sessions.Connection conn = Sessions.get(profile).getConnection(0);
        Execution exec = Execution.EXECUTION.get();
        if(conn != null) {
            exec.sendMessage(new ShowSystemMessage(msg), conn);
        }
        return "message to user [${profileId}] successfully sended";
    } else {
        console.println("profile [${profileId}] is offline");
        throw new ExecAdminScriptException(ServiceResult.ERR_RESPONSE, "1");
    }
}

def clearMission(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    String profileId = map.get("profileId");
    UserProfile profile = getUserProfile(context, profileId)
    StuffService stuffService = context.getBean(StuffService.class);
    DailyRegistry dailyRegistry = context.getBean(DailyRegistry.class);

    dailyRegistry.clearMission(profile.getId());
//    stuffService.getMissionStuffs().each {
//       stuffService.removeStuff(profile, it)
//    }
    return "[${profileId}] mission successfully cleaned"
}

def resetMissions(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    String profileId = map.get("profileId");
    UserProfile profile = getUserProfile(context, profileId)
    StuffService stuffService = context.getBean(StuffService.class);
    DailyRegistry dailyRegistry = context.getBean(DailyRegistry.class);
    TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class);
    UserProfileDao userProfileDao = context.getBean(UserProfileDao.class);

    dailyRegistry.clearMission(profile.getId());
//    stuffService.getMissionStuffs().each {
//       stuffService.removeStuff(profile, it)
//    }

    profile.currentMission = Math.min(profile.currentMission, 10)

    transactionTemplate.execute({ status -> userProfileDao.updateProfile(profile) } as TransactionCallbackWithoutResult);

    return "[${profileId}] missions successfully reseted"
}

def UserProfile getUserProfile(ApplicationContext context, String profileId) {
    ProfileService profileService = context.getBean(ProfileService.class)
    UserProfile profile = profileService.getUserProfile(profileId)

    if(profile == null) {
        throw new ExecAdminScriptException(ServiceResult.ERR_PROFILE_NOT_FOUND, "UserProfile not found by id ${profileId}")
    }
    return profile
}