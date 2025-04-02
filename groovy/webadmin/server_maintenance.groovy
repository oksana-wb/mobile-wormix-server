import com.pragmatix.achieve.domain.WormixAchievements
import com.pragmatix.achieve.services.AchieveCommandService
import com.pragmatix.admin.messages.client.Discard
import com.pragmatix.app.common.*
import com.pragmatix.app.dao.BundleDao
import com.pragmatix.app.dao.ReferralLinkDao
import com.pragmatix.app.domain.ReferralLinkEntity
import com.pragmatix.app.init.controller.InitController
import com.pragmatix.app.messages.server.ShowSystemMessage
import com.pragmatix.app.messages.structures.GenericAwardStructure
import com.pragmatix.app.model.UserProfile
import com.pragmatix.app.services.*
import com.pragmatix.app.services.rating.RatingService
import com.pragmatix.app.services.rating.RatingServiceImpl
import com.pragmatix.app.settings.BattleAwardSettings
import com.pragmatix.app.settings.HeroicMissionState
import com.pragmatix.arena.mercenaries.MercenariesService
import com.pragmatix.arena.mercenaries.messages.BackpackItemShortStruct
import com.pragmatix.arena.mercenaries.messages.MercenariesTeamMember
import com.pragmatix.chat.GlobalChatService
import com.pragmatix.chat.messages.ChatMessage
import com.pragmatix.clanserver.domain.ClanMember
import com.pragmatix.clanserver.messages.request.TopClansRequest
import com.pragmatix.clanserver.messages.response.TopClansResponse
import com.pragmatix.clanserver.services.ClanSeasonService
import com.pragmatix.clanserver.services.ClanServiceImpl
import com.pragmatix.gameapp.GameApp
import com.pragmatix.gameapp.IGameApp
import com.pragmatix.gameapp.cache.SoftCache
import com.pragmatix.gameapp.messages.Messages
import com.pragmatix.gameapp.services.OnlineService
import com.pragmatix.gameapp.sessions.Session
import com.pragmatix.gameapp.sessions.Sessions
import com.pragmatix.gameapp.social.SocialService
import com.pragmatix.gameapp.social.SocialServiceEnum
import com.pragmatix.gameapp.threads.Execution
import com.pragmatix.pvp.BattleWager
import com.pragmatix.pvp.services.PvpService
import com.pragmatix.sessions.IAppServer
import com.pragmatix.webadmin.AdminHandler
import com.pragmatix.wormix.webadmin.interop.InteropSerializer
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest
import com.pragmatix.wormix.webadmin.interop.response.structure.UserProfileStructure
import com.pragmatix.wormix.webadmin.interop.response.structure.WormStructure
import com.pragmatix.wormix.webadmin.interop.response.structure.clan.Rank
import com.thoughtworks.xstream.XStream
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionCallbackWithoutResult
import org.springframework.transaction.support.TransactionTemplate

import java.text.SimpleDateFormat

// выгнать всех
def kickOutAll(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    OnlineService onlineService = context.getBean(OnlineService.class);
    GameApp gameApp = context.getBean(GameApp.class);
    ReactionRateService reactionRateService = context.getBean(ReactionRateService.class);
    RatingService ratingService = context.getBean(RatingService.class);

    onlineService.setDiscard(true);

    // уведомляем PVP сервер
    def (msg, pvpServer) = newDiscardMessage(context)
    msg.event = Discard.DISCARD
    Messages.toServer(msg as Discard, pvpServer as IAppServer, true);
    // делаем паузу, чтобы получить с pvp сервера команды об окончании боёв
    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) {
    }

    for(Session session : gameApp.getSessions()) {
        Object user = session.getUser();
        if(user instanceof UserProfile) {
            UserProfile userProfile = (UserProfile) user;
            if(userProfile.getBattleState() == BattleState.SIMPLE) {
                userProfile.setBattlesCount(userProfile.getBattlesCount() + 1);
            }

            session.close();
        }
    }

    //сохраняем кто кому прокачивал реакцию
    reactionRateService.persistToDisk();
    // сохраняем ежедневные рейтинги
    ratingService.persistToDisk();

    return ("Everybody is Discarded");
}

def admitAll(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    OnlineService onlineService = context.getBean(OnlineService.class);

    // уведомляем PVP сервер
    def (msg, pvpServer) = newDiscardMessage(context)
    msg.event = Discard.UNDISARD;
    Messages.toServer(msg as Discard, pvpServer as IAppServer, true);

    onlineService.setDiscard(false);

    return ("UnDiscard, now you can login.");
}

// разрешить всем логиниться
def newDiscardMessage(ApplicationContext context) {
    def socialService = context.getBean(SocialService.class)
    def initController = context.getBean(InitController.class)

    Discard msg = new Discard()
    msg.socialService = socialService.getDefaultSocialServiceId() as SocialServiceEnum
    [msg, initController.pvpServerAddress]
}

// разослать всем сообщение
def broadcast(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer();
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class);
    String msgRu = map.get("message");
    String msgEn = map.get("message_en");

    GameApp gameApp = context.getBean(GameApp.class);
    int count = 0;
    int sessionCount = 0;

    for(Session session : gameApp.getSessions()) {
        sessionCount++;
        ///отправляем системное сообщение всем кто в онлайне*/
        def conn = Sessions.get(session.getKey()).getConnection(0);
        Execution exec = Execution.EXECUTION.get();
        if(conn != null) {
            UserProfile profile = session.getUser() as UserProfile
            String msg = msgRu;
            if(profile.getLocale() == Locale.EN && msgEn != null && !msgEn.isEmpty()) {
                msg = msgEn
            }
            exec.sendMessage(new ShowSystemMessage(msg), conn);
            count++;
        }
    }

    return "Sending system message sucessfull in ${count} connections. Iterated ${sessionCount} sessions";
}

def getTop(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def ratingService = context.getBean(RatingService.class)
    def profileService = context.getBean(ProfileService.class)
    boolean global = false
    Closure<LinkedHashMap<String, Serializable>> collectTop = {
        def profile = profileService.getUserProfile(it.profileId)
        byte socialNetId = profileService.getSocialIdForClan(profile) as byte
        [
                profileId  : it.profileId,
                profileName: it.name ?: getProfileName(context, it.profileId, it.profileStringId, socialNetId),
                clanId     : getClanId(context, it.profileId, socialNetId),
                clanName   : getClanName(context, it.profileId, socialNetId),
                rating     : global ? it.rating : it.ratingPoints,
                oldPlace   : it.oldPlace,
        ]
    }
    def dailyTop = ratingService.getTop(RatingType.Daily, BattleWager.valueOf(request.scriptParams), new UserProfile(1L))?.profileStructures?.collect(collectTop)
    def yesterdayTop = ratingService.getTop(RatingType.Yesterday, BattleWager.valueOf(request.scriptParams), new UserProfile(1L))?.profileStructures?.collect(collectTop)
    def seasonalTop = ratingService instanceof RatingServiceImpl ?
            seasonalTop = ratingService.getTop(RatingType.Seasonal, BattleWager.valueOf(request.scriptParams), new UserProfile(1L)).profileStructures.collect(collectTop)
            : []
    global = true
    def profile = new UserProfile(1L)
    profile.setRating(10000)
    def rubyLeague = ratingService.getTop(RatingType.Global, BattleWager.valueOf(request.scriptParams), profile).profileStructures.collect(collectTop)
    return AdminHandler.xstream.toXML([
            dailyTop    : dailyTop ?: [],
            yesterdayTop: yesterdayTop ?: [],
            seasonalTop : seasonalTop,
            rubyLeague  : rubyLeague
    ])
}

def getClanTop(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ClanServiceImpl clanService = context.getBean(ClanServiceImpl.class)
    TopClansResponse topResponse = clanService.topClans(new TopClansRequest(true), null)
    int i = 0
    def clanTop = topResponse.clans.collect() {
        [
                size              : it.size,
                createDate        : it.createDate,
                id                : it.id,
                joinRating        : it.joinRating,
                level             : it.level,
                prevSeasonTopPlace: it.prevSeasonTopPlace,
                name              : it.name,
                reviewState       : it.reviewState.code,
                rating            : it.seasonRating,
                oldPlace          : topResponse.oldPlaces[i++],
        ]

    }
    return AdminHandler.xstream.toXML([
            clanTop         : clanTop,
            startSeasonDate : topResponse.startSeasonDate,
            finishSeasonDate: topResponse.finishSeasonDate,
    ])
}

private String getProfileName(ApplicationContext context, long profileId, String profileStringId, byte socialNetId) {
    def clanSeasonService = context.getBean(ClanSeasonService.class)
    if(clanSeasonService.ENABLED) {
        def pvpService = context.getBean(PvpService.class)
        def clanService = context.getBean(ClanServiceImpl.class)
        def member = clanService.getClanMember((short) socialNetId, (int) profileId)
        member != null ? member.name : pvpService.userIdToNameMap.get(PvpService.getPvpUserId(profileId, socialNetId)) ?: "" + profileId
    } else {
        def v = profileStringId.split("#")
        v.length > 1 ? v[1] : profileStringId
    }
}

private String getClan(ApplicationContext context, long profileId, byte socialNetId) {
    ClanServiceImpl clanService = context.getBean(ClanServiceImpl.class)
    ClanMember member = clanService.getClanMember((short) socialNetId, (int) profileId)
    return member != null ? member.clanId + "#" + member.clan.name : "0"
}

private int getClanId(ApplicationContext context, long profileId, byte socialNetId) {
    def clanSeasonService = context.getBean(ClanSeasonService.class)
    if(clanSeasonService.ENABLED) {
        def clanService = context.getBean(ClanServiceImpl.class)
        def member = clanService.getClanMember((short) socialNetId, (int) profileId)
        member != null ? member.clanId : 0
    } else {
        0
    }
}

private String getClanName(ApplicationContext context, long profileId, byte socialNetId) {
    def clanSeasonService = context.getBean(ClanSeasonService.class)
    if(clanSeasonService.ENABLED) {
        def clanService = context.getBean(ClanServiceImpl.class)
        def member = clanService.getClanMember((short) socialNetId, (int) profileId)
        member != null ? member.clan.name : ""
    } else {
        ""
    }
}

def getOnlineProfiles(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def gameApp = context.getBean(IGameApp.class)
    def profileService = context.getBean(ProfileService.class)
    def dailyRegistry = context.getBean(DailyRegistry.class)
    def jdbcTemplate = context.getBean(JdbcTemplate.class)
    def softCache = context.getBean(SoftCache.class)
    def achieveCommandService = context.getBean(AchieveCommandService.class)
    def achievementService = achieveCommandService.getService(new WormixAchievements(""))

    int level = request.scriptParams ? request.scriptParams as int : 0
    byte socialNetId = (byte) profileService.getSocialIdFor((byte) -1).type

    List<UserProfileStructure> profiles = new ArrayList<>()
    int maxSessions = 200
    int sessions = 0
    Map<Integer, Integer> online = [:]
    (1..99).each { online.put(it, 0) }
    for(Session session : gameApp.getSessions()) {
        Object user = session.getUser()
        if(user instanceof UserProfile) {

            UserProfile profile = (UserProfile) user
            if(!profile.isOnline()) continue

            online.put(profile.level, online.get(profile.level) + 1)

            if(level > 0 && level <= 99) {
                if(profile.level != level) {
                    continue
                }
            }

            def achieveId = profileService.getProfileAchieveId(profile.id)
            def profileAchievements = softCache.get(WormixAchievements.class, achieveId)

            UserProfileStructure profileStruct = new UserProfileStructure()
            com.pragmatix.app.messages.structures.UserProfileStructure userProfileStructure = profileService.getUserProfileStructure(profile)
            sessions++;
            if(sessions <= maxSessions || level > 0) {
                profileStruct.id = profileService.getProfileSocialId(profile)
                profileStruct.profileId = profile.getId()
                profileStruct.money = userProfileStructure.money
                profileStruct.realMoney = userProfileStructure.realMoney
                profileStruct.rating = userProfileStructure.rating
                profileStruct.reactionRate = userProfileStructure.reactionRate
                profileStruct.battlesCount = profile.battlesCount
                profileStruct.lastLoginTime = profile.lastLoginTime
                profileStruct.loginSequence = profile.loginSequence
                profileStruct.currentMission = profile.currentMission
                profileStruct.currentNewMission = profile.currentNewMission
                profileStruct.searchKeys = dailyRegistry.getSearchKeys(profile.getId())
                profileStruct.howManyPumped = dailyRegistry.getHowManyPumped(profile.getId())
                profileStruct.successedMission = dailyRegistry.isSuccessedMission(profile.getId())
                profileStruct.dailyRatings = dailyRegistry.getDailyRatings(profile.getId()) ?: new int[BattleWager.values().length]

                if(userProfileStructure.clanMember) {
                    profileStruct.clanId = userProfileStructure.clanMember.clanId
                    profileStruct.clanName = userProfileStructure.clanMember.clanName
                    profileStruct.rank = Rank.valueOf(userProfileStructure.clanMember.rank.name())
                }

                try {
                    profileStruct.registerDate = jdbcTemplate.queryForObject("SELECT creation_date FROM wormswar.creation_date WHERE id = ?", Date.class, profile.getId())
                } catch (Exception e) {
                }

                // юниты
                profileStruct.wormsGroup = new WormStructure[userProfileStructure.wormsGroup.length]
                int i = 0;
                userProfileStructure.wormsGroup.each {
                    WormStructure str = new WormStructure()
                    str.ownerId = "" + it.ownerId
                    str.ownerStringId = it.ownerStringId
                    str.armor = it.armor
                    str.attack = it.attack
                    str.level = it.level
                    str.experience = it.experience
                    str.hatId = it.hat
                    str.raceId = it.race
                    str.kitId = it.kit
                    str.name = it.name
                    profileStruct.wormsGroup[i] = str
                    i++
                }

                profileStruct.profileName = profile.name ?: getProfileName(context, profile.getId(), profile.getProfileStringId(), socialNetId)

                def metaMap = [:]
                metaMap.deviceId = profile.id
                metaMap.locale = profile.locale?.name()
                metaMap.achievePoints = profileAchievements ? achievementService.countAchievePoints(profileAchievements) : null
                profileStruct.meta = AdminHandler.xstream.toXML(metaMap)

                profiles.add(profileStruct)
            }
        }
    }
    return AdminHandler.xstream.toXML([profiles: profiles, online: online])
}

def getReferralLinks(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ReferralLinkService referralLinkService = context.getBean(ReferralLinkService.class)
    ReferralLinkDao referralLinkDao = context.getBean(ReferralLinkDao.class)
    List<ReferralLinkEntity> allList = referralLinkDao.getAllList()
    def result = allList.sort { a, b -> b.id - a.id }.collect {
        [
                live      : isLive(referralLinkService, it.token),
                token     : it.token,
                start     : it.start,
                finish    : it.finish,
                limit     : it.limit,
                visitors  : it.visitors,
                ruby      : it.ruby,
                fuzy      : it.fuzy,
                battles   : it.battles,
                reaction  : it.reaction,
                reagents  : it.reagents,
                weapons   : it.weapons,
                experience: it.experience,
                bossToken : it.bossToken,
                wagerToken: it.wagerToken,
        ]
    }
    XStream stream = new XStream()
    return stream.toXML(result)
}

def boolean isLive(ReferralLinkService referralLinkService, String referralLinkToken) {
    ReferralLinkEntity referralLink = referralLinkService.referralLinks.get(referralLinkToken);
    return referralLink != null && new Date().before(referralLink.finish) && ((referralLink.limit == 0 || (referralLink.limit > 0 && referralLink.visitors < referralLink.limit)));
}

def newReferralLink(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm")

    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)

    ReferralLinkService referralLinkService = context.getBean(ReferralLinkService.class)

    Date start = DATE_TIME_FORMAT.parse(map.start as String)
    Date finish = DATE_TIME_FORMAT.parse(map.finish as String)
    int limit = map.limit as Integer
    int ruby = map.ruby as Integer
    int fuzy = map.fuzy as Integer
    int battles = map.battles as Integer
    int reaction = map.reaction as Integer
    int experience = map.experience as Integer
    int bossToken = map.bossToken as Integer
    int wagerToken = map.wagerToken as Integer

    referralLinkService.addNewReferralLink(
            start,
            finish,
            limit,
            ruby,
            fuzy,
            battles,
            reaction,
            map.reagents as String,
            map.weapons as String,
            experience,
            bossToken,
            wagerToken
    )
    return "0"
}

def removeReferralLink(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ReferralLinkService referralLinkService = context.getBean(ReferralLinkService.class)
    referralLinkService.removeReferralLink(request.scriptParams)

    return "0"
}

def getTopDonaters(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def result = []
    context.getBean(JdbcTemplate.class).queryForList(
            """SELECT profile_id AS profileId, count(*) AS count, sum(votes) AS votes, max(date) AS lastDate FROM payment_statistic_parent WHERE payment_status = 0
            GROUP BY 1
            ORDER BY 3 DESC
            LIMIT 100""").each {
        result.add(
                [
                        profileId: it.profileId,
                        count    : it.count,
                        votes    : it.votes,
                        lastDate : it.lastDate,
                ]
        )
    }
    AdminHandler.xstream.toXML(result)
}

def sendVkActiveGamers(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    Map<String, String> params = AdminHandler.xstream.fromXML(request.scriptParams) as Map
    synchronized (context) {
        def script = "/home/user/bin/tester_candidats/find_and_send.sh"
        command = ["/bin/bash", "-c", "${script} ${params.days} ${params.ratingMin} ${params.ratingMax} ${params.level} ${params.pvpBattles} ${params.bossWin} ${params.email} ${params.maxLevel}"]
        command.execute()
        AdminHandler.xstream.toXML("OK")
    }
}

def getTesters(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def whiteList = context.getBean(WhiteList.class)
    AdminHandler.xstream.toXML(new TreeSet(whiteList.getSet()))
}

def setTesters(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def whiteList = context.getBean(WhiteList.class)

    InteropSerializer serializer = new InteropSerializer()
    def newSet = new TreeSet<>(serializer.fromString(request.scriptParams, Map.class).keySet())
    whiteList.setSet(newSet, true)

    AdminHandler.xstream.toXML("OK")
}

def getBundles(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def bundleService = context.getBean(BundleService.class)
    def profileBonusService = context.getBean(ProfileBonusService.class)
    def stuffService = context.getBean(StuffService.class)
    def weaponService = context.getBean(WeaponService.class)
    def allList = context.getBean(BundleDao.class).getAllList()
    def result = allList
            .sort { a, b -> int res = Boolean.compare(bundleService.isValid(b), bundleService.isValid(a)); res == 0 ? a.sortOrder - b.sortOrder : res }
            .collect { entity ->
        ArrayList<GenericAwardStructure> awardItems = []
        profileBonusService.setGenericAwardItems(entity.items, awardItems)
        def stuffs = awardItems.findAll { it.awardKind == AwardKindEnum.STUFF || it.awardKind == AwardKindEnum.TEMPORARY_STUFF }.collect {
            [stuff: stuffService.getStuff(it.itemId), itemId: it.itemId, count: it.count]
        }
        def weapons = awardItems.findAll { it.awardKind == AwardKindEnum.WEAPON || it.awardKind == AwardKindEnum.WEAPON_SHOT }.collect {
            [weapon: weaponService.getWeapon(it.itemId), itemId: it.itemId, count: it.count]
        }
        [
                id        : entity.id,
                code      : entity.code,
                order     : entity.sortOrder,
                start     : entity.start,
                finish    : entity.finish,
                votes     : entity.votes,
                discount  : entity.discount,
                createDate: entity.createDate,
                updateDate: entity.updateDate,
                enabled   : !entity.disabled,
                valid     : bundleService.isValid(entity),
                races     : entity.races,
                skins     : entity.skins,
                weapons   : weapons.collect { [id: it.weapon.weaponId, count: it.count, name: it.weapon.name] },
                hats      : stuffs.findAll { it.stuff.hat }.collect { formatStuff(it) },
                kits      : stuffs.findAll { it.stuff.kit }.collect { formatStuff(it) },
                boosters  : stuffs.findAll { it.stuff.boost }.collect { formatStuff(it) },
        ]
    }
    AdminHandler.xstream.toXML(result)
}

def persistBundle(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm")
    Map<String, Object> map = new InteropSerializer().fromString(request.scriptParams, Map.class)
    def bundleService = context.getBean(BundleService.class)
    bundleService.persistBundle(
            map.id as Long,
            map.code as String,
            map.order as int,
            map.start != "null" ? sdf.parse(map.start as String) : null,
            map.finish != "null" ? sdf.parse(map.finish as String) : null,
            map.discount as int,
            map.votes as float,
            map.races as String,
            map.skins as String,
            map.items as String,
            !(map.enabled as boolean),
    )
    "0"
}

protected Map formatStuff(Map<String, Object> it) {
    [id: it.stuff.stuffId, count: it.count, name: it.stuff.name]
}

def superBossDailyStat(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def battleAwardSettings = context.getBean(BattleAwardSettings.class)
    def heroicMissionService = context.getBean(HeroicMissionService.class)
    Map<String, Object> params = new InteropSerializer().fromString(request.scriptParams, Map.class)
    String forDate = params.get("forDate") as String
    int offset = params.get("offset") as int
    String out
    synchronized (context) {
        def command = ["/bin/bash", "-c", "support/bin/webadmin/sboss_daily_stat.sh ${forDate}"]
        def proc = command.execute()
        proc.waitFor()
        out = proc.in.text
    }
//    String out = """
//      5 [104,14]/69 NOT_WINNER
//      2 [104,14]/69 WINNER
//    1439 [1,104]/14 NOT_WINNER
//     23 [1,104]/14 WINNER
//     17 [12,1]/39 NOT_WINNER
//     15 [12,1]/39 WINNER
//    3009 [12,6]/86 NOT_WINNER
//    609 [12,6]/86 WINNER
//      2 [13,104]/72 NOT_WINNER
//      2 [16,12]/74 NOT_WINNER
//      2 [16,12]/74 WINNER
//    13469 [4,101]/108 NOT_WINNER
//    1081 [4,101]/108 WINNER
//    5399 [9,3]/52 NOT_WINNER
//    5392 [9,3]/52 WINNER
//    """
    def result = []
    def parsedOut = out.split("\n").findAll { !it.trim().isEmpty() } collect { it ->
        def arr = it.trim().split(" ")
        def arr2 = arr[1].split("[\\[\\]/,]")
        def sboss = arr2[1] + "_" + arr2[2]
        [
                result: arr[2],
                value : arr[0],
                sboss : sboss,
                level : -1,
                map   : arr2[4],
        ]
    }

    def heroicMissionStates = heroicMissionService.heroicMissionStates
//    def heroicMissionStates = [
//            newHeroicMissionState(["2_3", "9_13", "11_1", "12_1", "1_104"]),
//            newHeroicMissionState(["101_13", "11_4", "11_13", "13_104", "9_3"]),
//            newHeroicMissionState(["4_2", "15_8", "16_101", "16_12", "12_6"]),
//            newHeroicMissionState(["15_104", "16_8", "15_10", "104_14", "4_101"]),
//    ]
    def keys = heroicMissionStates.collect { it -> it.missionsHistory[it.missionsHistory.size() - offset - 1] }

    keys.eachWithIndex { item, index ->
        def win = parsedOut.find { it -> it.sboss == item && it.result == "WINNER" }
        def defeat = parsedOut.find { it -> it.sboss == item && it.result == "NOT_WINNER" }
        result.add([
                levelInPlace: index as int,
                sboss       : item,
                map         : win ? win.map as int : 0,
                levelCurr   : battleAwardSettings.getLevel(item),
                win         : win ? win.value as int : 0,
                defeat      : defeat ? defeat.value as int : 0,
        ])
    }
    AdminHandler.xstream.toXML(result)
}

def HeroicMissionState newHeroicMissionState(List<String> missionsHistory) {
    def result = new HeroicMissionState()
    result.missionsHistory = missionsHistory
    result
}

def setSuperBossLevel(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def battleAwardSettings = context.getBean(BattleAwardSettings.class)
    def transactionTemplate = context.getBean(TransactionTemplate.class)
    Map<String, Object> params = new InteropSerializer().fromString(request.scriptParams, Map.class)
    def key = params.key as String
    def level = params.level as int
    if(battleAwardSettings.heroicMissionLevels.containsKey(key) && level >= 0 && level < 4) {
        battleAwardSettings.heroicMissionLevels.put(key, level)
        transactionTemplate.execute({ status -> battleAwardSettings.persistLevels() } as TransactionCallbackWithoutResult)
    }
    AdminHandler.xstream.toXML("OK")
}

def getGlobalChatHistory(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def chatService = context.getBean(GlobalChatService.class)
    historyToXml(chatService.chatHistory() ?: new ChatMessage[0])
}

def removeMessageFromGlobalChatHistory(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def chatService = context.getBean(GlobalChatService.class)
    Map<String, Object> params = new InteropSerializer().fromString(request.scriptParams, Map.class)
    def messageId = params.messageId as long
    def profileId = params.profileId as int
    chatService.removeMessageFromHistory(messageId, profileId)

    AdminHandler.xstream.toXML("OK")
}

String historyToXml(ChatMessage[] chatHistory) {
    return AdminHandler.xstream.toXML(chatHistory.sort { m1, m2 -> m2.logDate - m1.logDate }.collect {
        [
                id             : it.id,
                action         : it.action.type,
                logDate        : new Date(it.logDate * 1000L),
                profileId      : it.profileId,
                profileStringId: it.profileStringId,
                profileName    : it.profileName,
                message        : it.message,
                params         : it.params,
        ]
    })
}

def getMercenariesDefs(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def mercenariesService = context.getBean(MercenariesService.class)
    def result = mercenariesService.mercenariesDefs().collect {
        [
                id      : it.id,
                level   : it.level,
                race    : it.race,
                armor   : it.armor,
                attack  : it.attack,
                hat     : it.hat,
                kit     : it.kit,
                skin    : it.skin,
                active  : it.active,
                backpack: it.backpack.sort { it.weaponId }.collect { it.weaponId + ":" + it.count }.join(" ")
        ]
    }.sort { it.id }
    AdminHandler.xstream.toXML(result)
}

def updateMercenaryDef(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def mercenariesService = context.getBean(MercenariesService.class)
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    def mercenaryDef = new MercenariesTeamMember()
    mercenaryDef.id = map.id as byte
    mercenaryDef.level = map.level as byte
    mercenaryDef.race = map.race as byte
    mercenaryDef.armor = map.armor as byte
    mercenaryDef.attack = map.attack as byte
    mercenaryDef.hat = map.hat as short
    mercenaryDef.kit = map.kit as short
    mercenaryDef.skin = map.skin as short
    mercenaryDef.active = map.active as boolean
    mercenaryDef.backpack = (map.backpack as String).split(" ").collect { it.split(":") }.collect { new BackpackItemShortStruct(it[0] as short, it[1] as short) }

    mercenariesService.updateMercenaryDef(mercenaryDef)

    AdminHandler.xstream.toXML("OK")
}
