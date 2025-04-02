import com.fasterxml.jackson.databind.ObjectMapper
import com.pragmatix.achieve.domain.WormixAchievements
import com.pragmatix.achieve.services.AchieveCommandService
import com.pragmatix.app.model.RestrictionItem
import com.pragmatix.app.model.UserProfile
import com.pragmatix.app.services.BanService
import com.pragmatix.app.services.DailyRegistry
import com.pragmatix.app.services.ProfileService
import com.pragmatix.app.services.RestrictionService
import com.pragmatix.clanserver.domain.ClanMember
import com.pragmatix.clanserver.services.ClanServiceImpl
import com.pragmatix.gameapp.GameApp
import com.pragmatix.gameapp.cache.SoftCache
import com.pragmatix.pvp.BattleWager
import com.pragmatix.pvp.services.PvpService
import com.pragmatix.webadmin.AdminHandler
import com.pragmatix.wormix.webadmin.interop.CommonResponse
import com.pragmatix.wormix.webadmin.interop.InteropSerializer
import com.pragmatix.wormix.webadmin.interop.ServiceResult
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest
import com.pragmatix.wormix.webadmin.interop.response.structure.BanType
import com.pragmatix.wormix.webadmin.interop.response.structure.clan.Rank
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate

import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Timestamp

private String getProfileName(ApplicationContext context, long profileId, byte socialNetId, String defaultName) {
    PvpService pvpService = context.getBean(PvpService.class)
    ClanServiceImpl clanService = context.getBean(ClanServiceImpl.class)
    ClanMember member = clanService.getClanMember((short) socialNetId, (int) profileId)
    return member != null ? member.name : pvpService.userIdToNameMap.get(PvpService.getPvpUserId(profileId, socialNetId)) ?: defaultName
}

def batchView(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> params = serializer.fromString(request.scriptParams, Map.class)
    String[] profiles = (params.get("profiles") as String).split(" ")
    Map<String, int[]> pvpTotalMap = new HashMap<>()
    File file = new File("logs/cdr/var/pvp_total")
    if (file.exists()) {
        file.eachLine { String line ->
            String[] ss = line.split(" ")
            int w = ss[0] as int
            int l = ss[1] as int
            int dr = ss[2] as int
            int d = ss[3] as int
            int t = w + l + dr + d

            pvpTotalMap.put(ss[4], [t, w, l, dr, d] as int[])
        }
    }
    def result = fillProfiles(context, profiles.toList(), pvpTotalMap, params.serverId as int)
    return AdminHandler.xstream.toXML(result)
}

def desynchView(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    String[] params = request.scriptParams.split(" ")
    String date = params[0] == "" ? "" : "-" + params[0]
    int lowLimit = params[1] as int
    Map<String, int[]> pvpTotalMap = [:]
    File file = new File("logs/cdr/var/pvp_total" + date)
    if (file.exists()) {
        file.eachLine { String line ->
            String[] ss = line.split(" ")
            int w = ss[0] as int
            int l = ss[1] as int
            int dr = ss[2] as int
            int d = ss[3] as int
            int t = w + l + dr + d
            int ds = Math.round(d * 100 / t)
            if (lowLimit >= 3) {
                if (d >= lowLimit)
                    pvpTotalMap.put(ss[4], [t, w, l, dr, d] as int[])
            } else {
                if (d >= 20 ||
                        (ds >= 30 && ds < 45 && d >= 6) ||
                        (ds >= 45 && d >= 4))
                    pvpTotalMap.put(ss[4], [t, w, l, dr, d] as int[])
            }
        }
    }
    AdminHandler.xstream.toXML(pvpTotalMap)
}

def desynchViewFillProfiles(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    int socialId = context.getBean(ProfileService.class).getDefaultSocialId().type
    Map<String, int[]> pvpTotalMap = (AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, int[]>).findAll { it.key.split(":")[0] as int == socialId }
    List<String> profiles = pvpTotalMap.keySet().toList()
    return AdminHandler.xstream.toXML(fillProfiles(context, profiles, pvpTotalMap))
}

private List<LinkedHashMap<String, Serializable>> fillProfiles(ApplicationContext context, List<String> profiles, Map<String, int[]> pvpTotalMap, int serverId) {
    def profileService = context.getBean(ProfileService.class)
    def banService = context.getBean(BanService.class)
    def dailyRegistry = context.getBean(DailyRegistry.class)
    def jdbcTemplate = context.getBean(JdbcTemplate.class)
    def softCache = context.getBean(SoftCache.class)
    def achieveCommandService = context.getBean(AchieveCommandService.class)
    def achievementService = achieveCommandService.getService(new WormixAchievements(""))

    def result = profiles.collect { it.split(":") }.collect {
        UserProfile profile
        int socialNetId
        if (it.length < 2) {
            profile = profileService.getUserProfile(it[0])
            socialNetId = serverId
        } else {
            socialNetId = it[0] as int
            profile = profileService.getUserProfile(it[1] as int)
        }
        if (profile) {
//            TrueSkillEntity skillEntity = profileService.getTrueSkillFor(profile)
            def userProfileStructure = profileService.getUserProfileStructure(profile)
            def registerDate = null
            try {
                registerDate = jdbcTemplate.queryForObject("SELECT creation_date FROM wormswar.creation_date WHERE id = ?", Date.class, profile.id)
            } catch (Exception e) {
            }
            String bans = ""
            def daysInBan = 0
            try {
                def res = jdbcTemplate.queryForMap("SELECT " +
                        " array_to_string(array_agg(type), ', ') AS bans, " +
                        " to_char(sum(CASE WHEN end_date IS NULL THEN now() ELSE end_date END- date), 'DD') AS daysInBan " +
                        "FROM wormswar.ban_list WHERE profile_id = ?", profile.id)
                if (res != null) {
                    def bx = (res.bans as String).replace(" ", "").split(",").collect { it as int } as TreeSet
                    bx.remove(62)
                    bans = bx.join(", ")
                    daysInBan = res.daysInBan
                }
            } catch (Exception e) {
            }
            int[] pvpTotal = pvpTotalMap.get(socialNetId + ":" + profile.id)
            int desynch = 0
            if (pvpTotal) {
                desynch = Math.round(pvpTotal[4] * 100 / pvpTotal[0])
            }
            def achieveId = profileService.getProfileAchieveId(profile.id)
            def profileAchievements = softCache.get(WormixAchievements.class, achieveId)
            def achievePoints = profileAchievements ? achievementService.countAchievePoints(profileAchievements) : 0

            def dailyRatings = dailyRegistry.getDailyRatings(profile.id) ?: new int[BattleWager.values().length] as List
            [
                    profileId       : profile.id,
                    socialNetId     : socialNetId,
                    profileName     : profile.name,
                    socialName      : getProfileName(context, profile.id, profileService.getSocialIdForClan(profile) as byte, ""),
                    level           : profile.level,
                    money           : profile.money,
                    realMoney       : profile.realMoney,
                    rating          : profile.rating,
                    dr_NO_WAGER     : dailyRatings[BattleWager.NO_WAGER.ordinal()],
                    dr_WAGER_15_DUEL: dailyRatings[BattleWager.WAGER_15_DUEL.ordinal()],
                    dr_WAGER_50_DUEL: dailyRatings[BattleWager.WAGER_50_DUEL.ordinal()],
                    dr_WAGER_50_2x2 : dailyRatings[BattleWager.WAGER_50_2x2.ordinal()],
                    banned          : banService.isBanned(profile.id),
                    online          : profile.online,
//                    trueSkillMean     : skillEntity.mean,
//                    trueSkillDeviation: skillEntity.standardDeviation,
//                    trueSkillRating   : Math.round((skillEntity.mean - skillEntity.standardDeviation * (double) 3) * (double) 500),
//                    pvpBattles        : skillEntity.battles,
                    registerDate    : registerDate,
                    lastLoginTime   : profile.lastLoginTime ? new Timestamp(profile.lastLoginTime.getTime()) : null,
                    clanId          : userProfileStructure.clanMember?.clanId,
                    clanName        : userProfileStructure.clanMember?.clanName,
                    rank            : userProfileStructure.clanMember ? Rank.valueOf(userProfileStructure.clanMember.rank.name()) : null,
                    bans            : bans,
                    daysInBan       : daysInBan,
                    desynch         : desynch,
                    pvpTotal        : pvpTotal,
                    achievePoints   : achievePoints,
            ]
        } else {
            [
                    profileId: it.join(":")
            ]
        }
    }
    return result
}

private List<LinkedHashMap<String, Serializable>> fillProfilesCompact(ApplicationContext context, Collection<Object> profileIds) {
    def profileService = context.getBean(ProfileService.class)
    def banService = context.getBean(BanService.class)

    def profiles = profileIds.collect { profileService.getUserProfile(it) }.findAll { it != null }

    return profiles.collect {
        [
                profileId  : it.id,
                profileName: it.name,
                socialName : getProfileName(context, it.id, profileService.getSocialIdForClan(it) as byte, null), // социальное имя берётся из кланов/pvp (если есть)
                level      : it.level,
                rating     : it.rating,
                money      : it.money,
                realMoney  : it.realMoney,
                online     : it.online,
                lastLogin  : it.lastLoginTime,
                banned     : banService.isBanned(it.id),
        ]
    }
}

def batchSearch(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class)
    GameApp gameApp = context.getBean(GameApp.class)
    Map<String, Object> params = AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, Object>

    String searchPhrase = params.searchPhrase as String ?: ""
    int limit = (params.limit ?: 25) as Integer
    int offset = (params.offset ?: 0) as Integer

    console.println("Searching for name=='${searchPhrase}' (limit ${limit} offset ${offset})...")

    gameApp.sessions.each { session ->
        def user = session.user
        if (user instanceof UserProfile) {
            final def userProfile = user as UserProfile
            if (userProfile.name && userProfile.name.toLowerCase().contains(searchPhrase.toLowerCase())) {
                profileService.updateSync(userProfile)
            }
        }
    }
    String sqlPattern = '%' + searchPhrase.toLowerCase().replaceAll('[%_]', '\\\\$0') + '%'
    Set<Long> userIds = new TreeSet<>(jdbcTemplate.queryForList("SELECT id FROM wormswar.user_profile WHERE LOWER(name) LIKE ? order by id LIMIT ? OFFSET ?", Long.class,
            sqlPattern, limit, offset))
    long total = jdbcTemplate.queryForObject("SELECT count(*) FROM wormswar.user_profile WHERE LOWER(name) LIKE ?", Long.class, sqlPattern)

    console.println("Found ${userIds?.size()} users")
    if (params.searchInClans) {
        console.println("Searching in _clans_ for social name=='${searchPhrase}' (limit ${limit} offset ${offset})...")
        userIds.addAll(
                jdbcTemplate.queryForList("SELECT profile_id FROM clan.clan_member WHERE LOWER(name) LIKE ? LIMIT ? OFFSET ?", Long.class,
                        sqlPattern, limit, offset)
                        .toSet()
        )
        gameApp.sessions.each { session ->
            def user = session.user
            if (user instanceof ClanMember) {
                final def userProfile = user as ClanMember
                if (userProfile.name && userProfile.name.toLowerCase().contains(searchPhrase.toLowerCase())) {
                    userIds.add(userProfile.profileId)
                }
            }
        }
        console.println("Found total ${total} users")
    }

    def profiles = fillProfilesCompact(context, userIds)

    console.println("Succesfully filled ${profiles.size()} found profiles")

    return AdminHandler.xstream.toXML([
            profiles: profiles,
            total   : total,
    ])
}

def batchClearName(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ProfileService profileService = context.getBean(ProfileService.class)

    Map<String, Object> params = AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, Object>
    def ids = (params.profiles as List).unique()

    def profiles = ids.collect { profileService.getUserProfile(it) }
            .findAll { it != null }

    console.println "Clear name for ${profiles.size()} profiles..."
    if (profiles.size() > 0) {
        for (UserProfile profile : profiles) {
            profileService.clearName(profile)
            profileService.updateAsync(profile)
        }
        return AdminHandler.xstream.toXML("[OK] " + profiles.size() + " profiles: names was successfully cleared")
    } else {
        return AdminHandler.xstream.toXML("[OK] found 0 of " + ids.size() + " profiles: nothing to clear")
    }
}

def getBossCheatStats(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    Map<String, Object> params = AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, Object>
    String forDate = params.get("forDate")
    String[] groupBy = (params.get("groupBy") as String[])?.findAll { ['gamerId', 'reason', 'missionId'].contains(it) }
    def groupCollector = (params.get("collector") == "joined") ? this.&allJoined : this.&mostFrequent
    def filter = params.get("filter") as Map<String, Object>

    synchronized (context) {
        def regex = '\\\"valid\\\":false'
        def exclude = 'UNCHECKED'
        def command
        if (forDate == "today") {
            command = ["bash", "-c", "grep -P '${regex}' logs/cdr/battles_stat.log | grep -v ${exclude}"]
        } else {
            command = ["bash", "-c", "zgrep -P '${regex}' logs/cdr/battles_stat-${forDate}.log.zip | grep -v ${exclude}"]
        }
        console.println "executing command: " + command
        long startTime = System.currentTimeMillis()
        def proc = command.execute()

        ObjectMapper mapper = new ObjectMapper()

        List<Map<String, Object>> result = []
        proc.in.eachLine { row ->
            String[] ss = row.split("\t")
            if (ss[13]) {
                def mLog = mapper.readValue(ss[13], Map.class)
                def item = [
                        gamerId     : ss[2],
                        result      : ss[4],
                        missionId   : ss[6] as int,
                        reason      : mLog.reason,
                        consequence : mLog.consequence,
                        count       : 1,
                        desynchValue: mLog.desynchValue,
                ]
                boolean matches = !filter ? true : filter.every { key, value -> item[key] == value }
                if (matches) {
                    result.add(item)
                }
            }
        }
        if (groupBy) {
            result = result.groupBy {
                // делаем строку только из тех значений, по которым нужно группировать, разделенных |
                groupBy.join('|').replace('gamerId', it.gamerId?.toString())
                        .replace('reason', it.reason?.toString())
                        .replace('missionId', it.missionId?.toString())
            }.collect { k, group ->
                [
                        groupKey    : k,
                        gamerId     : groupCollector(group) { it.gamerId },
                        result      : groupCollector(group) { it.result },
                        missionId   : groupCollector(group) { it.missionId },
                        reason      : groupCollector(group) { it.reason },
                        consequence : groupCollector(group) { it.consequence },
                        count       : group.sum(0) { it.count },
                        desynchValue: group.findResults { it.desynchValue }.max(),
                ]
            }

        }

        String stderr = proc.err.text
        proc.waitFor()

        long execTime = System.currentTimeMillis() - startTime
        console.println "finished command: $command in $execTime ms"
        console.println "return code: " + proc.exitValue()
        console.println "stderr:" + stderr

        return AdminHandler.xstream.toXML(result)
    }
}

def getPvpCheatStats(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    Map<String, Object> params = AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, Object>
    String forDate = params.get("forDate")
    String[] groupBy = (params.get("groupBy") as String[])?.findAll { ['gamerId', 'reason', 'battleType'].contains(it) }
    def groupCollector = (params.get("collector") == "joined") ? this.&allJoined : this.&mostFrequent
    def filter = params.get("filter") as Map<String, Object>
    String battleLogFilter = ".battleLog.turns | contains([{valid: false}])"
    String jqFilter
    jqFilter = "select( ${battleLogFilter} )"
    synchronized (context) {
        def regex = 'battleLog' // бои без лога боя даже и не парсим через jq (оптимизация)
        def cdrPvpDir = "logs/cdr/cdr-pvp"
        if (Files.notExists(Paths.get(cdrPvpDir))) {
            cdrPvpDir = "logs/cdr"
        }
        def command
        if (forDate == "today") {
            command = ["bash", "-c", "grep '${regex}' ${cdrPvpDir}/pvp/pvp-details.log | jq -c '${jqFilter}'"]
        } else {
            command = ["bash", "-c", "zgrep '${regex}' ${cdrPvpDir}/pvp/pvp-details-${forDate}.log.zip | jq -c '${jqFilter}'"]
        }
        console.println "executing command: " + command
        long startTime = System.currentTimeMillis()
        def proc = command.execute()

        ObjectMapper mapper = new ObjectMapper()
        List<Map<String, Object>> result = []
        proc.in.eachLine { row ->
            def battle = mapper.readValue(row, Map.class)
            if (battle.battleLog) {
                def bLog = battle.battleLog as Map
                List gamerIds = battle.participants.collect { it.profile.profileId as int }
                List socialNetIds = battle.participants.collect { it.profile.socialNetId as int }
                (bLog.turns as List<Map>)?.findAll { !it.valid }?.each { turn ->
                    def item = [
                            gamerId    : "" + socialNetIds[turn.playerNum as int] + ":" + gamerIds[turn.playerNum as int],
                            battleType : battle.battleType,
                            reason     : turn.reason,
                            consequence: turn.consequence,
                            turnNum    : turn.turnNum,
                            lastWeapon : turn.currentWeaponId,
                            count      : 1,
                    ]
                    boolean matches = !filter ? true : filter.every { key, value -> item[key] == value }
                    if (matches) {
                        result.add(item)
                    }
                }
            }
        }
        if (groupBy) {
            result = result.groupBy {
                // делаем строку только из тех значений, по которым нужно группировать, разделенных |
                groupBy.join('|').replace('gamerId', it.gamerId as String)
                        .replace('reason', it.reason as String)
                        .replace('battleType', it.battleType as String)
            }.collect { k, group ->
                [
                        groupKey   : k,
                        gamerId    : groupCollector(group) { it.gamerId },
                        battleType : groupCollector(group) { it.battleType },
                        reason     : groupCollector(group) { it.reason },
                        consequence: groupCollector(group) { it.consequence },
                        turnNum    : groupCollector(group) { it.turnNum },
                        lastWeapon : groupCollector(group) { it.lastWeapon },
                        count      : group.sum(0) { it.count },
                ]
            }

        }

        String stderr = proc.err.text
        proc.waitFor()

        long execTime = System.currentTimeMillis() - startTime
        console.println "finished command: $command in $execTime ms"
        console.println "return code: " + proc.exitValue()
        console.println "stderr:" + stderr

        return AdminHandler.xstream.toXML(result)
    }
}

def addRestriction(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def restrictionService = context.getBean(RestrictionService.class)
    def profileService = context.getBean(ProfileService.class)
    def params = AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, Object>
    String[] gamers = (params.gamers as String).split("\r\n")

    int reason = (params.reason as Integer) ?: BanType.BAN_BY_ADMIN.type
    def blocks = params.blocks as short

    // проверяем, что админ написал причину
    def note = params.note as String
    if (note == null || note.isEmpty()) {
        return new CommonResponse(ServiceResult.ERR_INVALID_ARGUMENT, "note is empty", "note=${note.inspect()}")
    }

    String result = ""
    gamers.collect { it.split(" ")[0] }.findAll { !it.trim().isEmpty() }.each { profileId ->
        UserProfile profile = profileService.getUserProfile(profileId)
        if (profile) {
            RestrictionItem newRestriction = restrictionService.addRestriction(
                    profile.id,
                    blocks,
                    params.days as Long,
                    reason,
                    note,
                    request.adminUser,
            )
            if (!newRestriction) {
                def alreadyBlocked = restrictionService.getRestrictions(profile.id).inject(0) { res, item -> res | item.blocks } as short
                if (alreadyBlocked) {
                    result += "$profileId (уже есть запрет)\n"
                } else {
                    result += "$profileId (ошибка)\n"
                }
            } else {
                result += "$profileId (OK)\n"
            }

        } else {
            result += "$profileId (не найден)\n"
        }

    }
    return AdminHandler.xstream.toXML(result)
}

public <T> T mostFrequent(List<Map> items, Closure<T> transform) {
    items.findResults(transform).groupBy { it }.max { Map.Entry e -> e.value.size() }?.key
}

public <T> String allJoined(List<Map> items, Closure<T> transform) {
    items.findResults(transform).unique().join(' ') // findResults здесь используется как Optional.flatMap: [[gamerId: 1], [:], [gamer:Id: 3] -> [1, 3]
}
