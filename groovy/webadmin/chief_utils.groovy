import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.pragmatix.achieve.domain.WormixAchievements
import com.pragmatix.app.common.BanType
import com.pragmatix.app.common.PvpBattleResult
import com.pragmatix.app.common.Race
import com.pragmatix.app.common.TeamMemberType
import com.pragmatix.app.init.controller.InitController
import com.pragmatix.app.model.UserProfile
import com.pragmatix.app.services.*
import com.pragmatix.app.settings.AppParams
import com.pragmatix.app.settings.BattleAwardSettings
import com.pragmatix.clanserver.domain.ClanMember
import com.pragmatix.clanserver.services.ClanServiceImpl
import com.pragmatix.common.utils.AppUtils
import com.pragmatix.craft.domain.Reagent
import com.pragmatix.gameapp.cache.SoftCache
import com.pragmatix.gameapp.sessions.Sessions
import com.pragmatix.gameapp.social.service.facebook.FacebookService
import com.pragmatix.pvp.BattleWager
import com.pragmatix.pvp.services.PvpService
import com.pragmatix.webadmin.AdminHandler
import com.pragmatix.webadmin.CdrLogHelper
import com.pragmatix.wormix.webadmin.interop.InteropSerializer
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest
import groovy.transform.Field
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.context.ApplicationContext
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

import javax.sql.DataSource
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.ResultSet
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.function.Function

def moveProfile(ApplicationContext context, String adminUser, int profileId, String from, int sourceProfileId) {
    if (profileId == 0) return ""
    def target = new NamedParameterJdbcTemplate(context.getBean(DataSource.class))
    def profileService = context.getBean(ProfileService.class)
    def banService = context.getBean(BanService.class)
    def profile = profileService.getUserProfile(profileId)
    if (!profile) return "ERROR: Не верный id профиля! (1-й аргумент)"
    def softCache = context.getBean(SoftCache.class)
    def session = Sessions.get(profile)
    if (session != null) {
        session.close()
        Thread.sleep(100)
    }
    def host, databaseName, sourceSocialNetId, targetSocialNetId
    def serverId = context.getBean(InitController.class).serverId
    if (serverId == "android_main") {
        targetSocialNetId = "Android"
    } else if (serverId == "ios_main") {
        targetSocialNetId = "iOS"
    } else {
        targetSocialNetId = "Steam"
    }
    if (from == "ios") {
        host = serverId == "steam" ? "lord.rmart.ru" : "127.0.0.1"
        databaseName = "wormswar_mobile"
        sourceSocialNetId = "iOS"
    } else if (from == "android") {
        host = serverId == "steam" ? "lord.rmart.ru" : "127.0.0.1"
        databaseName = "wormswar_android"
        sourceSocialNetId = "Android"
    } else if (from == "steam") {
        host = "tesla.rmart.ru"
        databaseName = "wormswar_steam"
        sourceSocialNetId = "Steam"
    } else {
        return "ERROR: 2-й аргумент должен быть [ios, android, steam]"
    }

    def ds = new PGSimpleDataSource()
    ds.serverName = host
    ds.user = "smos"
    ds.databaseName = databaseName
    def source = new NamedParameterJdbcTemplate(ds)

    def user_profile
    try {
        user_profile = source.queryForMap("select * from wormswar.user_profile where id = ${sourceProfileId}", [:])
    } catch (EmptyResultDataAccessException e) {
        return "ERROR: Не верный id профиля источника! (3-й аргумент)"
    }
    target.update(""" 
update wormswar.user_profile
set armor                 = :armor,
    attack                = :attack,
    battles_count         = :battles_count,
    experience            = :experience,
    last_battle_time      = :last_battle_time,
    last_login_time       = :last_login_time,
    level                 = :level,
    money                 = :money,
    realmoney             = :realmoney,
    rating                = :rating,
    last_search_time      = :last_search_time,
    --social_id             = :social_id,
    hat                   = :hat,
    stuff                 = :stuff,
    login_sequence        = :login_sequence,
    reaction_rate         = :reaction_rate,
    current_mission       = :current_mission,
    recipes               = :recipes,
    comebacked_friends    = :comebacked_friends,
    current_new_mission   = :current_new_mission,
    kit                   = :kit,
    race                  = :race,
    locale                = :locale,
    name                  = :name,
    rename_act            = :rename_act,
    temporal_stuff        = :temporal_stuff,
    logout_time           = :logout_time,
    pick_up_daily_bonus   = :pick_up_daily_bonus,
    races                 = :races,
    select_race_time      = :select_race_time,
    vip_expiry_time       = :vip_expiry_time,
    last_payment_date     = :last_payment_date,
    skins                 = :skins,
    rename_vip_act        = :rename_vip_act,
    country_code          = :country_code,
    currency_code         = :currency_code,
    --device_id             = :device_id,
    level_up_time         = :level_up_time,
    release_award         = :release_award,
    --strict_auth           = :strict_auth,
    --version               = :version,
    vip_subscription_id   = :vip_subscription_id
    --advertising_purchases = :advertising_purchases
where id = ${profileId};
""", user_profile)
//-- reagents ----------------------------------
    def reagents = source.queryForMap("select * from wormswar.reagents where profile_id = ${sourceProfileId}", [:])
    reagents.remove("profile_id")
    def updateReaqentsSql = """ 
update wormswar.reagents set ${reagents.keySet().collect { " ${it} = :${it}" }.join(",")}
where profile_id = ${profileId};
"""
    if (target.update(updateReaqentsSql, reagents) == 0) {
        target.update("insert into wormswar.reagents (profile_id) values (${profileId});", [:])
        target.update(updateReaqentsSql, reagents)
    }
//-- true_skill  -------------------------------
    def true_skill = source.queryForMap("select * from wormswar.true_skill where profile_id = ${sourceProfileId}", [:])
    true_skill.remove("profile_id")
    if (target.update(""" 
update wormswar.true_skill set ${true_skill.keySet().collect { " ${it} = :${it}" }.join(",")}
where profile_id = ${profileId};
""", true_skill) == 0) {
        true_skill.profile_id = profileId
        target.update("insert into wormswar.true_skill (profile_id, battles, mean, spread) values (:profile_id, :battles, :mean, :spread)", true_skill)
    }
//-- worms_achievements  -----------------------
    def achieveId = profileService.getProfileAchieveId(profileId)
    def achieveSourceId = "" + sourceProfileId
    if (from == "steam") {
        achieveSourceId = source.queryForObject("select string_id from wormswar.social_id where social_net_id = 12 and profile_id = $sourceProfileId", [:], String.class)
    }
    def worms_achievements = source.queryForMap("select * from achieve.worms_achievements where profile_id = '${achieveSourceId}'", [:])
    worms_achievements.remove("profile_id")
    worms_achievements.remove("user_profile_id")
    def updateAchievementsSql = """ 
update achieve.worms_achievements set ${worms_achievements.keySet().collect { " ${it} = :${it}" }.join(",")}
where profile_id = '${achieveId}';
"""
    if (target.update(updateAchievementsSql, worms_achievements) == 0) {
        target.update("insert into achieve.worms_achievements (profile_id) values ('${achieveId}')", [:])
        target.update(updateAchievementsSql, worms_achievements)
    }
//-- backpack_item  ----------------------------
    target.update("delete from wormswar.backpack_item_${profileId % 4} where profile_id = ${profileId}", [:])
    source.queryForList("select weapon_count, weapon_id from wormswar.backpack_item where profile_id = ${sourceProfileId}", [:]).each {
        target.update("insert into wormswar.backpack_item_${profileId % 4} values (${profileId}, ${it.weapon_count}, ${it.weapon_id})", [:])
    }
//----------------------------------------------
    softCache.remove(UserProfile.class, profile.getId())
    softCache.remove(WormixAchievements.class, achieveId)

    if (from == "steam") {
        def stuffService = context.getBean(StuffService.class)
        profile = profileService.getUserProfile(profileId)
        profile.level = Math.min(30, profile.level)
        [5001, 5002, 5003, 5004, 5005, 5006, 5007, 5008].each { stuffService.removeStuff(profile, (short) it) }
        profile.markDirty()
        profileService.updateSync(profile)
        softCache.remove(UserProfile.class, profile.getId())
    }

    banService.addToBanList((long) profileId, BanType.PROFILE_MOVED_FROM.type, -1, "профиль был перенесен из ${sourceSocialNetId} ${sourceProfileId}", adminUser, "")

    source.update("""
    INSERT INTO wormswar.ban_list(
            id, admin, date, end_date, note, profile_id, type, attachments)
    VALUES (nextval('ban_sequence'), :admin, now(), null, :note, :profile_id, :type, '');
""", [
            admin     : adminUser,
            profile_id: sourceProfileId,
            type      : BanType.PROFILE_MOVED_TO.type,
            note      : "профиль был перенесен в ${targetSocialNetId} ${profileId}"
    ])

    "OK UPID{${profile.getId()}}"
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

def getSimpleBossStat(ApplicationContext context, PrintWriter console, String dateFromInclusive, String dateToInclusive) {
    def sourceDir = "logs/cdr/events"
    def filePrefix = "events"
    def cmdPattern = """zgrep -h END_SIMPLE_BATTLE #in# | jq -cr 'select(.missionId > 0) | "\\(.missionId)-\\(.bossBattleResultType)"' | sort | uniq -c | cut -d '-' --output-delimiter=' ' -f 1,2 | sort -n -k2,2 | awk '{print \$2" "\$3" "\$1}'"""
    parseFiles(context, console, dateFromInclusive, dateToInclusive, sourceDir, filePrefix, cmdPattern)
}

@Field
def awkCmd = """'
\$2 == 0 {
    win[\$1]++
    w_turns[\$1]+=\$3
    if(\$3 > w_max[\$1])
        w_max[\$1]=\$3
    if(\$3 < w_min[\$1] || w_min[\$1]==0)
        w_min[\$1]=\$3
}
\$2 != 0 {
    def[\$1]++
    d_turns[\$1]+=\$3
    if(\$3 > d_max[\$1])
        d_max[\$1]=\$3
    if(\$3 < d_min[\$1] || d_min[\$1]==0)
        d_min[\$1]=\$3
}
END{
    for (i in def)
        printf("%s def: %d turns: %d %.0f %d win: %d turns: %d %.0f %d \\n", i, def[i], d_min[i], def[i] > 0 ? d_turns[i]/def[i] : 0, d_max[i], win[i], w_min[i], win[i] > 0 ? w_turns[i]/win[i] : 0, w_max[i])
}'"""

def getCooperativeBossStat(ApplicationContext context, PrintWriter console, String dateFromInclusive, String dateToInclusive) {
    def sourceDir = "logs/cdr/pvp"
    def filePrefix = "pvp-details"
    def cmdPattern = """
echo "friend:"
zgrep -h "battleType\\":1" #in# | jq -cr 'select((.missionIds | length) == 1) | "\\(.missionIds[0])#\\(.mapId) \\(.participants | map(.battleResult) | min) \\(.turnCount)"' | awk $awkCmd | sort -n
echo
echo "arena:"
zgrep -h "battleType\\":2" #in# | jq -cr 'select((.missionIds | length) == 1) | "\\(.missionIds[0])#\\(.mapId) \\(.participants | map(.battleResult) | min) \\(.turnCount)"' | awk $awkCmd | sort -n
"""
    parseFiles(context, console, dateFromInclusive, dateToInclusive, sourceDir, filePrefix, cmdPattern)
}

def getSuperBossStat(ApplicationContext context, PrintWriter console, String dateFromInclusive, String dateToInclusive) {
    def sourceDir = "logs/cdr/pvp"
    def filePrefix = "pvp-details"
    def cmdPattern = """
echo "friend:"
zgrep -h "battleType\\":1" #in# | jq -cr 'select((.missionIds | length) == 2) | "\\(.missionIds[0])-\\(.missionIds[1])#\\(.mapId) \\(.participants | map(.battleResult) | min) \\(.turnCount)"' |  awk $awkCmd | sort -n  
echo
echo "arena:"
zgrep -h "battleType\\":2" #in# | jq -cr 'select((.missionIds | length) == 2) | "\\(.missionIds[0])-\\(.missionIds[1])#\\(.mapId) \\(.participants | map(.battleResult) | min) \\(.turnCount)"' |  awk $awkCmd | sort -n
"""
    parseFiles(context, console, dateFromInclusive, dateToInclusive, sourceDir, filePrefix, cmdPattern)
}

def getProfileSessions(ApplicationContext context, PrintWriter console, int profileId, String date) {
    def (String cmd, String sourceDir, String fileName, String dt) = applyForEvents(date)
    def cmdPattern = """$cmd ":$profileId," #in# | grep "LOGIN\\|LOGOUT" | jq -c 'select(.profileId==$profileId) | """ +
            """[.date,.event,.version,"METHOD{getLoginsByIp "+.remoteAddress+" $dt}{"+.remoteAddress+"}",.sessionTime,"&nbsp;&nbsp",{level: .profile.level},{money:.profile.money},{real:.profile.realMoney}] | map(select(. != null))'"""
    def method = "getProfileSessions"
    def dt0 = LocalDate.parse(dt).minusDays(1).toString()
    def dt1 = LocalDate.now().toString() != dt ? LocalDate.parse(dt).plusDays(1).toString() : null
    String out = "METHOD{ }{INDEX}: [UPID{$profileId} METHOD{getProfileEvents $profileId $dt}{>>}] METHOD{$method $profileId $dt0}{$dt0} $dt "
    if (dt1) out += "METHOD{$method $profileId $dt1}{$dt1}"
    out + "\n" + parseFile(context, console, sourceDir, fileName, cmdPattern)
            .replaceAll(",\"METHOD\\{getLoginsByIp  $dt\\}\\{\\}\"", "")
            .replaceAll(",", " ")
            .replaceAll("\"$dt ", "")
            .replaceAll("[\\[\\]\"]", "")
            .replaceAll("LOGOUT(.+?)\\{", "LOGOUT\$1${space(20)}\\{")
}

class Unit {
    public int race
    public int kit
    public int hat
    public TeamMemberType type

    Race race() {
        Race.valueOf(race)
    }

    @Override
    public String toString() {
        return "{${type}:${race()}"
    }
}

class Profile {
    public int profileId
    public List<Unit> units

    @Override
    String toString() {
        return "{units=$units}";
    }
}

class Participant {
    public Profile profile
    public int battleResult

    PvpBattleResult battleResult() {
        PvpBattleResult.valueOf(battleResult)
    }

    @Override
    String toString() {
        return "{$profile}, ${battleResult()}"
    }
}

class PvpDetails {
    public BattleWager wager_raw
    public List<Participant> participants

    @Override
    String toString() {
        return "{$wager_raw, $participants}";
    }
}

class RaceStat {
    int leader
    int leaderWin
    int unit
    int unitWin

    int total() { leader + unit }

    int leaderWinPercent() { leader > 0 ? leaderWin * 100 / leader : 0 }

    int unitWinPercent() { unit > 0 ? unitWin * 100 / unit : 0 }
}

def getRaceStats(ApplicationContext context, PrintWriter console, String date, String wager, int teamSize) {
    def (String cmd, String sourceDir, String fileName, String dt) = applyForPvpDetails(date)
    def cmdPattern = """$cmd wager_raw #in# | jq -c '{wager_raw, participants}'"""

    def method = "getRaceStats"
    def dt0 = LocalDate.parse(dt).minusDays(1).toString()
    def dt1 = LocalDate.now().toString() != dt ? LocalDate.parse(dt).plusDays(1).toString() : null
    String out = "METHOD{ }{INDEX}: (wager=$wager, teamSize=$teamSize) METHOD{$method $dt0 $wager $teamSize}{$dt0} $dt "
    if (dt1) out += "METHOD{$method $dt1 $wager $teamSize}{$dt1}"

    def mapper = new ObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Set<BattleWager> ignored = [
            BattleWager.ROPE_RACE_QUEST,
            BattleWager.FRIEND_ROPE_RACE_QUEST,
            BattleWager.FRIEND_ROPE_RACE,
    ]

    Map<Race, Map<BattleWager, RaceStat>> result = [:]
    parseFile(context, console, sourceDir, fileName, cmdPattern).split("\n")
            .findAll { it }
            .collect {
                try {
                    return mapper.readValue(it, PvpDetails.class)
                } catch (Exception e) {
                    return null
                }
            }.findAll { it }
            .forEach { pvpDetails ->
                if (wager != 'ALL') {
                    if (pvpDetails.wager_raw.name() != wager) return
                } else {
                    if (ignored.contains(pvpDetails.wager_raw)) return
                }

                if (teamSize > 0) {
                    for (def participant : pvpDetails.participants) {
                        if (participant.profile.units?.size() != teamSize) return
                    }
                }

                pvpDetails.participants.forEach {
                    it.profile.units?.forEach { unit ->
                        def raceStat = result.computeIfAbsent(unit.race(), new Function<Race, Map<BattleWager, RaceStat>>() {
                            @Override
                            Map<BattleWager, RaceStat> apply(Race race) {
                                return [:]
                            }
                        }).computeIfAbsent(pvpDetails.wager_raw, new Function<BattleWager, RaceStat>() {
                            @Override
                            RaceStat apply(BattleWager battleWager) {
                                return new RaceStat()
                            }
                        })
                        if (unit.type == TeamMemberType.Himself) {
                            raceStat.leader++
                            if (it.battleResult() == PvpBattleResult.WINNER) {
                                raceStat.leaderWin++
                            }
                        } else {
                            raceStat.unit++
                            if (it.battleResult() == PvpBattleResult.WINNER) {
                                raceStat.unitWin++
                            }
                        }
                    }
                }
            }
    out += "\n\n"
    result.entrySet().forEach {
        out += "<b>${it.key.name()}</b>\n"
        (it.value.entrySet() as List)
                .sort { it.value.total() }
                .reverse()
                .forEach {
                    def battleWager = it.key.name()
                    def mm = wager == 'ALL' ? "METHOD{$method $dt $battleWager $teamSize}{$battleWager}" : "METHOD{$method $dt ALL $teamSize}{<<} $battleWager"
                    def raceStat = it.value
                    out += "&nbsp;&nbsp;$mm => leader = ${raceStat.leader} (${raceStat.leaderWinPercent()}% win) unit = ${raceStat.unit} (${raceStat.unitWinPercent()}% win)\n"
                }
    }
    return out
}

def getEquipWagerStats(ApplicationContext context, PrintWriter console, String date, String wager, int teamSize, int equipId) {
    def (String cmd, String sourceDir, String fileName, String dt) = applyForPvpDetails(date)
    def cmdPattern = """$cmd wager_raw #in# | jq -c '{wager_raw, participants}'"""

    def method = "getEquipWagerStats"
    def dt0 = LocalDate.parse(dt).minusDays(1).toString()
    def dt1 = LocalDate.now().toString() != dt ? LocalDate.parse(dt).plusDays(1).toString() : null
    String out = "METHOD{ }{INDEX}: (wager=$wager, teamSize=$teamSize, equipId=$equipId) METHOD{$method $dt0 $wager $teamSize $equipId}{$dt0} $dt "
    if (dt1) out += "METHOD{$method $dt1 $wager $teamSize $equipId}{$dt1}"
    if (!equipId) return out

    def mapper = new ObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Set<BattleWager> ignored = [
            BattleWager.ROPE_RACE_QUEST,
            BattleWager.FRIEND_ROPE_RACE_QUEST,
            BattleWager.FRIEND_ROPE_RACE,
    ]

    Map<BattleWager, List<Integer>> result = [:]
    parseFile(context, console, sourceDir, fileName, cmdPattern).split("\n")
            .findAll { it }
            .collect {
                try {
                    return mapper.readValue(it, PvpDetails.class)
                } catch (Exception e) {
                    out += e.toString() + "\n"
                    return null
                }
            }.findAll { it }
            .forEach { pvpDetails ->
                if (wager != 'ALL') {
                    if (pvpDetails.wager_raw.name() != wager) return
                } else {
                    if (ignored.contains(pvpDetails.wager_raw)) return
                }

                if (teamSize > 0) {
                    for (def participant : pvpDetails.participants) {
                        if (participant.profile.units?.size() != teamSize) return
                    }
                }

                pvpDetails.participants.forEach { participant ->
                    participant.profile.units?.forEach { unit ->
                        def equipStat = result.computeIfAbsent(pvpDetails.wager_raw, new Function<BattleWager, List<Integer>>() {
                            @Override
                            List<Integer> apply(BattleWager battleWager) {
                                return []
                            }
                        })
                        if (unit.type == TeamMemberType.Himself && (unit.hat == equipId || unit.kit == equipId)) {
                            equipStat.add(participant.profile.profileId)
                        }
                    }
                }
            }
    out += "\n\n"
    result.entrySet()
            .forEach {
                def battleWager = it.key.name()
                def mm = wager == 'ALL' ? "METHOD{$method $dt $battleWager $teamSize $equipId}{$battleWager}" : "METHOD{$method $dt ALL $teamSize $equipId}{<<} $battleWager"
                def equipStat = it.value
                out += "&nbsp;&nbsp;$mm => battles = ${equipStat.size()}; uniq profiles = ${(equipStat as Set).size()}\n"
            }
    return out
}

def space(int i) {
    StringUtils.repeat("&nbsp", i)
}

def getProfileEvents(ApplicationContext context, PrintWriter console, int profileId, String date) {
    def (String cmd, String sourceDir, String fileName, String dt) = applyForEvents(date)
    def cmdPattern = """$cmd ":$profileId," #in# | jq -c 'select(.profileId==$profileId) | """ +
            """[.date,.event,.battleTime,.eventType,.version,.sessionTime,"METHOD{getLoginsByIp "+.remoteAddress+" $dt}{"+.remoteAddress+"}",.missionId,.battleType,.wager,.result,.bossBattleWinType,.friendId,{money:.money}] | map(select(. != null))'"""
    def method = "getProfileEvents"
    def dt0 = LocalDate.parse(dt).minusDays(1).toString()
    def dt1 = LocalDate.now().toString() != dt ? LocalDate.parse(dt).plusDays(1).toString() : null
    String out = "METHOD{ }{INDEX}: [UPID{$profileId} METHOD{getProfileSessions $profileId $dt}{>>}] METHOD{$method $profileId $dt0}{$dt0} $dt "
    if (dt1) out += "METHOD{$method $profileId $dt1}{$dt1}"
    out + "\n" + parseFile(context, console, sourceDir, fileName, cmdPattern)
            .replaceAll(",\"METHOD\\{getLoginsByIp  $dt\\}\\{\\}\"", "")
            .replaceAll(",", " ")
            .replaceAll("\"$dt ", "")
            .replaceAll("[\\[\\]\"]", "")
            .replaceAll(" \\{money:null\\}", "")
}


def getLoginsByIp(ApplicationContext context, PrintWriter console, String ip, String date) {
    def (String cmd, String sourceDir, String fileName, String dt) = applyForEvents(date)
    def method = "getLoginsByIp"
    def cmdPattern = """$cmd LOGIN #in# | grep "${ip}" | jq -c 'select(.remoteAddress==\"$ip\") | """ +
            """[.date,.event,"UPID{"+(.profileId|tostring)+"}",.version,{level: .profile.level},{money:.profile.money},{real:.profile.realMoney}]'"""
    def dt0 = LocalDate.parse(dt).minusDays(1).toString()
    def dt1 = LocalDate.now().toString() != dt ? LocalDate.parse(dt).plusDays(1).toString() : null
    String out = "METHOD{ }{INDEX}: [$ip] METHOD{$method $ip $dt0}{$dt0} $dt "
    if (dt1) out += "METHOD{$method $ip $dt1}{$dt1}"
    out + "\n" + parseFile(context, console, sourceDir, fileName, cmdPattern)
            .replaceAll(",", " ")
            .replaceAll("\"$dt ", "")
            .replaceAll("[\\[\\]\"]", "")
}

def getAllLoginsByIp(ApplicationContext context, PrintWriter console, String ip) {
    def mapper = new ObjectMapper()
    def (String cmd, String sourceDir, String fileName, String dt) = applyForEvents(null)
    def cmdPattern = """zgrep -h LOGIN logs/cdr/events/*.zip logs/cdr/events/events.log | grep "${ip}" | jq -c 'select(.remoteAddress==\"$ip\") | {date, profileId}' """
    List<Map<String, Object>> result = parseFile(context, console, sourceDir, fileName, cmdPattern).split("\n")
            .findAll { it }
            .collect {
                try {
                    mapper.readValue(it, Map.class)
                } catch (Exception e) {
                    return null
                }
            }
            .findAll { it }
    def uids = result.collect { it.profileId as int }.unique()
    uids.collect { profileId ->
        def logins = result.findAll { map -> map.profileId == profileId }
                .collect { it.date as String }
                .collect { it.replace(" ", "T") }
                .collect { LocalDateTime.parse(it).toLocalDate() }
                .unique().sort()
                .collect { "METHOD{getProfileSessions $profileId $it}{$it}" }
                .join(", ")
        "UPID{${profileId}} # ${logins}"
    }.join("\n")
}

def getRefsList(ApplicationContext context, PrintWriter console, String dateFrom) {
    def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm")
    def from = new SimpleDateFormat("yyyy-MM-dd").parse(dateFrom)
    context.getBean(ReferralLinkService.class).referralLinks.values().findAll { it.start.after(from) }.toSorted { it.start }
            .collect { "${it.token} ${sdf.format(it.start)} ${sdf.format(it.finish)}" }.join("\n")
}

def getCheatProfileSessions(ApplicationContext context, PrintWriter console, String date, String version) {
    def (String cmd, String sourceDir, String fileName, String dt) = applyForEvents(date)
    def method = "getCheatProfileSessions"
    def cmdPattern = """$cmd LOGIN #in# | grep -v "${version}" | jq -c '[.date,.event,"METHOD{getProfileEvents "+(.profileId|tostring)+" $dt}{"+(.profileId|tostring)+"}",.version,"METHOD{getLoginsByIp "+.remoteAddress+" $dt}{"+.remoteAddress+"}"]'"""
    def dt0 = LocalDate.parse(dt).minusDays(1).toString()
    def dt1 = LocalDate.now().toString() != dt ? LocalDate.parse(dt).plusDays(1).toString() : null
    String out = "METHOD{ }{INDEX}:  METHOD{$method $dt0 $version}{$dt0} $dt "
    if (dt1) out += "METHOD{$method $dt1 $version}{$dt1}"
    out + "\n" + parseFile(context, console, sourceDir, fileName, cmdPattern)
            .replaceAll(",", " ")
            .replaceAll("\"$dt ", "")
            .replaceAll("[\\[\\]\"]", "")
}

def getFarmersTop(ApplicationContext context, PrintWriter console, String date) {
    def (String grep, String sourceDir, String fileName, String dt) = applyForEvents(date)
    def method = "getFarmersTop"
    def cmdPattern = """$grep _BATTLE #in# | jq -cr 'select(.money>0) | (.profileId|tostring)+" "+(.money|tostring)' | awk '{arr[\$1]+=\$2;arr2[\$1]++}END{for (key in arr) printf("%s %s UPID{%s} METHOD{getProfileEvents %s $dt}{>>}\\n", arr[key],arr2[key],key,key)}' | sort -nr | head -100"""
    def dt0 = LocalDate.parse(dt).minusDays(1).toString()
    def dt1 = LocalDate.now().toString() != dt ? LocalDate.parse(dt).plusDays(1).toString() : null
    String out = "METHOD{ }{INDEX}:  METHOD{$method $dt0}{$dt0} $dt "
    if (dt1) out += "METHOD{$method $dt1}{$dt1}"
    out + "\n" + parseFile(context, console, sourceDir, fileName, cmdPattern)
}

def getLongestSessions(ApplicationContext context, PrintWriter console, String date) {
    def (String cmd, String sourceDir, String fileName, String dt) = applyForEvents(date)
    def method = "getLongestSessions"
    def cmdPattern = """$cmd LOGOUT #in# | jq -cr '(.profileId|tostring)+" "+.sessionTime' | """ +
            """awk '{arr[\$1]+=convert(\$2)}END{for (key in arr) printf("%s %s %s\\n",arr[key],key,strftime("%H:%M:%S",arr[key],1))} function convert(t){split(t,Arr,":");return Arr[1]*3600+Arr[2]*60+Arr[3]}' | """ +
            """sort -nr | awk '{printf("%s UPID{%s} METHOD{getProfileSessions %s $dt}{>>}\\n",\$3,\$2,\$2)}' | head -60"""
    def dt0 = LocalDate.parse(dt).minusDays(1).toString()
    def dt1 = LocalDate.now().toString() != dt ? LocalDate.parse(dt).plusDays(1).toString() : null
    String out = "METHOD{ }{INDEX}:  METHOD{$method $dt0}{$dt0} $dt "
    if (dt1) out += "METHOD{$method $dt1}{$dt1}"
    out + "\n" + parseFile(context, console, sourceDir, fileName, cmdPattern)
}

protected List applyForEvents(String date) {
    def sourceDir = "logs/cdr/events"
    def fileName = StringUtils.isEmpty(date) || LocalDate.now().toString() == date ? "events.log" : "events-${date}.log.zip"
    def cmd = fileName.endsWith(".zip") ? "zgrep" : "grep"
    def dt = StringUtils.isEmpty(date) ? LocalDate.now().toString() : date
    return [cmd, sourceDir, fileName, dt]
}

protected List applyForPvpDetails(String date) {
    def sourceDir = "logs/cdr/pvp"
    def fileName = StringUtils.isEmpty(date) || LocalDate.now().toString() == date ? "pvp-details.log" : "pvp-details-${date}.log.zip"
    def cmd = fileName.endsWith(".zip") ? "zgrep" : "grep"
    def dt = StringUtils.isEmpty(date) ? LocalDate.now().toString() : date
    return [cmd, sourceDir, fileName, dt]
}

def getTimeInPvpBattles(ApplicationContext context, PrintWriter console, String dateFromInclusive, String dateToInclusive) {
    def sourceDir = "logs/cdr/pvp"
    def filePrefix = "pvp-details"
    def cmdPattern = """
zgrep -h ",\\"battleType\\":4,\\"wager\\":0,"   #in# | jq -c 'select(.special=="coliseum") | "\\((.finish | strptime("%Y-%m-%d %H:%M:%S") | mktime) - (.start | strptime("%Y-%m-%d %H:%M:%S") | mktime)) \\(.participants[0].profile.profileId) \\(.participants[1].profile.profileId)"' | sed 's/"//g' | awk '{a_d[\$2]+=\$1;a_d[\$3]+=\$1}END{td=0;size=1; for(i in a_d) td+=a_d[i]; if(length(a_d)>0) size=length(a_d); print "coliseum "strftime("%H:%M:%S", td/size, 3)}';
zgrep -h ",\\"battleType\\":4,\\"wager\\":0,"   #in# | jq -c 'select(.special=="merc") | "\\((.finish | strptime("%Y-%m-%d %H:%M:%S") | mktime) - (.start | strptime("%Y-%m-%d %H:%M:%S") | mktime)) \\(.participants[0].profile.profileId) \\(.participants[1].profile.profileId)"' | sed 's/"//g' | awk '{a_d[\$2]+=\$1;a_d[\$3]+=\$1}END{td=0;size=1; for(i in a_d) td+=a_d[i]; if(length(a_d)>0) size=length(a_d); print "merc "strftime("%H:%M:%S", td/size, 3)}';
zgrep -h ",\\"battleType\\":4,\\"wager\\":15,"  #in# | jq -c '"\\((.finish | strptime("%Y-%m-%d %H:%M:%S") | mktime) - (.start | strptime("%Y-%m-%d %H:%M:%S") | mktime)) \\(.participants[0].profile.profileId) \\(.participants[1].profile.profileId)"' | sed 's/"//g' | awk '{a_d[\$2]+=\$1;a_d[\$3]+=\$1}END{td=0; for(i in a_d) td+=a_d[i]; print "1x1#15 "strftime("%H:%M:%S", td/length(a_d), 3)}';
zgrep -h ",\\"battleType\\":4,\\"wager\\":20,"  #in# | jq -c '"\\((.finish | strptime("%Y-%m-%d %H:%M:%S") | mktime) - (.start | strptime("%Y-%m-%d %H:%M:%S") | mktime)) \\(.participants[0].profile.profileId) \\(.participants[1].profile.profileId)"' | sed 's/"//g' | awk '{a_d[\$2]+=\$1;a_d[\$3]+=\$1}END{td=0;size=1; for(i in a_d) td+=a_d[i]; if(length(a_d)>0) size=length(a_d); print "1x1#20 "strftime("%H:%M:%S", td/size, 3)}';
zgrep -h ",\\"battleType\\":4,\\"wager\\":300," #in# | jq -c '"\\((.finish | strptime("%Y-%m-%d %H:%M:%S") | mktime) - (.start | strptime("%Y-%m-%d %H:%M:%S") | mktime)) \\(.participants[0].profile.profileId) \\(.participants[1].profile.profileId)"' | sed 's/"//g' | awk '{a_d[\$2]+=\$1;a_d[\$3]+=\$1}END{td=0; for(i in a_d) td+=a_d[i]; print "1x1#300 "strftime("%H:%M:%S", td/length(a_d), 3)}';
zgrep -h ",\\"battleType\\":8,\\"wager\\":50,"  #in# | jq -c '"\\((.finish | strptime("%Y-%m-%d %H:%M:%S") | mktime) - (.start | strptime("%Y-%m-%d %H:%M:%S") | mktime)) \\(.participants[0].profile.profileId) \\(.participants[1].profile.profileId) \\(.participants[2].profile.profileId)"' | sed 's/"//g' | awk '{a_d[\$2]+=\$1;a_d[\$3]+=\$1;a_d[\$4]+=\$1}END{td=0; for(i in a_d) td+=a_d[i]; print "1x1x1#50 "strftime("%H:%M:%S", td/length(a_d), 3)}';
zgrep -h ",\\"battleType\\":6,\\"wager\\":50,"  #in# | jq -c '"\\((.finish | strptime("%Y-%m-%d %H:%M:%S") | mktime) - (.start | strptime("%Y-%m-%d %H:%M:%S") | mktime)) \\(.participants[0].profile.profileId) \\(.participants[1].profile.profileId) \\(.participants[2].profile.profileId) \\(.participants[3].profile.profileId)"' | sed 's/"//g' | awk '{a_d[\$2]+=\$1;a_d[\$3]+=\$1;a_d[\$4]+=\$1;a_d[\$5]+=\$1}END{td=0; for(i in a_d) td+=a_d[i]; print "2x2#50 "strftime("%H:%M:%S", td/length(a_d), 3)}';
"""
    parseFiles(context, console, dateFromInclusive, dateToInclusive, sourceDir, filePrefix, cmdPattern)
}

def getBundlesStat(ApplicationContext context, PrintWriter console, String dt, String dateToExclusive) {
    if (!dt) {
        dt = LocalDate.now().toString()
    }
    if (!dateToExclusive) {
        dateToExclusive = LocalDate.parse(dt).plusDays(1).toString()
    }
    def sql = """
        select item::int, count(*) from payment_statistic_parent where payment_status = 0 and completed  
            and date >= '$dt' and date < '$dateToExclusive' and item ~ '^\\d+\$' and item::int > 50
        group by 1
        order by 1
    """
    def method = "getBundlesStat"
    def dt0 = LocalDate.parse(dt).minusDays(1).toString()
    def dt1 = LocalDate.now().toString() != dt ? LocalDate.parse(dt).plusDays(1).toString() : null
    String out = "METHOD{ }{INDEX}:  METHOD{$method $dt0}{$dt0} $dt "
    if (dt1) out += "METHOD{$method $dt1}{$dt1}"
    out += "\n\n"
    def jdbcTemplate = context.getBean(JdbcTemplate.class)
    jdbcTemplate.queryForList(sql).each { out += it.values().join(" ") + "\n" }
    out
}
// #/etc/crontab
// 1 *     * * *   user   gzip -kf /home/user/server/logs/cdr/events/events.log; gzip -kf /home/user/server/logs/cdr/pvp/pvp-details.log
// 1 *     * * *   user   gzip < /home/user/wormix-server-ok/logs/cdr/events/events.log       > /home/user/wormix-server-ok/logs/cdr/events/events.log.gz
// 1 *     * * *   user   gzip < /home/user/wormix-server-facebook/logs/cdr/events/events.log > /home/user/wormix-server-facebook/logs/cdr/events/events.log.gz
// 1 *     * * *   user   gzip < /home/user/wormix-server-mailru/logs/cdr/events/events.log   > /home/user/wormix-server-mailru/logs/cdr/events/events.log.gz

def copyEvents(ApplicationContext context, PrintWriter console, String dateFromInclusive, String dateToInclusive) {
    def sourceDir = "logs/cdr/events"
    def filePrefix = "events"
    copyCdr(context, console, dateFromInclusive, dateToInclusive, sourceDir, filePrefix)
}

def copyPvpBattles(ApplicationContext context, PrintWriter console, String dateFromInclusive, String dateToInclusive) {
    def sourceDir = "logs/cdr/pvp"
    def filePrefix = "pvp-details"
    copyCdr(context, console, dateFromInclusive, dateToInclusive, sourceDir, filePrefix)
}

def copyBattles(ApplicationContext context, PrintWriter console, String dateFromInclusive, String dateToInclusive) {
    def sourceDir = "logs/cdr"
    def filePrefix = "battles_stat"
    copyCdr(context, console, dateFromInclusive, dateToInclusive, sourceDir, filePrefix)
}

def getProfiles(ApplicationContext context, PrintWriter console) {
    def sql = """
        with bans as (
            select profile_id from wormswar.ban_list where end_date is null or end_date > now()
        )
        select UP.id, level, coalesce(name, '') as name, last_login_time from wormswar.user_profile UP
        WHERE not exists (select * from bans where UP.id = profile_id)
        order by UP.id
        limit 300
    """
    context.getBean(JdbcTemplate.class).queryForList(sql).collect {
        "${rightPad(it.id, 15)} ${rightPad(it.level, 5)} ${rightPad(it.name, 30)} ${AppUtils.formatDate(it.last_login_time)}"
    }.join("\n")
}

private String rightPad(Object s, int size) {
    StringUtils.rightPad("" + s, size, "#").replaceAll("#", "&nbsp;")
}

def copyConfigs(ApplicationContext context, PrintWriter console) {
    def sourceDir = "src/main/resources"
    [
            "achieve-beans.xml",
            "award-beans.xml",
            "battle-awards.xml",
            "battle-beans.xml",
            "beans.xml",
            "coliseum-beans.xml",
            "craft-beans.xml",
            "items-beans.xml",
            "mercenaries-beans.xml",
            "price-beans.xml",
            "quest-beans.xml",
            "seasons-beans.xml",
    ].collect() {
        copyFile(context, console, sourceDir, it)
        it
    }.join("\n")
}

def getHeroicMissionLevelsAndMaps(ApplicationContext context, PrintWriter console) {
    def battleAwardSettings = context.getBean(BattleAwardSettings.class)

    String out = battleAwardSettings.heroicMissionLevels.sort { getSortKey(it.value, it.key) }.collect {
        char[] space = new char[10 - it.key.length()]
        Arrays.fill(space, "#" as char)
        """&lt;entry key="${it.key}"${space}value="${it.value}" /&gt;"""
    }.join("\n").replaceAll("#", "&nbsp;")

    out += "\n\n"

    out += battleAwardSettings.heroicMissions.values().sort { getSortKey(battleAwardSettings.heroicMissionLevels.get(it.key), it.key) }.collect {
        char[] space = new char[10 - it.key.length()]
        Arrays.fill(space, "#" as char)
        def mapsSet = it.maps.sort().join(" ")
        char[] space2 = new char[35 - mapsSet.length()]
        Arrays.fill(space2, "#" as char)
        """&lt;bean p:key="${it.key}"${space}class="com.pragmatix.app.settings.HeroicMission" p:mapsSet="${mapsSet}"${space2}/&gt; &lt;!--${battleAwardSettings.heroicMissionLevels.get(it.key)}--&gt"""
    }.join("\n").replaceAll("#", "&nbsp;")

    out
}

private String getSortKey(int level, String bossKey) {
    def arr = bossKey.split("_")
    "" + level +
            "_" + StringUtils.leftPad(arr[0], 3, "0") +
            "_" + StringUtils.leftPad(arr[1], 3, "0")

}

def getFbPaymentDetails(ApplicationContext context, PrintWriter console, Long transactionId) {
    def service = context.getBean(FacebookService.class)
    "" + service.getPaymentDetails(transactionId, org.slf4j.LoggerFactory.getLogger(this.getClass()))
}

def copyCdr(ApplicationContext context, PrintWriter console, String dateFromInclusive, String dateToInclusive, String sourceDir, String filePrefix) {
    def cmdPattern = """
scp #in# vasilevskiy@slots.rmart.ru:/home/vasilevskiy/server_logs/${context.getEnvironment().getProperty("server.id")}/
for i in #in#
    do
        echo "copy \$i ..."
    done
echo ""
echo "Готово."
"""
    if (!dateFromInclusive) {
        parseFile(context, console, sourceDir, filePrefix + ".log.gz", cmdPattern)
    } else {
        parseFiles(context, console, dateFromInclusive, dateToInclusive, sourceDir, filePrefix, cmdPattern)
    }
}

def parseFiles(ApplicationContext context, PrintWriter console, String dateFromInclusive, String dateToInclusive, String sourceDir, String filePrefix, String cmdPattern) {
    def sdf = new SimpleDateFormat("yyyy-MM-dd")
//    synchronized (context) {
    if (!dateFromInclusive) {
        dateFromInclusive = sdf.format(new Date() - 1)
    }
    def dates = [dateFromInclusive]
    if (dateToInclusive) {
        def from = sdf.parse(dateFromInclusive)
        def to = sdf.parse(dateToInclusive)
        def days = Math.min(31, Math.max(0, to - from))
        for (int i = 0; i < days; i++) {
            def date = sdf.format(from + i + 1)
            if (new File("${sourceDir}/${filePrefix}-${date}.log.zip").exists()) {
                dates.add(date)
            }
        }
    }
    def source = dates.collect { "${sourceDir}/${filePrefix}-${it}.log.zip" }.join(" ")

    def command = ["bash", "-c", cmdPattern.replaceAll("#in#", source)]
    console.println "executing command: " + command
    long startTime = System.currentTimeMillis()

    def proc = command.execute()
    def stdin = proc.in.text
    String stderr = proc.err.text
    proc.waitFor()

    long execTime = System.currentTimeMillis() - startTime
    console.println "finished command: $command in $execTime ms"
    console.println "return code: " + proc.exitValue()
    console.println "stderr:" + stderr
    return stderr ? "Ошибка: " + stderr : "(${Math.round(execTime / 1000L)} сек.) ${dates.min()} - ${dates.max()}\n\n" + stdin
//    }
}

def parseFile(ApplicationContext context, PrintWriter console, String sourceDir, String fileName, String cmdPattern) {
//    def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//    synchronized (context) {
    def command = ["bash", "-c", cmdPattern.replaceAll("#in#", "${sourceDir}/$fileName")]
//    console.println "executing command: " + command
//    long startTime = System.currentTimeMillis()

    def proc = command.execute()
    def stdin = proc.in.text
    String stderr = proc.err.text
    proc.waitFor()

//    long execTime = System.currentTimeMillis() - startTime
//    console.println "finished command: $command in $execTime ms"
//    console.println "return code: " + proc.exitValue()
//    console.println "stderr:" + stderr
//    return stderr ? "Ошибка: " + stderr : "(${Math.round(execTime / 1000L)} сек.) ${sdf.format(new Date())}\n\n" + stdin
    return stderr ? "Ошибка: " + stderr : "\n" + stdin
//    }
}

def copyFile(ApplicationContext context, PrintWriter console, String sourceDir, String fileName) {
    def cmdPattern = "scp #in# vasilevskiy@slots.rmart.ru:/home/vasilevskiy/server_configs/"
    def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    def command = ["bash", "-c", cmdPattern.replaceAll("#in#", "${sourceDir}/$fileName")]
    console.println "executing command: " + command
    long startTime = System.currentTimeMillis()

    def proc = command.execute()
    def stdin = proc.in.text
    String stderr = proc.err.text
    proc.waitFor()

    long execTime = System.currentTimeMillis() - startTime
    console.println "finished command: $command in $execTime ms"
    console.println "return code: " + proc.exitValue()
    console.println "stderr:" + stderr
    return stderr ? "Ошибка: " + stderr : "(${Math.round(execTime / 1000L)} сек.) ${sdf.format(new Date())}\n\n" + stdin
}

def todayOldClients(ApplicationContext context, PrintWriter console) {
    def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    def cmd = " ( zgrep \"Версия клиента устарела\" logs/server/server-`date +'%Y-%m-%d'`-*.log.zip ; grep \"Версия клиента устарела\" logs/server.log ) | awk '{print \$7\" \"\$11}' | sort | uniq -c | sort -nr"

    def command = ["bash", "-c", cmd]
    console.println "executing command: " + command
    long startTime = System.currentTimeMillis()

    def proc = command.execute()
    def stdin = proc.in.text
    String stderr = proc.err.text
    proc.waitFor()

    long execTime = System.currentTimeMillis() - startTime
    console.println "finished command: $command in $execTime ms"
    console.println "return code: " + proc.exitValue()
    console.println "stderr:" + stderr
    return stderr ? "Ошибка: " + stderr : "(${Math.round(execTime / 1000L)} сек.) ${sdf.format(new Date())}\n\n" + stdin
}

def getReagentStats(ApplicationContext context, PrintWriter console, int reagentId, int amountFromInclusive, String dateFromInclusiveStr) {
    def jdbcTemplate = context.getBean(JdbcTemplate.class)

    String reagent = Reagent.valueOf(reagentId).name()
    def sdf = new SimpleDateFormat("yyyy-MM-dd")
    Date dateFromInclusive = sdf.parse(dateFromInclusiveStr)

    def out = "Counting users that have >= $amountFromInclusive ${reagent}s and lastLoginDate > $dateFromInclusiveStr...\n"
    console.println out

    (1..30).each { level ->
        def sql = """
            select count(*) from wormswar.reagents r
                inner join wormswar.user_profile u on r.profile_id = u.id
            where u.last_login_time >= ? and u.level = ? and r.$reagent >= ?
        """
        def count = jdbcTemplate.queryForObject(sql, Long.class, dateFromInclusive, level, amountFromInclusive)
        def res = "For level $level: $count users\n"
        console.println res
        out += res
    }
    out
}

def getWeaponStats(ApplicationContext context, PrintWriter console, int weaponId, String dateFromInclusiveStr) {
    def weaponService = context.getBean(WeaponService)
    def jdbcTemplate = context.getBean(JdbcTemplate)

    def weapon = weaponService.getWeapon(weaponId)?.name
    if (weapon == null) {
        return "ERROR: weapon not found by id " + weaponId
    }
    def sdf = new SimpleDateFormat("yyyy-MM-dd")
    Date dateFromInclusive = sdf.parse(dateFromInclusiveStr)

    def out = "Counting users that have $weapon and lastLoginDate > $dateFromInclusiveStr...\n"
    console.println(out)
    (1..30).each { level ->
        def sql = """
            select count(*) from wormswar.backpack_item w
                inner join wormswar.user_profile u on w.profile_id = u.id
            where u.last_login_time >= ? and u.level = ? and w.weapon_id = ? and w.weapon_count <> 0
        """
        def count = jdbcTemplate.queryForObject(sql, Long.class, dateFromInclusive, level, weaponId)
        def res = "For level $level: $count users\n"
        console.println res
        out += res
    }
    out
}

def getStuffStats(ApplicationContext context, PrintWriter console, short stuffId, String dateFromInclusiveStr, int levelFromInclusive, int levelToInclusive) {
    def stuffService = context.getBean(StuffService)
    def userProfileDao = context.getBean(DaoService).userProfileDao

    def stuff = stuffService.getStuff(stuffId, false)
    if (stuff == null) {
        return "ERROR: stuff not found by id " + stuffId
    }
    def sdf = new SimpleDateFormat("yyyy-MM-dd")
    Date dateFromInclusive = sdf.parse(dateFromInclusiveStr)

    String type = stuff.isBoost() ? "boost" : (!stuff.isKit() ? "hat" : "kit")
    def out = "Counting users of levels $levelFromInclusive..$levelToInclusive that have ${stuff.temporal ? "temporal" : ""} ${type} ${stuff.name} and lastLoginDate > $dateFromInclusiveStr...\n"
    console.println(out)

    (levelFromInclusive..levelToInclusive).each { level ->
        int count = 0
        def entities = userProfileDao.getEm()
                .createQuery("select u.id, u.stuff, u.temporalStuff from UserProfileEntity u where u.level = :level and u.lastLoginTime > :dateFrom")
                .setParameter("level", level as short)
                .setParameter("dateFrom", dateFromInclusive)
                .getResultList() as List<List>;
        entities.each {
            // пустой фейковый профиль: функции isExist нужны только поля stuff и temporalStuff
            def fakeProfile = new UserProfile(it[0] as int)
            fakeProfile.stuff = it[1] as short[]
            fakeProfile.temporalStuff = it[2] as byte[]
            if (stuffService.isExist(fakeProfile, stuffId)) {
                count++
            }
        }
        def res = "For level $level: $count users\n"
        console.println res
        out += res
    }
    out
}

def setHeroicMissionState(ApplicationContext context, int levelParam, String key, int map) {
    def level = levelParam - 1
    // 3 24_12 34
    def heroicMissionService = context.getBean(HeroicMissionService.class)
    def mission = heroicMissionService.battleAwardSettings.heroicMissions[key]
    if (!mission) return "ERROR: связки $key не существует!"
    if (!mission.maps.contains(map)) return "ERROR: для связки $key нет карты $map!"
    def keyLevel = heroicMissionService.battleAwardSettings.heroicMissionLevels[key]
    if (keyLevel != level) return "ERROR: связка $key имеет уровень сложности ${keyLevel + 1}!"

    def out = ""
    def state = heroicMissionService.heroicMissionStates[level]
    out += "Уровень: $levelParam\n"
    out += "Текущая связка: ${state.getCurrentMission()} на карте ${state.getCurrentMissionMap()}\n"
    heroicMissionService.heroicMissionStates[level].setCurrentMission(key)
    heroicMissionService.heroicMissionStates[level].setCurrentMissionMap(map)
//    out += "Новая связка: ${key} на карте ${map}\n"
    out += "Новая связка: ${state.getCurrentMission()} на карте ${state.getCurrentMissionMap()}\n"

    out
}

def doWork(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    "do nothing"
}

def achieveCheatersGraves(ApplicationContext context, int limit = 0) {
    achieveCheaters(context, "graves_sank", "s_graves_sank", limit)
}

def achieveCheatersInquisitor(ApplicationContext context, int limit = 0) {
    achieveCheaters(context, "inquisitor", "s_killed_revived", limit)
}

def achieveCheaters(ApplicationContext context, String f1, String f2, int limit = 0) {
    def jdbcTemplate = context.getBean(JdbcTemplate.class)
    def sql = """
with a as (
select coalesce(S.profile_id, A.profile_id::int) profile_id, $f1, $f2, abs($f1 - $f2) delta
from achieve.worms_achievements A 
left join wormswar.social_id S on A.profile_id = S.string_id
where $f1 != $f2 and A.profile_id not like '%_w%'
)
select row_number() over(ORDER BY delta desc) as i, * from a where not exists (select * from wormswar.ban_list B where B.profile_id = A.profile_id and end_date is null)
and delta > $limit
order by delta desc
"""
    "'№' 'profile_id' '$f1' '$f2' 'delta'\n" +
            "--------------------\n" +
            jdbcTemplate.queryForList(sql)
                    .collect { "${it.i}) UPID{${it.profile_id}} ${it[f1]} ${it[f2]} ${it.delta}" }
                    .join("\n")
}

def achieveCheaters(ApplicationContext context, String f1, int limit) {
    def jdbcTemplate = context.getBean(JdbcTemplate.class)
    def sql = """
with a as (
select coalesce(S.profile_id, A.profile_id::int) profile_id, $f1
from achieve.worms_achievements A 
left join wormswar.social_id S on A.profile_id = S.string_id
where $f1 > $limit and A.profile_id not like '%_w%'
)
select row_number() over(ORDER BY $f1 desc) as i, * from a where not exists (select * from wormswar.ban_list B where B.profile_id = A.profile_id and end_date is null)
order by $f1 desc
limit 1000
"""
    "'№' 'profile_id' '$f1'\n" +
            "--------------------\n" +
            jdbcTemplate.queryForList(sql)
                    .collect { "${it.i}) UPID{${it.profile_id}} ${it[f1]}" }
                    .join("\n")
}

def getDuelStat(ApplicationContext context, PrintWriter console, String date) {
//    def serverNetId = context.getBean(SocialService.class).socialServicesMap.keySet()
//            .collect { SocialServiceEnum.valueOf(it) }
//            .find { it.mobileOS }
//            .type
    def serverNetId = 12
    def (String cmd, String sourceDir, String fileName, String dt) = applyForPvpDetails(date)
    def method = "getDuelStat"
    def cmdPattern = """$cmd WAGER_15_DUEL #in# | jq -cr '(.turnCount|tostring) as \$turns | .participants | map(\$turns+" "+(.profile.profileId|tostring)+" "+(.profile.socialNetId|tostring)+" "+(.battleResult|tostring)) | .[]' """

    def dt0 = LocalDate.parse(dt).minusDays(1).toString()
    def dt1 = LocalDate.now().toString() != dt ? LocalDate.parse(dt).plusDays(1).toString() : null
    String out = "METHOD{ }{INDEX}:  METHOD{$method $dt0}{$dt0} $dt "
    if (dt1) out += "METHOD{$method $dt1}{$dt1}"

    out += "\n\n"
    Map<Integer, DuelStat> statMap = [:]
    parseFile(context, console, sourceDir, fileName, cmdPattern)
            .split("\n")
            .findAll { it }
            .collect { it.split(" ") }
            .forEach { ss ->
                int turns = ss[0] as int
                int profileId = ss[1] as int
                int socialNetId = ss[2] as int
                int result = ss[3] as int
                if (result > 1 || serverNetId != socialNetId) return
                if (!statMap.containsKey(profileId)) {
                    statMap.put(profileId, new DuelStat())
                }
                statMap.get(profileId).push(result, turns)
            }
    statMap.entrySet()
            .findAll { it.value.battles() > 10 }
            .sort { it.value.battles() }.reverse()
            .forEach {
                int profileId = it.key
                def duelStat = it.value
                def wins = duelStat.winTurns.size()
                def defeats = duelStat.defeatTurns.size()
                def battles = duelStat.battles()
                def turnsMed = DuelStat.median(duelStat.totalTurns)
                def winTurnsMed = DuelStat.median(duelStat.winTurns)
                def defTurnsMed = DuelStat.median(duelStat.defeatTurns)
                if (turnsMed < 2) {
                    out += String.format("BATTLES{%s ${date && LocalDate.now().toString() != date ? date : 'today'}} %s %s/%s turns[Med]:%s %s/%s \n", profileId, battles, wins, defeats, turnsMed, winTurnsMed, defTurnsMed)
                }
            }
    return out
}

class DuelStat {
    List<Integer> winTurns = []
    List<Integer> defeatTurns = []
    List<Integer> totalTurns = []

    def push(int result, int turns) {
        if (result == 0) {
            winTurns.add(turns)
        } else {
            defeatTurns.add(turns)
        }
        totalTurns.add(turns)
    }

    int battles() {
        totalTurns.size()
    }

    static int median(List<Integer> list) {
        return list ? list.sort().get((int) (list.size() / 2)) : 0
    }
}

class ChatLog {
    public int battleId
    public List<ChatMessage> chatLog
}

class ChatMessage {
    public int battleId
    public String date
    public int profileId
    public String message
//    public boolean teamsMessage
}

def findInPvpChat(ApplicationContext context, PrintWriter console, String date, String str) {
    def (String cmd, String sourceDir, String fileName, String dt) = applyForPvpDetails(date)
//    def cmdPattern = """zgrep chatLog #in# | grep '$str' | jq -c '{chatLog}'"""
    def cmdPattern = """zgrep chatLog #in# | jq -c '{battleId, chatLog}'"""

    if (!str || str == 'null') {
        str = '-'
    }

//    def dt0 = LocalDate.parse(dt).minusDays(1).toString()
//    def dt1 = LocalDate.now().toString() != dt ? LocalDate.parse(dt).plusDays(1).toString() : null
//    String out = "METHOD{ }{INDEX}: /$str/ METHOD{findInPvpChat $dt0 $str}{$dt0} $dt "
//    if (dt1) out += "METHOD{findInPvpChat $dt1 $str}{$dt1}"

    def out = "Строка поиска: '$str'\n\n"
    if (str.length() < 3) {
        return out + "Ошибка: минимальная строка для поиска - 3 символа"
    }
    def mapper = new ObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    out += parseFile(context, console, sourceDir, fileName, cmdPattern).split("\n")
            .findAll { it }
            .collect {
                try {
                    return mapper.readValue(it, ChatLog.class)
                } catch (Exception e) {
                    return null
                }
            }.findAll { it && it.chatLog }
            .collect { it -> it.chatLog.forEach { log -> log.battleId = it.battleId }; return it }
            .collectMany { it.chatLog }
            .findAll { it.message.toLowerCase().contains(str.toLowerCase()) }
            .collect { "${it.date.split()[1]} [BATTLES{${it.profileId} ${LocalDate.now().toString() != dt ? dt : 'today'}} ${it.battleId}] ${it.message.replace("\n", " ")}" }
            .join("\n")
    out
}

@Field
static List<String> battleTypes = [
        'WAGER_15_DUEL',
        'WAGER_50_2x2',
        'WAGER_50_2x2_FRIENDS',
        'MERCENARIES_DUEL',
        'GLADIATOR_DUEL',
        'WAGER_50_3_FOR_ALL',
        'ZOMBIE_RISE_PARTNER',
        'ZOMBIE_RISE_FRIEND',
        'PvE_PARTNER',
        'PvE_FRIEND',
        'ROPE_RACE_QUEST',
        'FRIEND_ROPE_RACE_QUEST',
        'FRIEND_ROPE_RACE',
        'NO_WAGER',
]

@Field
static List<String> battleTypesMobile = [
        'WAGER_15_DUEL',
        'WAGER_50_DUEL',
        'WAGER_50_2x2',
        'WAGER_50_2x2_FRIENDS',
        'PvE_PARTNER',
        'PvE_FRIEND',
        'NO_WAGER',
]

private static String getProfileName(PvpService pvpService, ClanServiceImpl clanService, UserProfile profile) {
    if (profile.name) {
        return profile.name
    }
    ClanMember member = clanService.getClanMember((short) profile.socialId, (int) profile.profileId)
    return member != null ? member.name : pvpService.userIdToNameMap.get(PvpService.getPvpUserId(profile.profileId, profile.socialId)) ?: ""
}

private static String getProfileSocialName(PvpService pvpService, ClanServiceImpl clanService, UserProfile profile) {
    ClanMember member = clanService.getClanMember((short) profile.socialId, (int) profile.profileId)
    return member != null ? member.name : pvpService.userIdToNameMap.get(PvpService.getPvpUserId(profile.profileId, profile.socialId)) ?: ""
}

@Field
static Map<Integer, Map<Short, String>> platformsByServer = [
        99: [
                0: "VK, OK, Mail",
                1: "VK",
                2: "OK",
                3: "Mail",
        ],
        11: [
                0 : "Android, iOS",
                11: "Android",
                14: "iOS",
        ],
        14: [
                0 : "Android, iOS",
                11: "Android",
                14: "iOS",
        ],
        12: [
                0: "Steam",
        ]
]

private static boolean isMobileServer(int serverId) {
    return serverId == 11 || serverId == 12 || serverId == 14 || serverId == 103
}

private static String platformView(int socialNetId) {
    switch (socialNetId) {
        case 1: return "VK"
        case 2: return "OK"
        case 3: return "Mail"
        case 11: return "Android"
        case 12: return "Steam"
        case 14: return "iOS"
        default: return ""
    }
}

def getWinsCountTop(ApplicationContext context, int serverId, String forDate_p, String platform_p, String battleWager_p, String teamSize_p, String topSize_p) {
    def profileService = context.getBean(ProfileService.class)
    def pvpService = context.getBean(PvpService.class)
    def clanService = context.getBean(ClanServiceImpl.class)

    LocalDate forDate = forDate_p ? LocalDate.parse(forDate_p) : LocalDate.now().minusDays(1)
    BattleWager battleWager = battleWager_p ? BattleWager.valueOf(battleWager_p) : BattleWager.WAGER_15_DUEL
    int teamSize = (teamSize_p ?: 0) as int
    int topSize = (topSize_p ?: 50) as int
    short platform = (platform_p ?: 0) as short

    Map<Integer, String> platforms = platformsByServer.getOrDefault(serverId, [0: "Все"])
    int i = 1
    def tbody = pvpDetailsLogForDate(forDate)
            .findAll { line -> line.contains(battleWager.name()) }
            .findAll { line -> line.contains(",\"battleResult\":0,") }
            .collect { CdrLogHelper.toPvpBattleLogRecord(it) }
            .findAll { it -> it.wager_raw == battleWager }
            .collectMany { it -> it.participants.findAll { it.battleResult == PvpBattleResult.WINNER } }
            .findAll { teamSize ? it.profile.units.size() == teamSize : true }
            .collect { it.profile }
            .findAll { platform ? it.socialNetId == platform : true }
            .groupBy { it.profileId }
            .sort { a, b -> b.value.size() <=> a.value.size() }
            .take(topSize ?: Integer.MAX_VALUE)
            .collect {
                Long profileId = it.key
                def userProfile = profileService.getUserProfile(profileId)
                String name = userProfile ? getProfileName(pvpService, clanService, userProfile) : ""
                def profile = it.value[0]
                def socialNetId = profile.socialNetId
                [i++, platformView(socialNetId), [socialNetId, profileId], name, profile.level, it.value.size()]
            }

    AdminHandler.xstream.toXML(
            [
                    contentType: 'table',
                    title      : "ТОП ${topSize ?: ''} по количеству побед" as String,
                    form       : [
                            method: 'getWinsCountTop',
                            args  : [
                                    [
                                            label: 'Дата',
                                            value: java.sql.Date.valueOf(forDate),
                                            type : 'jqueryPicker:date'
                                    ],
                                    [
                                            label: 'Платформа',
                                            value: platform,
                                            type : 'select',
                                            from : platforms
                                    ],
                                    [
                                            label: 'Тип боя',
                                            value: battleWager.name(),
                                            type : 'select',
                                            from : isMobileServer(serverId) ? battleTypesMobile : battleTypes
                                    ],
                                    [
                                            label: 'Размер команды',
                                            value: teamSize,
                                            type : 'select',
                                            from : [0, 1, 2, 3, 4]
                                    ],
                                    [
                                            label: 'Размер списка',
                                            value: topSize,
                                            type : 'select',
                                            from : [0, 50, 100]
                                    ],
                            ]
                    ],
                    thead      : [
                            [
                                    title: '№',
                            ],
                            [
                                    title: '',
                            ],
                            [
                                    type: 'UPIDX',
                            ],
                            [
                                    title: 'Ник',
                                    align: 'left'
                            ],
                            [
                                    title: 'Уровень',
                            ],
                            [
                                    title: 'Кол-во побед',
                            ],
                    ],
                    tbody      : tbody
            ]
    )
}

protected static List<String> pvpDetailsLogForDate(LocalDate forDate) {
    def file = new File("logs/cdr/pvp/pvp-details-${forDate.toString()}.log")
    if (file.exists()) {
        return FileUtils.readLines(file, StandardCharsets.UTF_8)
    }
    def cdrPvpDir = "logs/cdr/cdr-pvp"
    if (Files.notExists(Paths.get(cdrPvpDir))) {
        cdrPvpDir = "logs/cdr"
    }
    def command
    if (forDate == LocalDate.now()) {
//        command = ["bash", "-c", "grep '${regex}' ${cdrPvpDir}/pvp/pvp-details.log | jq -c '${jqFilter}'"]
        command = ["bash", "-c", "cat ${cdrPvpDir}/pvp/pvp-details.log"]
    } else {
//        command = ["bash", "-c", "zgrep '${regex}' ${cdrPvpDir}/pvp/pvp-details-${forDate}.log.zip | jq -c '${jqFilter}'"]
        command = ["bash", "-c", "zcat ${cdrPvpDir}/pvp/pvp-details-${forDate}.log.zip"]
    }
    def proc = command.execute()
    List<String> result = []
    proc.in.eachLine { row -> result.add(row) }
    return result
}

static def bossBattleExtraRewardTop(ApplicationContext context, Map params) {
    def profileService = context.getBean(ProfileService.class)
    def pvpService = context.getBean(PvpService.class)
    def clanService = context.getBean(ClanServiceImpl.class)
    def jdbcTemplate = context.getBean(JdbcTemplate.class)

    int award_type = 36
    int serverId = params.ctx as int

    LocalDate forDate = params.arg1 ? LocalDate.parse(params.arg1 as String) : LocalDate.now()
    int missionId = params.arg2 ? params.arg2 as int : 0
    int awardId = params.arg3 ? params.arg3 as int : 0

    String sql = """
        select profile_id, note from wormswar.award_statistic where award_type = ${award_type} and date >= ? and date < ?
    """

    def result = jdbcTemplate
            .queryForList(sql, forDate, forDate.plusDays(1))
            .findAll { missionId ? (it.note as String).startsWith("missionId=${missionId}, ") : true }
            .findAll { awardId ? (it.note as String).contains(", id=${awardId} ") : true }
            .groupBy { it.profile_id }
            .entrySet()
            .sort { o1, o2 -> o2.value.size() <=> o1.value.size() }

    Set<Integer> rewards = [0]
    Set<Integer> missions = [0]

    jdbcTemplate.query("select id, mission_id from wormswar.boss_battle_extra_reward", new RowCallbackHandler() {
        @Override
        void processRow(ResultSet res) throws SQLException {
            rewards.add(res.getInt("id"))
            missions.add(res.getInt("mission_id"))
        }
    })

    List tbody = []
    int i = 0;
    result.forEach {
        def profile = profileService.getUserProfile(it.key)
        def socialName = getProfileSocialName(pvpService, clanService, profile)
        socialName = socialName != profile.name ? socialName : ""
        TreeSet<Integer> mission_sx = []
        TreeSet<Integer> award_sx = []
        it.value.forEach { item ->
            def ss = (item.note as String).replace(",", "").split(" ")
            mission_sx.add((ss[0] - "missionId=") as int)
            award_sx.add((ss[1] - "id=") as int)
        }
        tbody.add([++i, profile.id, profile.level, profile.name, socialName, mission_sx.join(", "), award_sx.join(", "), it.value.size()])
    }

    AdminHandler.xstream.toXML(
            [
                    contentType: 'table',
                    title      : "ТОП по дополнительным наградам" as String,
                    form       : [
                            method: 'bossBattleExtraRewardTop',
                            args  : [
                                    [
                                            label: 'Дата',
                                            value: java.sql.Date.valueOf(forDate),
                                            type : 'jqueryPicker:date'
                                    ],
                                    [
                                            label: 'ID Босса',
                                            type : 'select',
                                            from : missions.sort(),
                                            value: missionId,
                                    ],
                                    [
                                            label: 'ID награды',
                                            type : 'select',
                                            from : rewards.sort(),
                                            value: awardId,
                                    ],
                            ]
                    ],
                    thead      : [
                            [
                                    title: '№',
                            ],
                            [
                                    title: '',
                                    type : 'UPID',
                            ],
                            [
                                    title: 'Уровень',
                            ],
                            [
                                    title: 'Имя',
                                    align: 'left'
                            ],
                            [
                                    title: 'Ник',
                                    align: 'left'
                            ],
                            [
                                    title: 'Боссы',
                            ],
                            [
                                    title: 'Награды',
                            ],
                            [
                                    title: 'Кол-во',
                            ],
                    ],
                    tbody      : tbody
            ]
    )
}


enum Method {
    doWork,
    bossInfo,
    statBossSimple,
    sendMessage,
    setHeroicMissionState,
    statBossCoop,
    statBossSuper,
    getBundlesStat,
    copyEvents,
    copyPvpBattles,
    copyBattles,
    copyConfigs,
    getProfiles,
    getFbPaymentDetails,
    getHeroicMissionLevelsAndMaps,
    getReagentStats,
    getWeaponStats,
    getStuffStats,
    getProfileSessions,
    getProfileEvents,
    getLoginsByIp,
    getAllLoginsByIp,
    getAllLoginsByID,
    getLoginsByDeviceId,
    getRaceStats,
    getEquipWagerStats,
    getRefsList,
    getCheatProfileSessions,
    getFarmersTop,
    getLongestSessions,
    winBossProfiles,
    winPveBossProfiles,
    link_android,
    link_ios,
    link,
    unLink,
    getAdsCheaters,
    getAdsCheatersIn,
    useWeapon,
    findProfileByOrderId,
    findProfileByVkId,
    findProfileByFbId,
    achieveCheatersGraves,
    achieveCheatersInquisitor,
    achieveCheaters,
    newRegs,
    payments,
    moveProfile,
    getSpeedupLevel,
    setSkill,
    addToGroup,
    getDuelStat,
    findInPvpChat,
    getWinsCountTop,
    bossBattleExtraRewardTop,    
}

def callMethod(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def index = """
        METHOD{getProfileSessions 1234567890 ${LocalDate.now()}}{Сессии игрока}
        METHOD{getProfileEvents 1234567890 ${LocalDate.now()}}{Действия игрока}
        METHOD{getLoginsByIp 127.0.0.1 ${LocalDate.now()}}{Логины по IP}
        METHOD{getCheatProfileSessions ${LocalDate.now()} ${context.getBean(AppParams.class).versionAsString}}{Читерские сессии}
        METHOD{getProfiles}{Список активных профилей (для теста)}

        METHOD{getFbPaymentDetails 1234567890}{Информация по платежу Facebook}
        
        METHOD{getDuelStat ${LocalDate.now()}}{Кандитаты в флагователи}

        METHOD{getRaceStats ${LocalDate.now()} ALL 0}{Статистика по использованию рас}
        METHOD{getEquipWagerStats ${LocalDate.now()} ALL 0 0}{Статистика по использованию шапки/артефакта}
        
        METHOD{getFarmersTop ${LocalDate.now()}}{ТОП по фузам в миссиях}
        METHOD{getLongestSessions ${LocalDate.now()}}{ТОП по длине сессии}
        METHOD{getWinsCountTop ${LocalDate.now()}}{ТОП по количеству побед}
        METHOD{bossBattleExtraRewardTop ${LocalDate.now()} 0 0}{ТОП по дополнительным наградам}
        
        METHOD{getRefsList ${LocalDate.now()}}{Реферальные ссылки}
       
        METHOD{bossInfo}{Информация по супер боссам (за сегодня)}
        Статистика по боссам: METHOD{statBossSimple ${LocalDate.now().minusDays(1)}}{Простые}  METHOD{statBossCoop ${LocalDate.now().minusDays(1)}}{Совместные} METHOD{statBossSuper ${LocalDate.now().minusDays(1)}}{Супер}
        METHOD{getBundlesStat ${LocalDate.now()}}{Статистика по продажам наборов}

        METHOD{getReagentStats 50 100 ${LocalDate.now().plusDays(1)}}{Статистика реагентов по уровням}
        METHOD{getWeaponStats 25 ${LocalDate.now().plusDays(1)}}{Статистика оружия по уровням}
        METHOD{getStuffStats 1081 ${LocalDate.now().plusDays(1)} 30 30}{Статистика шапок по уровням}
        
        METHOD{copyConfigs}{Копировать конфиги}
        METHOD{copyEvents ${LocalDate.now().minusDays(1)}}{Копировать лог (эвенты)}
        METHOD{copyPvpBattles ${LocalDate.now().minusDays(1)}}{Копировать лог (PVP бои)}
        METHOD{copyBattles ${LocalDate.now().minusDays(1)}}{Копировать лог (PVE боссы)}
        METHOD{getHeroicMissionLevelsAndMaps}{Настройки СуперБоссов}
        """
    def params = new InteropSerializer().fromString(request.scriptParams, Map.class);
    Method method = null
    try {
        method = Method.valueOf(params.method as String)
    } catch (Exception ignored) {
        return index
    }
    Set<String> roles = params.roles ? (params.roles as String).split(",") as Set : Collections.emptySet()
    def restricted = [
            (Method.moveProfile)          : "SUPER_USER",
            (Method.link_android)         : "SUPER_USER",
            (Method.link_ios)             : "SUPER_USER",
            (Method.link)                 : "SUPER_USER",
            (Method.unLink)               : "SUPER_USER",
            (Method.setHeroicMissionState): "SUPER_USER",
            (Method.setSkill)             : "SUPER_USER",
    ]
    def restrictedFor = restricted.get(method)
    if (restrictedFor && !roles.contains(restrictedFor)) {
        return "ERROR Для вызова '${method}' не хватает прав!\n" + index
    }
    switch (method) {
        case Method.setHeroicMissionState: return setHeroicMissionState(context, params.arg1 as int, params.arg2 as String, params.arg3 as int)
        case Method.bossInfo: return getHeroicMissionDailyProgress(context)
        case Method.statBossSimple: return getSimpleBossStat(context, console, params.arg1 as String, params.arg2 as String)
        case Method.statBossCoop: return getCooperativeBossStat(context, console, params.arg1 as String, params.arg2 as String)
        case Method.statBossSuper: return getSuperBossStat(context, console, params.arg1 as String, params.arg2 as String)
        case Method.getBundlesStat: return getBundlesStat(context, console, params.arg1 as String, params.arg2 as String)
        case Method.copyEvents: return copyEvents(context, console, params.arg1 as String, params.arg2 as String)
        case Method.copyPvpBattles: return copyPvpBattles(context, console, params.arg1 as String, params.arg2 as String)
        case Method.copyBattles: return copyBattles(context, console, params.arg1 as String, params.arg2 as String)
        case Method.copyConfigs: return copyConfigs(context, console)
        case Method.getProfiles: return getProfiles(context, console)
        case Method.getFbPaymentDetails: return getFbPaymentDetails(context, console, params.arg1 as Long)
        case Method.getHeroicMissionLevelsAndMaps: return getHeroicMissionLevelsAndMaps(context, console)
        case Method.getReagentStats: return getReagentStats(context, console, params.arg1 as int, params.arg2 as int, params.arg3 as String)
        case Method.getWeaponStats: return getWeaponStats(context, console, params.arg1 as int, params.arg2 as String)
        case Method.getStuffStats: return getStuffStats(context, console, params.arg1 as short, params.arg2 as String, params.arg3 as Integer ?: 1, params.arg4 as Integer ?: 30)
        case Method.getProfileSessions: return getProfileSessions(context, console, params.arg1 as int, params.arg2 as String)
        case Method.getProfileEvents: return getProfileEvents(context, console, params.arg1 as int, params.arg2 as String)
        case Method.getLoginsByIp: return getLoginsByIp(context, console, params.arg1 as String, params.arg2 as String)
        case Method.getAllLoginsByIp: return getAllLoginsByIp(context, console, params.arg1 as String)
        case Method.getRefsList: return getRefsList(context, console, params.arg1 as String)
        case Method.getCheatProfileSessions: return getCheatProfileSessions(context, console, params.arg1 as String, params.arg2 as String)
        case Method.getFarmersTop: return getFarmersTop(context, console, params.arg1 as String)
        case Method.getLongestSessions: return getLongestSessions(context, console, params.arg1 as String)
        case Method.achieveCheatersGraves: return achieveCheatersGraves(context, (params.arg1 as Integer) ?: 0)
        case Method.achieveCheatersInquisitor: return achieveCheatersGraves(context, (params.arg1 as Integer) ?: 0)
        case Method.achieveCheaters: return achieveCheaters(context, params.arg1 as String, params.arg2 as int)
        case Method.getRaceStats: return getRaceStats(context, console, params.arg1 as String, params.arg2 as String, params.arg3 as int)
        case Method.getEquipWagerStats: return getEquipWagerStats(context, console, params.arg1 as String, params.arg2 as String, params.arg3 as int, params.arg4 as int)
        case Method.getDuelStat: return getDuelStat(context, console, params.arg1 as String)
        case Method.findInPvpChat: return findInPvpChat(context, console, params.arg1 as String, params.arg2 as String)
        case Method.moveProfile: return moveProfile(context, request.adminUser, params.arg1 as int, params.arg2 as String, params.arg3 as int)
        case Method.doWork: return doWork(context, request, console)
        case Method.getWinsCountTop: return getWinsCountTop(context, params.ctx as int, params.arg1 as String, params.arg2 as String, params.arg3 as String, params.arg4 as String, params.arg5 as String)
        case Method.bossBattleExtraRewardTop: return bossBattleExtraRewardTop(context, params)
    }
}