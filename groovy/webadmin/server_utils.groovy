import com.google.common.reflect.ClassPath
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.pragmatix.app.common.AwardKindEnum
import com.pragmatix.app.common.ItemCheck
import com.pragmatix.app.common.PvpBattleResult
import com.pragmatix.app.common.Race
import com.pragmatix.app.domain.TrueSkillEntity
import com.pragmatix.app.init.UserProfileCreator
import com.pragmatix.app.messages.structures.GenericAwardStructure
import com.pragmatix.app.model.BackpackItem
import com.pragmatix.app.model.Stuff
import com.pragmatix.app.model.UserProfile
import com.pragmatix.app.model.Weapon
import com.pragmatix.app.model.group.FriendTeamMember
import com.pragmatix.app.model.group.TeamMember
import com.pragmatix.app.services.*
import com.pragmatix.app.services.rating.RankService
import com.pragmatix.app.services.social.vkontakte.WormixVkPaymentProcessor
import com.pragmatix.app.settings.HeroicMissionDailyProgress
import com.pragmatix.app.settings.HeroicMissionState
import com.pragmatix.app.settings.IItemRequirements
import com.pragmatix.clanserver.domain.Clan
import com.pragmatix.clanserver.domain.ClanMember
import com.pragmatix.clanserver.services.*
import com.pragmatix.clanserver.utils.WebadminLogger
import com.pragmatix.craft.model.StuffRecipe
import com.pragmatix.craft.services.CraftService
import com.pragmatix.gameapp.GameApp
import com.pragmatix.gameapp.cache.SoftCache
import com.pragmatix.gameapp.services.DailyScheduleService
import com.pragmatix.gameapp.sessions.Session
import com.pragmatix.gameapp.sessions.Sessions
import com.pragmatix.gameapp.social.SocialServiceEnum
import com.pragmatix.gameapp.social.service.VkontakteService
import com.pragmatix.intercom.service.AchieveServerAPI
import com.pragmatix.pvp.BattleWager
import com.pragmatix.pvp.services.PvpService
import com.pragmatix.pvp.services.matchmaking.BlackListService
import com.pragmatix.quest.QuestService
import com.pragmatix.server.Server
import com.pragmatix.webadmin.AdminHandler
import com.pragmatix.wormix.webadmin.interop.InteropSerializer
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest
import org.slf4j.Logger
import org.springframework.context.ApplicationContext
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.RowMapper
import org.springframework.transaction.support.TransactionCallbackWithoutResult
import org.springframework.transaction.support.TransactionTemplate

import java.beans.BeanInfo
import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.lang.reflect.Method
import java.sql.ResultSet
import java.sql.SQLException
import java.text.ParseException
import java.text.SimpleDateFormat

import static com.pragmatix.app.model.Weapon.WeaponType.COMPLEX

// переключить конфигурацию Героик боссов
def shiftHeroicMission(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def heroicMissionService = context.getBean(HeroicMissionService.class)
    heroicMissionService.getDailyTask().runServiceTask()
    Map<String, Object> resultMap = new LinkedHashMap<String, Object>()
    HeroicMissionState[] states = heroicMissionService.getHeroicMissionStates()
    for (int level = 0; level < HeroicMissionService.HeroicMissionDifficultyLevels; level++) {
        HeroicMissionState state = states[level]
        Map map = [currentMissionIds: state.currentMissionIds, currentMissionMap: state.currentMissionMap] as Map
        resultMap.put(level, map)
    }
    return new Gson().toJson(resultMap)
}

// задача, выполняемая в 00:00
def runDailyTask(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    DailyScheduleService dailyScheduleService = context.getBean(DailyScheduleService.class)
    dailyScheduleService.workhorseTask.run()
    return "OK"
}

// задача, выполняемая в 03:00
def runHeavyDailyTask(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    console.println("runHeavyDailyTask: start...")
    long start = System.currentTimeMillis();
    context.getBean(AppDailyScheduleService).runDailyTask()
    console.println("done in ${(double) (System.currentTimeMillis() - start) / 1000.0} sec.")
    def allAbandoned = context.getBean(UserRegistry).store.findResults { id, struct -> struct.isAbandonded ? id : null }.join(',')
    return "OK\nTotal abandoned profiles by now: ${allAbandoned}"
}

def closeCurrentSeason(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def seasonService = context.getBean(com.pragmatix.app.services.rating.SeasonService.class)
//    StringBuilder sb = new StringBuilder()
//    seasonService.closeSeason(new WebadminLogger(Server.sysLog, sb))
    seasonService.closeSeason()
    return "OK"
}

def getBlackListCandidats(ApplicationContext context, ExecScriptRequest request, PrintWriter console, Map<String, Object> params) {
    BlackListService blackListService = context.getBean(BlackListService.class)

    def resPairs = []
    def candidatsSet = [] as Set
    def loosersSet = [] as Set
    for (Map.Entry<Long, Map<Long, byte[]>> longMapEntry : blackListService.battlesResultsForUsers.entrySet()) {
        profileId = longMapEntry.getKey();
        for (Map.Entry<Long, byte[]> longEntry : longMapEntry.getValue().entrySet()) {
            Long opponentId = longEntry.getKey();
            int lostCount = 0;
            for (int i = 0; i < longEntry.getValue().length; i++) {
                byte battleResult = longEntry.getValue()[i];
                if (battleResult == PvpBattleResult.NOT_WINNER.getType()) {
                    lostCount++;
                }
            }
            if (lostCount >= 3) {
                resPairs.add([PvpService.getProfileId(profileId), PvpService.getProfileId(opponentId)])
                boolean addRes = candidatsSet.add(PvpService.getProfileId(opponentId))
                if (!addRes) {
                    loosersSet.add(PvpService.getProfileId(opponentId))
                }
            }
        }
    }

    String result = "blackListsForUsers:${blackListService.blackListsForUsers.size()}\n"
    result += "totalBattleResults:${blackListService.battlesResultsForUsers.size()}\n"
    result += "candidats (pair):${resPairs.size()}\n"
    result += "candidats:${candidatsSet.size()}\n"
    result += "loosers:${loosersSet.size()}\n\n"
    def losersMap = [:] as Map
    resPairs.findAll({ loosersSet.contains(it[1]) }).each {
        List opponents = losersMap.get(it[1])
        if (opponents == null) {
            opponents = []
        }
        opponents.add(it[0])
        losersMap.put(it[1], opponents)
    }
    losersMap = losersMap.sort({ o1, o2 -> o2.value.size() - o1.value.size() })
    losersMap.entrySet().each {
        result += "UPID{" + it.key + "} !=> "
        it.value.each { it2 ->
            result += "UPID{${it2}} "
        }
        result += "\n"
    }

    return result
}

def getBlackListTop(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def serializer = new InteropSerializer();
    Map<String, Object> params = serializer.fromString(request.scriptParams, Map.class);
    String order = params?.arg1 ?: "count"
    def blackListService = context.getBean(BlackListService.class)
    def softCache = context.getBean(SoftCache.class)
    def ratingService = context.getBean(RatingService.class)
    def dailyRegistry = context.getBean(DailyRegistry.class)
    String result = "blackListsForUsers:${blackListService.blackListsForUsers.size()}\n"
    result += "minSize=6  order by ${order} desc\n"
    blackListService.getBlackListsForUsers()
            .findAll { it.value.size() > 5 }
            .collectEntries {
                long profileId = PvpService.getProfileId(it.key as long)
                def profile = softCache.get(UserProfile.class, profileId, false)
                int yesterdayRating = 0
                try {
                    yesterdayRating = ratingService.getYesterdayRatingByWager().get(BattleWager.NO_WAGER).yesterdayRating.get(profileId) ?: 0
                } catch (Exception e) {
                }
                int todayRating = dailyRegistry.getDailyRating(profileId, BattleWager.NO_WAGER)

                [(it.key): [
                        count          : it.value.size(),
                        yesterdayRating: yesterdayRating,
                        todayRating    : todayRating,
                        level          : profile != null ? profile.level : 0,
                        rating         : profile != null ? profile.rating : 0,
                ]]
            }.sort { a, b ->
        switch (order) {
            case "yesterdayRating": return b.value.yesterdayRating - a.value.yesterdayRating
            case "rating": return b.value.rating - a.value.rating
            case "level": return b.value.level - a.value.level == 0 ? b.value.count - a.value.count : b.value.level - a.value.level
            default: return b.value.count - a.value.count
        }
    }.take(98)
            .each
                    {
                        long profileId = PvpService.getProfileId(it.key as long)
                        String profileName = getProfileName(context, profileId)
                        profileName = profileName.length() > 30 ? profileName.substring(0, 30) : profileName
                        result += String.format("<pre>%3s: %-30s %2s %10s%7s%8s UPID{%s}<pre>",
                                it.value.count, profileName, it.value.level, it.value.rating, "(" + it.value.todayRating + ")", "(" + it.value.yesterdayRating + ")", profileId)
                    }
    return result
}

def getPourDowners(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    RatingService ratingService = context.getBean(RatingService.class)
    ProfileService profileService = context.getBean(ProfileService.class)
    String result = ""
    def powerDowners = [:]
    BattleWager.values().each { wager ->
        powerDowners.putAll(ratingService.yesterdayRatingByWager.get(wager).yesterdayRating.findAll { it.value < -100 })
    }
    int last = 0
    powerDowners.sort { a, b -> a.value - b.value }.take(98).eachWithIndex { it, n ->
        long profileId = it.key as long
        UserProfile profile = profileService.getUserProfile(profileId)
        if (profile != null) {
            TrueSkillEntity skillEntity = profileService.getTrueSkillFor(profile)
            int trueSkillRating = Math.round((skillEntity.mean - skillEntity.standardDeviation * (double) 3) * (double) 500)
            String profileName = getProfileName(context, profileId)
            if (it.value > -500 && last <= -500) {
                result += "=="
            }
            result += String.format("<pre>%2s %5s %-30s %2s %6s %5s %7s  UPID{%s}<pre>", n + 1, it.value, profileName, profile.level, profile.rating, trueSkillRating, skillEntity.battles, profileId)
            last = it.value as int
        }
    }
    return result
}

def getTodayPourDowners(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    DailyRegistry dailyRegistry = context.getBean(DailyRegistry.class)
    ProfileService profileService = context.getBean(ProfileService.class)
    RatingService ratingService = context.getBean(RatingService.class)
    String result = ""
    def powerDowners = [:]
    dailyRegistry.store.each {
        int rating = dailyRegistry.getMinDailyRating(it.key)
        if (rating < -300) powerDowners.put(it.key, rating)
    }
    result += String.format("<pre>%2s %5s %-30s %2s %-6s %-5s %5s %7s", "", "", "", "", "rate", "yst.", "skill", "battles", "")
    int last = 0
    powerDowners.sort { a, b -> a.value - b.value }.take(98).eachWithIndex { it, n ->
        long profileId = it.key as long
        UserProfile profile = profileService.getUserProfile(profileId)
        TrueSkillEntity skillEntity = profileService.getTrueSkillFor(profile)
        int trueSkillRating = Math.round((skillEntity.mean - skillEntity.standardDeviation * (double) 3) * (double) 500)
        String profileName = getProfileName(context, profileId)
        Integer minYesterdayRating = getMinYesterdayMinRating(profileId, ratingService)
        if (it.value > -500 && last <= -500) {
            result += "=="
        }
        result += String.format("<pre>%2s %5s %-30s %2s %6s %5s %5s %7s  UPID{%s}<pre>",
                n + 1, it.value, profileName, profile.level, profile.rating, minYesterdayRating ?: "", trueSkillRating, skillEntity.battles, profileId)
        last = it.value as int
    }
    return result
}

def Integer getMinYesterdayMinRating(long profileId, RatingService ratingService) {
    def ratings = []
    BattleWager.values().each { wager ->
        def rating = ratingService.yesterdayRatingByWager.get(wager).yesterdayRating.get(profileId)
        if (rating != null) ratings.add(rating)
    }
    return ratings.size() > 0 ? ratings.min() as Integer : null
}

def setQuestData(ApplicationContext context, int questId, int profileId, String data) {
    def questService = context.getBean(QuestService.class)
    def profileService = context.getBean(ProfileService.class)
    def gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create()

    def profile = profileService.getUserProfile(profileId)
    def questEntity = questService.getQuestEntity(profile)
    if (data) {
        switch (questId) {
            case 1: questEntity.q1 = gson.fromJson(data, com.pragmatix.quest.quest01.Data.class); break
            case 2: questEntity.q2 = gson.fromJson(data, com.pragmatix.quest.quest02.Data.class); break
            case 3: questEntity.q3 = gson.fromJson(data, com.pragmatix.quest.quest03.Data.class); break
        }
    }
    gson.toJson([questEntity.q1, questEntity.q2, questEntity.q3][questId - 1])
}

def setBossPassStatus(ApplicationContext context, long profileId, boolean successedMission, boolean successedSuperBosMission) {
    def dailyRegistry = context.getBean(DailyRegistry.class)
    def dailyStructure = dailyRegistry.getDailyStructure(profileId);

    dailyStructure.successedMission = successedMission
    dailyStructure.successedSuperBossMission = successedSuperBosMission

    "${profileId}\n bossPassed = ${dailyRegistry.isSuccessedMission(profileId)}\n superBossPassed = ${dailyRegistry.isSuccessedSuperBossMission(profileId)}"
}

def setHeroicMissionDailyProgress(ApplicationContext context, int level, String data) {
    def heroicMissionService = context.getBean(HeroicMissionService.class)
    def gson = new Gson()

    if (data) {
        def map = gson.fromJson(data, Map.class)
        def progress = new HeroicMissionDailyProgress();
        progress.defeatCount = map.defeatCount as int
        progress.winners = map.winners.collect { it as Long } as Set
        heroicMissionService.heroicMissionDailyProgresses[level - 1] = progress
    }

    def progress = heroicMissionService.heroicMissionDailyProgresses[level - 1]
    gson.toJson([
            defeatCount: progress.getDefeatCount(),
            winners    : progress.winners,
    ])
}

def getHeroicMissionDailyProgress(ApplicationContext context) {
    def heroicMissionService = context.getBean(HeroicMissionService.class)
    def gson = new Gson()
    String out = ""
    out += "minWinThreshold: ${heroicMissionService.minWinThreshold}\n"
    out += "defeatContributionMoney: ${heroicMissionService.defeatContributionMoney}\n"
    out += "maxSingleAwardMoney: ${heroicMissionService.maxSingleAwardMoney}\n"
    int i = 1
    out += "\n" + heroicMissionService.heroicMissionDailyProgresses.collect { progress ->
        double defeatCount = progress.getDefeatCount()
        double winCount = progress.getWinCount()
        int awardMoney = 0
        int winPercent = 0
        if (winCount > 0 && defeatCount > 0) {
            winPercent = winCount * 100 / (defeatCount + winCount);
            if (winPercent < heroicMissionService.minWinThreshold) {
                awardMoney = Math.min(heroicMissionService.maxSingleAwardMoney, defeatCount * heroicMissionService.defeatContributionMoney / winCount);
            }
        }
        gson.toJson([
                level      : i++,
                defeatCount: defeatCount as int,
                winCount   : winCount as int,
                winPercent : winPercent,
                awardMoney : awardMoney,
        ])
    }.join("\n")
    out
}

def setProperty(ApplicationContext context, String className, String property, String newValue) {
    if (!className.contains(".")) {
        def loader = Thread.currentThread().getContextClassLoader()
        def classes = ClassPath.from(loader).getTopLevelClasses().findAll { it.simpleName == className } as List
        if (classes.size() > 1)
            throw new IllegalArgumentException("имя класса не уникально! Найдены кандидаты " + classes)
        className = classes[0].toString()
    }
    def bean = context.getBean(Class.forName(className))
    if (newValue != null && !newValue.trim().isEmpty()) {
        def oldValue = getPropertyValue(bean, property)
        String type
        String value
        if (newValue.contains(":")) {
            type = newValue.split(":")[1]
            value = newValue.split(":")[0]
        } else if (newValue == "true" || newValue == "false") {
            type = "bool"
            value = newValue
        } else {
            type = "String"
            value = newValue
        }
        switch (type) {
            case "String": bean[property] = value; break;
            case "int": bean[property] = value as int; break;
            case "bool": bean[property] = Boolean.valueOf(value); break;
            case "double": bean[property] = value as double; break;
        }
        return "${className}.${property}: ${oldValue} => ${getPropertyValue(bean, property)}"
    } else {
        return "${className}.${property}: ${bean[property]}"
    }
}

def Object getPropertyValue(Object bean, String property) {
    BeanInfo info = Introspector.getBeanInfo(bean.getClass(), Object.class);
    PropertyDescriptor[] props = info.getPropertyDescriptors();
    for (PropertyDescriptor pd : props) {
        String name = pd.getName();
        if (name == property) {
            Method getter = pd.getReadMethod();
            if (getter != null)
                return getter.invoke(bean);
            break;
        }
    }
    bean[property]
}

def setProperty2(ApplicationContext context, String className, String property, String newValue) {
    def bean = context.getBean(className)
    if (newValue != null && !newValue.trim().isEmpty()) {
        def oldValue = bean[property]
        String type
        String value
        if (newValue.contains(":")) {
            type = newValue.split(":")[1]
            value = newValue.split(":")[0]
        } else if (newValue == "true" || newValue == "false") {
            type = "bool"
            value = newValue
        } else {
            type = "String"
            value = newValue
        }
        switch (type) {
            case "String": bean[property] = value; break;
            case "int": bean[property] = value as int; break;
            case "bool": bean[property] = Boolean.valueOf(value); break;
            case "double": bean[property] = value as double; break;
        }
        toXML([
                result   : "${className}.${property}: ${oldValue} => ${bean[property]}" as String,
                className: className,
                property : property,
                oldValue : oldValue,
                value    : bean[property],
        ])
    } else {
        toXML([
                result   : "${className}.${property}: ${bean[property]}" as String,
                className: className,
                property : property,
                value    : bean[property],
        ])
    }
}

def toXML(Map resultMap) {
    AdminHandler.xstream.toXML(resultMap)
}

private String getProfileName(ApplicationContext context, long profileId) {
    PvpService pvpService = context.getBean(PvpService.class)
    ProfileService profileService = context.getBean(ProfileService.class)
    ClanServiceImpl clanService = context.getBean(ClanServiceImpl.class)
    byte socialNetId = (byte) profileService.getSocialIdForClan(profileService.getUserProfile(profileId))
    ClanMember member = clanService.getClanMember((short) socialNetId, (int) profileId)
    return member != null ? member.name : pvpService.userIdToNameMap.get(PvpService.getPvpUserId(profileId, socialNetId)) ?: ""
}

def discardClanServer(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ClanSeasonService clanSeasonService = context.getBean(ClanSeasonService.class)
    GameApp gameApp = context.getBean(GameApp.class)
    ClanService clanService = context.getBean(ClanService.class)
    SoftCache softCache = context.getBean(SoftCache.class)
    ClanRepoImpl clanRepo = context.getBean(ClanRepoImpl.class)

    StringBuilder sb = new StringBuilder()
    Logger logger = new WebadminLogger(Server.sysLog, sb)
    logger.info("Current season {}", clanSeasonService.currentSeason);

    clanSeasonService.rejectUpdateRating = true
    clanSeasonService.discard = true

    int i = 0;
    logger.info("close clan sessions ...");
    for (Session session : gameApp.getSessions()) {
        Object user = session.getUser();
        if (user instanceof ClanMember) {
            ClanMember clanMember = (ClanMember) user;
            //выполняем действия при выходе
            clanService.onLogout(clanMember, false);

            session.close();
            i++;

            if (clanMember.clan != null) {
                softCache.remove(Clan.class, clanMember.clan.id);
            }
        }
    }

    clanSeasonService.discardDAO = true

    clanRepo.getMemberCache().clear();
    logger.info("closed {} clan sessions", i);

    return sb.toString()
}

def checkMemberCache(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ClanSeasonService clanSeasonService = context.getBean(ClanSeasonService.class)
    GameApp gameApp = context.getBean(GameApp.class)
    ClanService clanService = context.getBean(ClanService.class)
    SoftCache softCache = context.getBean(SoftCache.class)
    ClanRepoImpl clanRepo = context.getBean(ClanRepoImpl.class)

    StringBuilder sb = new StringBuilder()
    Logger logger = new WebadminLogger(Server.sysLog, sb)

    int i = 0;
    for (Session session : gameApp.getSessions()) {
        Object user = session.getUser();
        if (user instanceof ClanMember) {
            i++;
        }
    }

    logger.info("clan sessions={}", i);
    logger.info("memberCache.size={}", clanRepo.getMemberCache().size());
    Set<Integer> set = new TreeSet<Integer>(clanRepo.getMemberCache().values())
    logger.info("memberCache:{}", set);

    for (Integer clanId : set) {
        Clan clan = softCache.get(Clan.class, clanId, false)
        logger.info(clan.toString());
    }

    return sb.toString()
}

def discardOffClanServer(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ClanSeasonService clanSeasonService = context.getBean(ClanSeasonService.class)

    clanSeasonService.discardDAO = false
    clanSeasonService.rejectUpdateRating = false
    clanSeasonService.discard = false

    return "OK"
}

def closeCurrentSeasonAndDiscard(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ClanSeasonService clanSeasonService = context.getBean(ClanSeasonService.class)
    StringBuilder sb = new StringBuilder()
    def log = new WebadminLogger(Server.sysLog, sb)
    clanSeasonService.closeCurrentSeason(log)

    clanSeasonService.discardDAO = true
    clanSeasonService.rejectUpdateRating = true
    clanSeasonService.discard = true

    log.warn("discard is TRUE")

    return sb.toString()
}

def closeCurrentClanSeason(ApplicationContext context) {
    ClanSeasonService clanSeasonService = context.getBean(ClanSeasonService.class)
    StringBuilder sb = new StringBuilder()
    def log = new WebadminLogger(Server.sysLog, sb)
    clanSeasonService.closeCurrentSeason(log)
    return sb.toString()
}

def bugFix(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ProfileService profileService = context.getBean(ProfileService.class)
    PermanentStuffService permanentStuffService = context.getBean(PermanentStuffService.class)
    StuffService stuffService = context.getBean(StuffService.class)
    TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class)
//    Stuff hat = stuffService.getHat((short) 1041)
    Stuff hat = stuffService.getHat((short) 1037)
    hat.temporal = true
    if (true) {
        return "OK"
    }
    List<String> lines = new File("data/profiles").readLines()
    StringBuilder sb = new StringBuilder()
    int i = 0
    int ok = 0
    int error = 0
    int offline = 0
    lines.each {
        i++
        String profileId = it.substring(1, it.length() - 1)
        if (profileId.contains(":")) {
            profileId = profileId.split(":")[1]
        }
        UserProfile profile = profileService.getUserProfile(profileId as Long)
        if (profile != null) {
//            sb.append("${i}/${lines.size()}: ${it} -> ${profileId}\n")

            boolean addResult = permanentStuffService.addStuff(profile, hat)
            sb.append(i + "/" + lines.size() + ": " + addResult + "\n")
            if (addResult) ok++ else error++
            if (addResult && !profile.isOnline()) {
                offline++
                profileService.updateSync(profile)
            }
        } else {
            sb.append(profileId + " not found\n")
        }
    }
    sb.append("ok=${ok} error=${error} offline=${offline}\n")
    return sb.toString()
}

def cleanSesonRatingForClan(ApplicationContext context, int clanId) {
    JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class)
    RatingServiceImpl ratingService = context.getBean(RatingServiceImpl.class)
    SoftCache softCache = context.getBean(SoftCache.class)

    softCache.remove(Clan.class, clanId);
    jdbcTemplate.update("UPDATE clan.clan SET season_rating = 0 WHERE id = ${clanId};\n" +
            "UPDATE clan.clan_member SET season_rating = 0 WHERE clan_id = ${clanId}")
    ratingService.reloadTop();

    return "" + clanId + " rating was cleaned."
}

def setClanSeasonAward(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ProfileBonusService.TOP_CLAN_AWARD_WEAPONS = [null, null, null] as double[][]

    //{{75.0, 0.8, 8.0}, {111.0, 0.4, 6.0}, {112.0, 0.4, 6.0}};
    ProfileBonusService.TOP_CLAN_AWARD_WEAPONS[0] = [75.0, 0.8, 8.0] as double[]
    ProfileBonusService.TOP_CLAN_AWARD_WEAPONS[1] = [111.0, 0.4, 6.0] as double[]
    ProfileBonusService.TOP_CLAN_AWARD_WEAPONS[2] = [112.0, 0.4, 6.0] as double[]

    Arrays.toString(ProfileBonusService.TOP_CLAN_AWARD_WEAPONS[0]) + ", " +
            Arrays.toString(ProfileBonusService.TOP_CLAN_AWARD_WEAPONS[1]) + ", " +
            Arrays.toString(ProfileBonusService.TOP_CLAN_AWARD_WEAPONS[2])
}

def clanDonateVkPriceFix(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def vkPaymentProcessor = context.getBean(WormixVkPaymentProcessor.class)
    String result = ""
    vkPaymentProcessor.priceByItem.put("41", [3, 34, 10, 26])
    vkPaymentProcessor.priceByItem.put("42", [3, 72, 20, 55])
    vkPaymentProcessor.priceByItem.put("43", [3, 182, 50, 140])
    vkPaymentProcessor.priceByItem.put("44", [3, 390, 100, 300])

    result += "" + vkPaymentProcessor.priceByItem.get("41") + "\n"
    result += "" + vkPaymentProcessor.priceByItem.get("42") + "\n"
    result += "" + vkPaymentProcessor.priceByItem.get("43") + "\n"
    result += "" + vkPaymentProcessor.priceByItem.get("44") + "\n"
    result
}

def resetDailyRegistry(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def dailyRegistry = context.getBean(DailyRegistry.class)
    dailyRegistry.store.clear()
    "OK"
}

def doWork(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def jdbcTemplate = context.getBean(JdbcTemplate.class)
    "прошли босса 116: \n" + jdbcTemplate.queryForList("SELECT id FROM wormswar.user_profile WHERE last_login_time > '2018-10-24' and current_new_mission = 116;", Long.class).collect { "UPID{$it}" }.join("\n")
}

def reconfigStuffRecipe(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def craftService = context.getBean(CraftService.class)
    /*
        <bean p:id="1660" p:resultStuffsMass="1661:25 1662:25 1663:25 1664:10 1665:5 1666:2" class="com.pragmatix.craft.model.StuffRecipe"
              p:needMoney="1800" p:needRealMoney="18" p:baseStuffs="1056 1058 1123" p:reagents="50:8"
              p:recraftMoney="6200" p:recraftRealMoney="62" p:recraftBaseStuffs="" p:recraftReagents="50:20"/>
     */
    def recipe1 = craftService.getStuffRecipes().get((short) 1660)
    recipe1.needMoney = 1800
    recipe1.needRealMoney = 18
    recipe1.setReagents("50:8")
    recipe1.recraftMoney = 6200
    recipe1.recraftRealMoney = 62
    recipe1.setRecraftReagents("50:20")
/*
        <bean p:id="1720" p:resultStuffsMass="1721:25 1722:25 1723:25 1724:10 1725:5 1726:2" class="com.pragmatix.craft.model.StuffRecipe"
              p:needMoney="2200" p:needRealMoney="22" p:baseStuffs="1041 1149 2048" p:reagents="50:10"
              p:recraftMoney="6000" p:recraftRealMoney="60" p:recraftBaseStuffs="" p:recraftReagents="50:16"/>
 */
    def recipe2 = craftService.getStuffRecipes().get((short) 1720)
    recipe2.needMoney = 2200
    recipe2.needRealMoney = 22
    recipe2.setReagents("50:10")
    recipe2.recraftMoney = 6000
    recipe2.recraftRealMoney = 60
    recipe2.setRecraftReagents("50:16")

    "" + print(recipe1) + "\n" + print(recipe2)
}

def String print(StuffRecipe recipe) {
    "StuffRecipe{" +
            "id=" + recipe.id +
            ", needMoney=" + recipe.needMoney +
            ", needRealMoney=" + recipe.needRealMoney +
            ", reagents=" + recipe.reagents +
            ", recraftMoney=" + recipe.recraftMoney +
            ", recraftRealMoney=" + recipe.recraftRealMoney +
            ", recraftReagents=" + recipe.recraftReagents +
            '}';
}

def issueFixDb(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def jdbcTemplate = context.getBean(JdbcTemplate.class)
    def profileService = context.getBean(ProfileService.class)
    def profileBonusService = context.getBean(ProfileBonusService.class)
    def weaponService = context.getBean(WeaponService.class)
    def craftService = context.getBean(CraftService.class)
    def gameApp = context.getBean(GameApp.class)
    def log = Server.sysLog

    String sql = """
select profile_id from wormswar.user_profile UP
inner join wormswar.worm_groups WG on id = profile_id
where last_login_time > '2018-05-29 06:22:04' and (
    team_member_1 > 0 and team_member_1 != profile_id
    or team_member_2 > 0 and team_member_2 != profile_id
    or team_member_3 > 0 and team_member_3 != profile_id
    or team_member_4 > 0 and team_member_4 != profile_id
    ) order by profile_id --limit 100
    """
//    log.info(sql + " ...");
    int i = 0
    int online = 0
    String out = ""
    jdbcTemplate.query(sql, { ResultSet res ->
        def profile = profileService.getUserProfile(res.getInt("profile_id"))
        def friends = profile.teamMembers.findAll { it instanceof FriendTeamMember }.collect { it as FriendTeamMember }.findAll { it.armor != it.attack }
        Set<Integer> ss = [] as Set
        if (friends) {
//            i++
//            out += "${i}: [${profile.id}] ${friends}\n"
            try {
                def oldTeamMap = jdbcTemplate.queryForMap("select * from tmp.worm_groups_old where profile_id = ?", profile.getId())
                def oldTeam = (0..3).collect { index -> TeamMember.newTeamMember(acceptTeamMemberMeta(oldTeamMap, index)) }
                for (int j = 0; j < profile.teamMembers.length; j++) {
                    if (profile.teamMembers[j] instanceof FriendTeamMember) {
                        def friend = profile.teamMembers[j] as FriendTeamMember
                        def oldFriendId = acceptTeamMember(oldTeamMap, j)
                        if (friend.armor != friend.attack) {
                            if (profile.wormsGroup[j] == oldFriendId) {
                                def oldFriend = oldTeam.get(j) as FriendTeamMember
                                int level = (oldFriend.attack + oldFriend.armor) / 2;
                                byte friendTeamMemberAttack = (byte) Math.round(oldFriend.attack * 60 / (2 * level));
                                byte friendTeamMemberArmor = (byte) (60 - friendTeamMemberAttack);

                                if (friend.armor == friendTeamMemberAttack && (oldFriend.attack + oldFriend.armor < 60)) {
                                    if (ss.add(profile.id.intValue())) {
                                        i++
                                        //out += "${i}: [${profile.id}] ${friends}\n"
                                    }
//                               profile.teamMembers[j] = new FriendTeamMember(friend, friendTeamMemberAttack, friendTeamMemberArmor);
//                               profile.setTeamMembersDirty(true);

                                    String msg = "${i}) [${profile.id}.${oldFriendId}] Attack ${friend.attack} => ${friendTeamMemberAttack}(${oldFriend.attack})   Armor ${friend.armor} => ${friendTeamMemberArmor} (${oldFriend.armor})"
//                               log.info(msg)
                                    out += msg + "\n"
                                }
                            } else if (oldTeam.get(j) instanceof FriendTeamMember) {
                                out += "${friend} != ${oldTeam.get(j)}\n"
                            }
                        }
                    }
                }
                if (profile.isTeamMembersDirty()) {
//                    UserProfileStructure userProfileStructure = profile.getUserProfileStructure();
//                    if(userProfileStructure != null) {
//                        userProfileStructure.wormsGroup = profileService.createWormGroupStructures(profile);
//                    }
//                    profileService.updateSync(profile)
                    if (profile.online) {
                        online++
//                       def session = gameApp.getSessions().get(profile)
//                        if(session){
//                            session.close()
//                        }
                    }
                }
            } catch (EmptyResultDataAccessException e) {
            }
        }
    } as RowCallbackHandler)
//    log.info("done. updated {} profiles", i)
    out += "done. updated=${i}\n"
    out += "online=" + online
    return out
}


public byte[] acceptTeamMemberMeta(Map<String, Object> map, int index) {
    switch (index) {
        case 0:
            return map.team_member_meta_1;
        case 1:
            return map.team_member_meta_2;
        case 2:
            return map.team_member_meta_3;
        case 3:
            return map.team_member_meta_4;
    }
    return map.team_member_meta_1;
}

public Integer acceptTeamMember(Map<String, Object> map, int index) {
    switch (index) {
        case 0:
            return map.team_member_1;
        case 1:
            return map.team_member_2;
        case 2:
            return map.team_member_3;
        case 3:
            return map.team_member_4;
    }
    return map.team_member_1;
}

def issueFixFile(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def profileService = context.getBean(ProfileService.class)
    def log = Server.sysLog

    def file = new File("logs/cdr/events/LEVEL_UP")

    String out = "read file [$file] ...\n"
    int i = 0
    def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")
    def sdf0 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    Map<Long, LevelDate> map = new TreeMap<>()
    file.readLines().collect { it.replaceAll("[\\[\\]\"]", "").split(",") }.forEach { ss ->
        Date date = null
        try {
            date = sdf.parse(ss[0])
        } catch (ParseException e) {
            date = sdf0.parse(ss[0])
        }
        long profileId = ss[1] as long
        int level = 0
        try {
            level = ss[2] as int
        } catch (NumberFormatException e) {
        }
        if (level > 0) {
            def value = map.get(profileId)
            if (value == null || level > value.level) {
                map.put(profileId, new LevelDate(level, date))
            }
        }
//        out += "${profile} -> ${money}\n"
//        log.info("[{}] выдаём [{}] фузов (корректировка)", profile, money)
//        GenericAward award = GenericAward.builder().addMoney(money).build()
//        profileBonusService.awardProfile(award, profile, AwardTypeEnum.ACTION, "note", "корректировка (не начислилась доп. награда за супербоссов)");
//        profileService.updateAsync(profile)
//        i++
    }
    log.info("map.size is {}", map.size())
    map.entrySet().forEach { entry ->
        def profile = profileService.getUserProfile(entry.key)
        if (profile != null && profile.levelUpTime == null && profile.level == entry.value.level) {
            i++
            profile.setLevelUpTime(entry.value.date)
            profileService.updateSync(profile)
            log.info("$i: [$profile.id] level=${profile.level} levelUpTime -> ${sdf.format(profile.levelUpTime)}")
        }
    }
    out += "map.size is ${map.size()}\n"
    out += "set levelUpTime for ${i} profiles\n"
    out += "done."
    return out
}

class LevelDate {
    int level
    Date date

    LevelDate(int level, Date date) {
        this.level = level
        this.date = date
    }
}

def issueFixCmd(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def profileService = context.getBean(ProfileService.class)
    def paymentService = context.getBean(PaymentService.class)
    def bundleService = context.getBean(BundleService.class)
    def log = Server.sysLog

    def gson = new Gson()

    String out = "\n"
    int i = 0
    def cmd = """ grep PAYMENT logs/cdr/events/events.log | jq -c 'select(.eventType=="BUNDLE") | {date,profileId,bundleCode}' | sort """
    def command = ["bash", "-c", cmd]
    def proc = command.execute()
    def source = proc.in.text
/*
{"date":"2018-01-15 00:13:31.977","profileId":74433405,"bundleCode":"812"}
{"date":"2018-01-15 00:37:26.86","profileId":311318675,"bundleCode":"806"}
{"date":"2018-01-15 08:14:34.713","profileId":266684622,"bundleCode":"108"}
{"date":"2018-01-15 09:07:44.772","profileId":2063388,"bundleCode":"108"}
{"date":"2018-01-15 09:51:22.582","profileId":153913558,"bundleCode":"108"}
{"date":"2018-01-15 10:14:58.328","profileId":399755158,"bundleCode":"806"}
 */
    def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")
    source.split("\\n").collect { gson.fromJson(it, Map.class) }.findAll { sdf.parse(it.date).before(sdf.parse("2018-01-15 12:20:00.0")) }.forEach { map ->
        def date = sdf.parse(map.date)

        def validBundle = bundleService.getValidBundle(map.bundleCode)
        def profile = profileService.getUserProfile(map.profileId as int)
        (items, realMoney) = issueBundle(context, profile, validBundle.items)

        if (items.size() > 0) {
            i++
            out += "${i}) [${map.profileId as int}] ${map.bundleCode} -> ${items} ${realMoney}\n"
            log.info("${i}) [${map.profileId as int}] ${map.bundleCode} -> ${items} ${realMoney}")

//            bundleService.issueBundle(profile, validBundle)
//            profileService.updateSync(profile)
        }
    }
    out += "done"
    return out
}


def issueBundle(ApplicationContext context, UserProfile profile, GenericAwardStructure[] awards) {
    def weaponService = context.getBean(WeaponService.class)
    def stuffService = context.getBean(StuffService.class)

    List<GenericAwardStructure> items = new ArrayList<>();
    int realMoney = 0;
    for (GenericAwardStructure award : awards) {
        switch (award.awardKind) {
            case AwardKindEnum.WEAPON:
            case AwardKindEnum.WEAPON_SHOT:
                int weaponId = award.itemId;
                Weapon weapon = weaponService.getWeapon(weaponId);
                if (weapon != null) {
                    if ((weapon.isSeasonal() || weapon.isConsumable()) && award.count > 0) {
//                        new AddWeaponShotEvent(weaponId, award.count, -1, weaponService).runEvent(profile);
                        items.add(award);
                    } else {
                        if (!weaponService.isPresentInfinitely(profile, weaponId)) {
                            BackpackItem backpackItem = profile.getBackpackItemByWeaponId(weaponId);
                            if (backpackItem != null && weapon.isType(COMPLEX) && backpackItem.getCount() != 0 && ItemCheck.hasRealPrice(weapon)) {
                                int realPrice = weapon.getRealprice() * Math.min(weapon.getInfiniteCount() - 1, weapon.complexWeaponLevel(backpackItem.getCount()));
//                                new AddRealMoneyEvent(realPrice).runEvent(profile);
                                realMoney += realPrice;
                            }
//                            new AddWeaponEvent(weaponId, weaponService).runEvent(profile);
                            items.add(award);
                        } else {
                            if (ItemCheck.hasRealPrice(weapon)) {
                                int realPrice = weapon.getRealprice() * Math.max(1, weapon.getInfiniteCount());
//                                new AddRealMoneyEvent(realPrice).runEvent(profile);
                                realMoney += realPrice;
                            }
                        }
                    }
                }
                break;
            case AwardKindEnum.TEMPORARY_STUFF:
            case AwardKindEnum.STUFF:
                short stuffId = (short) award.itemId;
                Stuff stuff = stuffService.getStuff(stuffId);
                if (stuff != null) {
                    if (stuff.isTemporal()) {
                        if (stuff.isBoost()) {
//                            new AddBoosterEvent(stuffId, stuffService).runEvent(profile);
                        } else {
                            if (stuff.needLevel() > profile.getLevel()) {
                                log.error("[{}] предмет из Bundle не доступен по уровню! {} {} > {}", profile, stuff, stuff.needLevel(), profile.getLevel());
                                return [Collections.emptyList(), realMoney]
                            }
//                            new AddTemporalStuffEvent(stuffId, 0, 0, true, stuffService).runEvent(profile);
                        }
                        items.add(award);
                    } else {
                        boolean setStuff = !stuff.isSticker();
                        if (!stuffService.isExist(profile, stuffId)) {
//                            new AddStuffEvent(stuffId, stuffService, setStuff).runEvent(profile);
                            items.add(award);
                        } else {
                            if (ItemCheck.hasRealPrice(stuff)) {
                                int realPrice = stuff.getRealprice();
//                                new AddRealMoneyEvent(realPrice).runEvent(profile);
                                realMoney += realPrice;
                            }
                        }
                    }
                }
                break;
            case AwardKindEnum.RACE:
                Race race = Race.valueOf(award.itemId);
                if (race != null) {
                    if (!RaceService.hasRace(profile, race)) {
//                        new AddRaceEvent(race).runEvent(profile);
                        items.add(award);
                    } else {
                        IItemRequirements itemRequirements = racePriceSettings.getPriceMap().get(race.type);
                        if (itemRequirements != null && ItemCheck.hasRealPrice(itemRequirements)) {
                            int realPrice = itemRequirements.needRealMoney();
//                            new AddRealMoneyEvent(realPrice).runEvent(profile);
                            realMoney += realPrice;
                        }
                    }
                }
                break;
        }
    }
    return [items, realMoney]
}

def getUserSubscription(ApplicationContext context, int profileId, int subscriptionId) {
    def vkontakteService = context.getBean(VkontakteService.class)
    def sub = vkontakteService.getUserSubscription(profileId, subscriptionId)
    "${sub}"
}

def getUserSubscriptions(ApplicationContext context, int profileId) {
    def vkontakteService = context.getBean(VkontakteService.class)
    def subs = vkontakteService.getUserSubscriptions(profileId)
    "${subs.items.collect { it.toString() }.join("\n")}"
}

def wipeAll(ApplicationContext context) {
    InteropSerializer serializer = new InteropSerializer()
    def achieveServerAPI = context.getBean(AchieveServerAPI.class);
    def profileService = context.getBean(ProfileService.class);
    def profileCreator = context.getBean(UserProfileCreator.class);
    def daoService = context.getBean(DaoService.class);
    def out = ""
    daoService.getUserProfileDao().allList.sort { it.id }.collect { profileService.getUserProfile(it.id) }.each { profile ->
        try {
            // обнуляем достижения и статистику
            def stringId = profileService.getProfileAchieveId(profile.getId())
            achieveServerAPI.wipeAchievements(stringId);

            final Session session = Sessions.get(profile);
            // проверяем в online ли он
            if (session != null) {
                session.close();
                Thread.sleep(300)
            }
            profileCreator.wipeUserProfile(profile);
            out += "[${profile}] wiped \n"
        } catch (Exception e) {
            out += "[${profile}] ${e.toString()} \n"
        }
    }
    "${subs.items.collect { it.toString() }.join("\n")}"
}

def cloneProfile(ApplicationContext context, int destProfileId, int sourceNetId, int sourceProfile) {
    def cloneProfileService = context.getBean(CloneProfileService.class)
    def profileService = context.getBean(ProfileService.class)

    def destProfile = profileService.getUserProfile(destProfileId)
    destProfile.setSocialId(profileService.getShortSocialIdFor(destProfile) as byte)

    def sourceNet = SocialServiceEnum.valueOf(sourceNetId)
    def result = cloneProfileService.cloneProfile(destProfile, sourceNet, (long) sourceProfile, true, cloneProfileService.masterSecureToken)

    destProfile.setRankPoints(0)
    destProfile.setBestRank(RankService.INIT_RANK_VALUE)
    profileService.updateSync(destProfile)

    "[${result ? "OK" : "FAILURE"}] Clone ${sourceNet}:${sourceProfile} => ${destProfile}"
}

def setNetworkDefeatModifier(ApplicationContext context, String netName, Double newValue) {
    def rankService = context.getBean(RankService.class)
    if (netName == null || netName.isEmpty() || newValue == null)
        return getNetworkDefeatModifiers(rankService)
    def network = SocialServiceEnum.valueOf(netName)
    def oldValue = rankService.networkDefeatModifiers.getOrDefault(network, 1.0)
    rankService.networkDefeatModifiers.put(network, newValue)
    "${network.name()}: ${oldValue} => ${newValue} \n===============\n" + getNetworkDefeatModifiers(rankService)
}

def getNetworkDefeatModifiers(RankService rankService) {
    def result = ""
    rankService.networkDefeatModifiers.entrySet().forEach { it -> result += it.key.name() + " => " + it.value + "\n" }
    result
}

private String getValue(Map<String, String> params, String valueName) {
    return params.values().find { it.startsWith(valueName + "=") }.replaceFirst(valueName + "=", "")
}

def cleanBackpackFix(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def jdbcTemplate = context.getBean(JdbcTemplate.class)
    def transactionTemplate = context.getBean(TransactionTemplate.class)
    def profileService = context.getBean(ProfileService.class)
    def weaponService = context.getBean(WeaponService.class)
    def log = Server.sysLog
    String sql = """
            select profile_id, item_id from wormswar.shop_statistic where date > '2016-03-18 04:00' and date < '2016-03-18 14:15:27' and item_type = 0 order by id desc
        """
    log.info(sql + " ...");
    List<int[]> result = jdbcTemplate.query(sql, new RowMapper<int[]>() {
        @Override
        public int[] mapRow(ResultSet rs, int rowNum) throws SQLException {
            return [rs.getInt("profile_id"), rs.getInt("item_id")] as int[];
        }
    });
    int i = 0
    for (int[] arr : result) {
        def profile = profileService.getUserProfile(arr[0])
        int weaponId = arr[1]
        def addResult = weaponService.addOrUpdateWeapon(profile, weaponId, -1)
        log.info("{} add weapon {} -> {}", profile.id, weaponId, addResult)
        transactionTemplate.execute({ status -> profileService.update(profile) } as TransactionCallbackWithoutResult);
        if (addResult) i++
    }
    log.info("done.")

    return "Add Weapon. Success:" + i
}

def setSkillRaw(ApplicationContext context, int profileId, double mean, double standardDeviation) {
    def skill = TrueSkillService.trueSkillRating(mean, standardDeviation)
    if (skill <= 0 || skill > 1000) {
        return "ERROR: invalid skill value ($skill) for mean: $mean and deviation: $standardDeviation"
    }
    def profileService = context.getBean(ProfileService.class)
    def profile = profileService.getUserProfile(profileId)
    if (!profile) {
        return "ERROR: profile not fount by id! [$profileId]"
    }
    def trueSkill = profileService.getTrueSkillFor(profile)
    String out = "profile: UPID{$profileId}\n\n"
    out += "current skill: [${TrueSkillService.trueSkillRating(trueSkill.mean, trueSkill.standardDeviation)}] μ=${trueSkill.mean}, σ=${trueSkill.standardDeviation} \n\n"

    trueSkill.mean = mean
    trueSkill.standardDeviation = standardDeviation
    trueSkill.setDirty(true)

    out += "new skill: [${TrueSkillService.trueSkillRating(trueSkill.mean, trueSkill.standardDeviation)}] μ=${trueSkill.mean}, σ=${trueSkill.standardDeviation} \n"

    profileService.updateSync(profile)

    out
}

def callMethod(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer();
    Map<String, Object> params = serializer.fromString(request.scriptParams, Map.class);
    String method = params.get("method");
    switch (method) {
        case "runDailyTask": return runDailyTask(context, request, console)
        case "runHeavyDailyTask": return runHeavyDailyTask(context, request, console)
        case "shiftHeroicMission": return shiftHeroicMission(context, request, console)
        case "closeCurrentSeason": return closeCurrentSeason(context, request, console)
        case "getBlackListCandidats": return getBlackListCandidats(context, request, console, params)
        case "getBlackListTop": return getBlackListTop(context, request, console)
        case "getPourDowners": return getPourDowners(context, request, console)
        case "getTodayPourDowners": return getTodayPourDowners(context, request, console)
        case "let": return setProperty(context, "" + params.arg1, "" + params.arg2, params.arg3 as String)
        case "setProperty": return setProperty(context, "" + params.arg1, "" + params.arg2, params.arg3 as String)
        case "setProperty2": return setProperty2(context, "" + params.arg1, "" + params.arg2, params.arg3 as String)
        case "cleanSesonRatingForClan": return cleanSesonRatingForClan(context, params.arg1 as int)

        case "discardOffClanServer": return discardOffClanServer(context, request, console)
        case "checkMemberCache": return checkMemberCache(context, request, console)
        case "discardClanServer": return discardClanServer(context, request, console)
        case "closeCurrentClanSeason": return closeCurrentClanSeason(context)
        case "closeCurrentSeasonAndDiscard": return closeCurrentSeasonAndDiscard(context, request, console)
        case "setClanSeasonAward": return setClanSeasonAward(context, request, console)

        case "doWork": return doWork(context, request, console)
        case "resetDailyRegistry": return resetDailyRegistry(context, request, console)
//        case "issueFix": return issueFixDb(context, request, console)
        case "clanDonateVkPriceFix": return clanDonateVkPriceFix(context, request, console)
        case "cloneProfile": return cloneProfile(context, params.arg1 as Integer, params.arg2 as Integer, params.arg3 as Integer)
        case "setNetworkDefeatModifier": return setNetworkDefeatModifier(context, params.arg1 as String, params.arg2 as Double)
        case "getUserSubscription": return getUserSubscription(context, params.arg1 as Integer, params.arg2 as Integer)
        case "getUserSubscriptions": return getUserSubscriptions(context, params.arg1 as Integer)

        case "setBossPassStatus": return setBossPassStatus(context, params.arg1 as long, params.arg2 as int == 1, params.arg3 as int == 1)
        case "setQuestData": return setQuestData(context, params.arg1 as int, params.arg2 as int, params.arg3 as String)
        case "setHeroicMissionDailyProgress": return setHeroicMissionDailyProgress(context, params.arg1 as int, params.arg2 as String)
        case "getHeroicMissionDailyProgress": return getHeroicMissionDailyProgress(context)
        case "setSkillRaw": return setSkillRaw(context, params.arg1 as int, params.arg2 as double, params.arg3 as double)

        default: return "Method '${method}' not found!"
    }
}