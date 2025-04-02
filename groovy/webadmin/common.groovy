import com.pragmatix.app.common.BattleResultEnum
import com.pragmatix.app.common.Race
import com.pragmatix.app.dao.BossBattleExtraRewardDao
import com.pragmatix.app.domain.BossBattleExtraRewardEntity
import com.pragmatix.app.init.StuffCreator
import com.pragmatix.app.model.UserProfile
import com.pragmatix.app.services.*
import com.pragmatix.clanserver.domain.ClanMember
import com.pragmatix.clanserver.services.ClanServiceImpl
import com.pragmatix.gameapp.GameApp
import com.pragmatix.pvp.services.PvpService
import com.pragmatix.webadmin.AdminHandler
import com.pragmatix.webadmin.CdrLogHelper
import com.pragmatix.webadmin.model.BossBattleLogRecord
import com.pragmatix.wormix.webadmin.interop.InteropSerializer
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest
import org.apache.commons.io.FileUtils
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

//== bossBattleExtraReward ==

static def bossBattleExtraReward(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def transactionTemplate = context.getBean(TransactionTemplate.class)
    def service = context.getBean(BossBattleExtraRewardService.class)
    def dao = context.getBean(BossBattleExtraRewardDao.class)

    Map<String, Object> params = AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, Object>
    String action = params.action as String
    if (action == "insert") {
        def entity = bossRewardEntityFromParams(params)
        transactionTemplate.execute { dao.insert(entity) }
        service.loadReward()
    } else if (action == "modify") {
        def entity = bossRewardEntityFromParams(params)
        transactionTemplate.execute { dao.update(entity) }
        service.loadReward()
    } else if (action == "remove") {
        transactionTemplate.execute { dao.deleteById(params.id as int) }
        service.loadReward()
    }
    def result = dao.getAllList().collect {
        [
                id       : it.id,
                start    : it.start ? it.start.truncatedTo(ChronoUnit.SECONDS).toString() : "",
                finish   : it.finish ? it.finish.truncatedTo(ChronoUnit.SECONDS).toString() : "",
                missionId: it.missionId,
                levelFrom: it.levelFrom,
                levelTo  : it.levelTo,
                archive  : it.archive,
                chance   : it.chance,
                realMoney: it.realMoney,
                money    : it.money,
                reaction : it.reaction,
                reagents : it.reagents,
                weapons  : it.weapons,
                stuff    : it.stuff,
        ]
    }
    return AdminHandler.xstream.toXML([
            result: result,
    ])
}

protected static BossBattleExtraRewardEntity bossRewardEntityFromParams(Map<String, Object> params) {
    def entity = new BossBattleExtraRewardEntity()
    entity.id = params.id ? params.id as int : null
    entity.start = params.start ? LocalDateTime.parse(params.start as String) : null
    entity.finish = params.finish ? LocalDateTime.parse(params.finish as String) : null
    entity.missionId = params.missionId as short
    entity.levelFrom = params.levelFrom as short
    entity.levelTo = params.levelTo as short
    entity.archive = params.archive as boolean
    entity.chance = params.chance as int
    entity.realMoney = params.realMoney as int
    entity.money = params.money as int
    entity.reaction = params.reaction as int
    entity.reagents = params.reagents as String
    entity.weapons = params.weapons as String
    entity.stuff = params.stuff as String
    return entity
}


//== findByName ==

static def findByName(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def jdbcTemplate = context.getBean(JdbcTemplate.class);
    def gameApp = context.getBean(GameApp.class)
    def profileService = context.getBean(ProfileService.class)
    def banService = context.getBean(BanService.class)
    def stuffService = context.getBean(StuffService.class)
    def pvpService = context.getBean(PvpService.class)
    def clanService = context.getBean(ClanServiceImpl.class)

    Map<String, Object> params = AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, Object>

    String searchPhrase = params.searchPhrase as String ?: ""

    def from = """
         FROM wormswar.user_profile UP
                  left join wormswar.creation_date using(id)
"""
    Set<String> where = []
    def sqlParams = []
    String nameTable = "UP"
    if (params.searchInClans) {
        from += " left join clan.clan_member CM on UP.id = CM.profile_id "
        nameTable = "CM"
    }
    if (searchPhrase.any { it == "*" }) {
        where.add("length(${nameTable}.name) >= ?")
        sqlParams.add(searchPhrase.length())
    } else {
        String sqlPattern = '%' + searchPhrase.toLowerCase().replaceAll('[%_]', '\\\\$0') + '%'
        where.add("LOWER(${nameTable}.name) LIKE ?")
        sqlParams.add(sqlPattern)
    }
    if (params.levelFrom) {
        where.add("level >= ?")
        sqlParams.add(params.levelFrom as int)
    }
    if (params.levelTo) {
        where.add("level <= ?")
        sqlParams.add(params.levelTo as int)
    }
    if (params.moneyFrom) {
        where.add("money >= ?")
        sqlParams.add(params.moneyFrom as int)
    }
    if (params.moneyTo) {
        where.add("money <= ?")
        sqlParams.add(params.moneyTo as int)
    }
    if (params.realMoneyFrom) {
        where.add("realmoney >= ?")
        sqlParams.add(params.realMoneyFrom as int)
    }
    if (params.realMoneyTo) {
        where.add("realmoney <= ?")
        sqlParams.add(params.realMoneyTo as int)
    }
    if (params.ratingFrom) {
        where.add("rating >= ?")
        sqlParams.add(params.ratingFrom as int)
    }
    if (params.ratingTo) {
        where.add("rating <= ?")
        sqlParams.add(params.ratingTo as int)
    }
    if (params.donater == 1) {
        where.add("last_payment_date is not null")
    } else if (params.donater == 0) {
        where.add("last_payment_date is null")
    }
    if (params.regTo) {
        where.add("creation_date <= ?")
        sqlParams.add(params.regTo as Date)
    }
    if (params.loginTo) {
        where.add("UP.last_login_time <= ?")
        sqlParams.add(params.loginTo as Date)
    }
    if (params.currentMission) {
        where.add("current_mission = ?")
        sqlParams.add(params.currentMission as short)
    }
    if (params.currentNewMission) {
        where.add("current_new_mission = ?")
        sqlParams.add(params.currentNewMission as short)
    }

    long start = System.currentTimeMillis()

    long total = (params.total ?: jdbcTemplate.queryForObject("SELECT count(*) ${from} WHERE ${where.join(" AND ")}", Long.class, sqlParams as Object[])) as long

    int limit = (params.limit ?: 100) as int
    int offset = (params.offset ?: 0) as int

    sqlParams.addAll([limit, offset])
    def result = jdbcTemplate.queryForList("SELECT UP.id, creation_date ${from} WHERE ${where.join(" AND ")} order by UP.id LIMIT ? OFFSET ?", sqlParams as Object[])

    def profiles = result.
            collect { it.put("profile", profileService.getUserProfile(it.id as long)); it }
            .findAll { it.profile != null }
            .findAll { params.race >= 0 && (it.profile as UserProfile).wormStructure ? (it.profile as UserProfile).wormStructure.race == params.race as byte : true }
            .findAll { params.stuffId > 0 ? stuffService.isExist(it.profile as UserProfile, params.stuffId as short, false) : true }
            .collect {
                def profile = it.profile as UserProfile
                profileService.getUserProfileStructure(profile)

                short socialNetId = profileService.getShortSocialIdFor(profile)
                ClanMember member = clanService.getClanMember(socialNetId, (int) profile.id)
                // социальное имя берётся из кланов/pvp (если есть)
                def socialName = member != null ? member.name : pvpService.userIdToNameMap.get(PvpService.getPvpUserId(profile.id, socialNetId as byte)) ?: null
                [
                        profileId        : profile.id,
                        profileName      : profile.name,
                        socialName       : socialName,
                        level            : profile.level,
                        rating           : profile.rating,
                        money            : profile.money,
                        realMoney        : profile.realMoney,
                        online           : profile.online,
                        creationTime     : it.creation_date,
                        lastLoginTime    : profile.lastLoginTime,
                        currentMission   : profile.currentMission,
                        currentNewMission: profile.currentNewMission,
                        race             : profile.wormStructure.race,
                        donater          : profile.lastPaymentTime > 0,
                        banned           : banService.isBanned(profile.id),
                ]
            }

    return AdminHandler.xstream.toXML([
            "total"      : total,
            "skipped"    : (result.size() - profiles.size()),
            "profiles"   : profiles,
            "processTime": System.currentTimeMillis() - start,
    ])
}

//== bossBattleStats ==

static def bossBattleStats(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    long start = System.currentTimeMillis()

    def stuffCreator = context.getBean(StuffCreator.class)

    Map<String, Object> params = AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, Object>

    LocalDate forDate = LocalDate.parse(params.forDate as String)
    int fromHour = params.fromHour ? params.fromHour as int : 0
    int toHour = params.toHour ? params.toHour as int : 23

    boolean allOfMissionIds = params.allOfMissionIds
    boolean allOfWeapons = params.allOfWeapons
    boolean allOfRaces = params.allOfRaces
    boolean allOfHats = params.allOfHats
    boolean allOfKits = params.allOfKits
    String bestBy = params.bestBy
    Set<String> missionIds = params.missionIds ? params.missionIds as Set : []
    Set<Integer> weapons = params.weapons ? params.weapons as Set : []
    Set<Race> races = params.races ? params.races.collect { Race.valueOf(it as int) } as Set : []
    Set<Short> hats = params.hats ? params.hats as Set : []
    Set<Short> kits = params.kits ? params.kits as Set : []

    List<BossBattleLogRecord> logsRecords = []

    def pveMissions = missionIds
            .findAll { it.contains("_") || Integer.valueOf(it) > 100 }
            .collect { it.replace("_", ",") }
            .collect { ",\"missionIds\":[$it]," as String } as Set
    if (pveMissions || missionIds.isEmpty()) {
        pvpDetailsLogForDate(forDate)
                .findAll { line ->
                    (missionIds.isEmpty() && line.contains("missionIds") && !line.contains(",\"missionIds\":[]," as String))
                            || (!missionIds.isEmpty() && pveMissions.any { line.contains(it) })
                }
                .findAll { line -> line.contains(",\"battleResult\":0,") }
                .collect { CdrLogHelper.toPvpBattleLogRecord(it) }
                .forEach { record ->
                    record.participants
                            .collect { participant -> BossBattleLogRecord.valueOf(record, participant) }
                            .findAll { it }
                            .forEach { logsRecords.add(it) }
                }
    }
    def simpleMissions = missionIds.findAll { !it.contains("_") && Integer.valueOf(it) < 100 }.collect { Short.valueOf(it) } as Set
    if (simpleMissions || missionIds.isEmpty()) {
        battlesStatLogForDate(forDate)
                .collect {
                    CdrLogHelper.toSimpleBattleLogRecord(forDate, it,
                            { result, missionId -> result == BattleResultEnum.WINNER && (missionIds.isEmpty() || simpleMissions.contains(missionId)) }
                    )
                }.findAll { it }
                .collect {//todo удалить со временем
                    def turns = it.missionLog().turns
                    if (turns && turns.first.turnNum == (short) 0) {
                        turns.removeFirst()
                    }
                    it
                }.collect { BossBattleLogRecord.valueOf(it) }
                .findAll { it }
                .forEach { logsRecords.add(it) }
    }
    logsRecords = logsRecords
            .findAll { it.win }
            .findAll { missionIds.isEmpty() || missionIds.contains(it.missionIds) }
            .findAll {
                if (!weapons) return true
                if (!it.usedWeapons) return false
                allOfWeapons ? it.usedWeapons.containsAll(weapons) : weapons.containsAll(it.usedWeapons)
            }
            .findAll {
                if (!races) return true
                allOfRaces ? it.usedRaces.containsAll(races) : races.containsAll(it.usedRaces)
            }
            .findAll {
                if (!hats) return true
                if (!it.usedHats) return false
                allOfHats ? it.usedHats.containsAll(hats) : hats.containsAll(it.usedHats)
            }
            .findAll {
                if (!kits) return true
                if (!it.usedKits) return false
                allOfKits ? it.usedKits.containsAll(kits) : kits.containsAll(it.usedKits)
            }
            .findAll { fromHour == 0 || it.finishBattleTime.getHour() >= fromHour }
            .findAll { toHour == 23 || it.finishBattleTime.getHour() <= toHour }

    def missionIdsSize = missionIds.size()
    if (allOfMissionIds && missionIdsSize > 1) {
        Map<Long, Set<String>> map = new HashMap<>()
        for (final def it in logsRecords) {
            map.computeIfAbsent(it.profileId, { key -> new HashSet<String>(missionIdsSize) })
                    .add(it.missionIds)
        }
        logsRecords = logsRecords.findAll { map.get(it.profileId).size() == missionIdsSize }
    }
    if (bestBy) {
        logsRecords = logsRecords.groupBy { it.missionIds + "_" + it.profileId }
                .values().collect { sx ->
            if (bestBy == "turnsCount") {
                sx.sort(true, { it.turnsCount })
            } else if (bestBy == "battleDuration") {
                sx.sort(true, { it.battleDuration })
            }
            sx.get(0)
        }
    }
    def result = logsRecords.collect {
        [
                battleId        : it.battleId,
                finishBattleTime: it.finishBattleTime.toLocalTime().truncatedTo(ChronoUnit.SECONDS).toString(),
                missionIds      : it.missionIds.split("_"),
                profileId       : it.profileId,
                socialNetId     : it.socialNetId,
                turnsCount      : it.turnsCount,
                battleDuration  : it.battleDuration.seconds,
                usedWeapons     : it.usedWeapons.sort(),
                usedRaces       : it.usedRaces.collect { it.shortType }.sort(),
                usedHats        : it.usedHats.sort().collect { [id: it, name: stuffCreator.getStuff(it).name] },
                usedKits        : it.usedKits.sort().collect { [id: it, name: stuffCreator.getStuff(it).name] },
                teamUnits       : it.teamUnits.collect { unit ->
                    [
                            id    : unit.id,
                            type  : unit.type.type,
                            level : unit.level,
                            armor : unit.armor,
                            attack: unit.attack,
                            exp   : unit.exp,
                            race  : unit.race.type,
                            hat   : unit.hat,
                            kit   : unit.kit,
                    ]
                }
        ]
    }

    AdminHandler.xstream.toXML([
            result     : result,
            processTime: System.currentTimeMillis() - start,
    ]
    )
}

protected static List<String> battlesStatLogForDate(LocalDate forDate) {
    def file = new File("logs/cdr/battles_stat-${forDate.toString()}.log")
    if (file.exists()) {
        return FileUtils.readLines(file, StandardCharsets.UTF_8)
    }
    def command
    if (forDate == LocalDate.now()) {
//        command = ["bash", "-c", "grep -P '${regex}' logs/cdr/battles_stat.log | grep -v ${exclude}"]
        command = ["bash", "-c", "cat logs/cdr/battles_stat.log"]
    } else {
//        command = ["bash", "-c", "zgrep -P '${regex}' logs/cdr/battles_stat-${forDate}.log.zip | grep -v ${exclude}"]
        command = ["bash", "-c", "zcat logs/cdr/battles_stat-${forDate}.log.zip"]
    }
    def proc = command.execute()
    List<String> result = []
    proc.in.eachLine { row -> result.add(row) }
    return result
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


def removeFromGroup(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> params = serializer.fromString(request.scriptParams, Map.class)

    def profileService = context.getBean(ProfileService.class)
    def groupService = context.getBean(GroupService.class)
    def gameApp = context.getBean(GameApp.class)


    def profile = profileService.getUserProfile(params.profileId as Long)
    def result = groupService.removeFromGroup(profile, params.teamMemberId as int)

    if (profile.online) {
        gameApp.sessions.get(profile)?.close()
    }
    AdminHandler.xstream.toXML([
            result: result.name(),
    ])
}

static def shiftHeroicMissionsState(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def start = LocalTime.now()
    def heroicMissionService = context.getBean(HeroicMissionService.class)
    def dailyRegistry = context.getBean(DailyRegistry.class)

    heroicMissionService.getDailyTask().runServiceTask()

    dailyRegistry.getStore().forEach {
        it.value.setSuccessedSuperBossMission(false)
    }

    def out = []
    var path = Paths.get("logs/syslog.log")
    Files.readAllLines(path).forEach {
        if (it.contains(" INFO ")) {
            var t = it.split(" ")[0]
            var tt = LocalTime.parse(t)
            if (tt.isAfter(start)) {
                out.add(it)
            }
        }
    }
    AdminHandler.xstream.toXML([
            consoleOut: out,
    ])
}
