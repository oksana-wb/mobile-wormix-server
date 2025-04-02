import com.fasterxml.jackson.databind.ObjectMapper
import com.pragmatix.achieve.common.AchieveUtils
import com.pragmatix.achieve.domain.ProfileAchievements
import com.pragmatix.achieve.domain.WormixAchievements
import com.pragmatix.achieve.services.AchieveCommandService
import com.pragmatix.app.achieve.AchieveAwardService
import com.pragmatix.app.common.AwardTypeEnum
import com.pragmatix.app.domain.BanEntity
import com.pragmatix.app.domain.PaymentStatisticEntity
import com.pragmatix.app.domain.TrueSkillEntity
import com.pragmatix.app.init.StuffCreator
import com.pragmatix.app.init.WeaponsCreator
import com.pragmatix.app.model.UserProfile
import com.pragmatix.app.services.*
import com.pragmatix.app.services.rating.RankService
import com.pragmatix.app.settings.AppParams
import com.pragmatix.clanserver.domain.ClanMember
import com.pragmatix.clanserver.services.ClanServiceImpl
import com.pragmatix.common.utils.AppUtils
import com.pragmatix.craft.domain.Reagent
import com.pragmatix.craft.services.CraftService
import com.pragmatix.gameapp.social.SocialService
import com.pragmatix.gameapp.social.SocialServiceEnum
import com.pragmatix.pvp.BattleWager
import com.pragmatix.pvp.BattleWager
import com.pragmatix.pvp.services.PvpService
import com.pragmatix.pvp.services.matchmaking.BlackListService
import com.pragmatix.webadmin.AdminHandler
import com.pragmatix.webadmin.ExecAdminScriptException
import com.pragmatix.wormix.webadmin.interop.InteropSerializer
import com.pragmatix.wormix.webadmin.interop.ServiceResult
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest
import com.pragmatix.wormix.webadmin.interop.response.structure.*
import com.pragmatix.wormix.webadmin.interop.response.structure.clan.Rank
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler

import java.nio.file.Files
import java.nio.file.Paths
import java.sql.ResultSet

def getUserProfile(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    UserProfile profile = getUserProfileFromParams(context, request.scriptParams)

    def profileService = context.getBean(ProfileService.class)
    def banService = context.getBean(BanService.class)
    def craftService = context.getBean(CraftService.class)
    def depositService = (context.getBeansOfType(DepositService.class).values() + null).first()
    def dailyRegistry = context.getBean(DailyRegistry.class);
    def userRegistry = context.getBean(UserRegistry.class)
    def rankService = context.getBeansOfType(RankService.class).size() == 1 ? context.getBean(RankService.class) : null
    def jdbcTemplate = context.getBean(JdbcTemplate.class);
    def restrictionService = context.getBean(RestrictionService.class)
    def reactionRateService = context.getBean(ReactionRateService.class)

    byte socialNetId = profileService.getShortSocialIdFor(profile) as byte

    def userProfileStructure = profileService.getUserProfileStructure(profile)

    UserProfileStructure profileStruct = new UserProfileStructure()

    profileStruct.id = profileService.getProfileSocialId(profile)
    profileStruct.profileId = profile.getId()
    profileStruct.profileName = profile.name ?: getProfileName(context, profile.getId(), profileService.getSocialIdForClan(profile) as byte, "")
    profileStruct.renameAct = profile.renameAct
    profileStruct.banned = banService.isBanned(profile.getId())
    profileStruct.money = userProfileStructure.money
    profileStruct.realMoney = userProfileStructure.realMoney
    profileStruct.rating = userProfileStructure.rating
    profileStruct.stuffIds = userProfileStructure.stuff
    profileStruct.temporalStuffs = userProfileStructure.temporalStuff
    profileStruct.reactionRate = userProfileStructure.reactionRate
    profileStruct.recipeIds = userProfileStructure.recipes
    profileStruct.reagentQuants = craftService.getReagentsForProfile(profile.getId()).values
    profileStruct.battlesCount = profile.battlesCount
    profileStruct.lastLoginTime = profile.lastLoginTime
    profileStruct.loginSequence = profile.loginSequence
    profileStruct.online = profile.online
    profileStruct.currentMission = profile.currentMission
    profileStruct.currentNewMission = profile.currentNewMission
    profileStruct.searchKeys = dailyRegistry.getSearchKeys(profile.getId())
    profileStruct.howManyPumped = dailyRegistry.getHowManyPumped(profile.getId())
    profileStruct.successedMission = dailyRegistry.isSuccessedMission(profile.getId())
    profileStruct.successedSuperBoss = dailyRegistry.isSuccessedSuperBossMission(profile.getId())
    profileStruct.dailyRatings = dailyRegistry.getDailyRatings(profile.getId())
    TrueSkillEntity skillEntity = profileService.getTrueSkillFor(profile)
    profileStruct.trueSkillMean = skillEntity.mean
    profileStruct.trueSkillDeviation = skillEntity.standardDeviation
    profileStruct.trueSkillRating = Math.round((profileStruct.trueSkillMean - profileStruct.trueSkillDeviation * (double) 3) * (double) 500)
    profileStruct.pvpBattles = skillEntity.battles
    profileStruct.locale = profile.getLocale().getType()

    try {
        profileStruct.registerDate = jdbcTemplate.queryForObject("SELECT creation_date FROM wormswar.creation_date WHERE id = ?", Date.class, profile.getId())
    } catch (Exception e) {
    }

    try {
        profileStruct.flashPlayerVersion = jdbcTemplate.queryForObject("SELECT flash_version FROM stat.profile_stat WHERE profile_id = ?", Short.class, profile.getId())
    } catch (Exception e) {
    }

    if (userProfileStructure.clanMember) {
        profileStruct.clanId = userProfileStructure.clanMember.clanId
        profileStruct.clanName = userProfileStructure.clanMember.clanName
        profileStruct.rank = Rank.valueOf(userProfileStructure.clanMember.rank.name())
    }

    // юниты
    profileStruct.wormsGroup = new WormStructure[userProfileStructure.wormsGroup.length]
    WormStructure masterWorm = null
    int i = 0;
    userProfileStructure.wormsGroup.each {
        WormStructure str = new WormStructure()
        str.ownerId = it.ownerStringId != null && !it.ownerStringId.isEmpty() ? it.ownerStringId : "" + it.ownerId
        str.ownerStringId = it.ownerStringId
        str.armor = it.armor
        str.attack = it.attack
        str.level = it.level
        str.experience = it.experience
        str.hatId = it.hat
        str.raceId = it.race
        str.skinId = it.skin
        str.kitId = it.kit
        str.teamMemberType = it.teamMemberType.getType()
        str.active = it.active
        str.name = it.name
        profileStruct.wormsGroup[i] = str
        if (it.ownerId == profile.id)
            masterWorm = str
        i++
    }
    profileStruct.extraGroupSlotsCount = userProfileStructure.extraGroupSlotsCount

    // рюкзак
    def backpack = [];
    profile.backpack.each {
        if (it.count != 0) {
            BackpackItemStructure str = new BackpackItemStructure()
            str.weaponId = it.weaponId
            str.count = it.count
            backpack.add(str)
        }
    }
    profileStruct.backpacks = backpack.toArray(new BackpackItemStructure[backpack.size()]);

// исторя банов
    DaoService daoService = context.getBean(DaoService.class)
    List<BanEntity> banEntities = daoService.getBanDao().selectBanEntities(profile.getId());
    profileStruct.bans = new BanItem[banEntities.size()]
    i = 0
    banEntities.each {
        BanItem item = new BanItem()
        item.startDate = it.date
        item.endDate = it.endDate
        item.note = it.note
        item.adminUser = it.admin
        item.type = it.type
        item.attachments = it.attachments

        profileStruct.bans[i] = item
        i++
    }

    Map blackListMaps = getBlackListMaps(context, profile, socialNetId)
    profileStruct.blackListCandidats = blackListMaps.candidats.size()
    profileStruct.personalBlackListSize = blackListMaps.blackList.size()

    def metaMap = [:]
    metaMap.masterWorm = masterWorm
    metaMap.version = AppParams.versionToString(profile.version)
    metaMap.races = com.pragmatix.app.common.Race.toList(profile.races).collect { it.type }
    metaMap.skins = profile.skins.collect { Math.abs(it) }
    metaMap.activeSkins = profile.skins.findAll { it.intValue() < 0 }.collect { Math.abs(it) }
    metaMap.nextSelectRaceTime = AppUtils.formatDateInSeconds(profileService.getNextSelectRaceTime(profile))
    try {
        metaMap.lastBeingComebackedTime = jdbcTemplate.queryForObject("SELECT last_comebacked_time FROM wormswar.user_profile_meta WHERE profile_id = ?", Date.class, profile.getId())
    } catch (ignored) {
    }
    metaMap.isAbandoned = userRegistry.isProfileAbandonded(profile.id)
    metaMap.rankPoints = profile.rankPoints
    metaMap.rank = rankService ? rankService.getPlayerRankValue(profile.rankPoints) as Integer : null
    metaMap.bestRank = rankService ? profile.bestRank as Integer : null
    metaMap.logoutTime = profile.logoutTime > 0 ? new Date(profile.logoutTime * 1000L) : null
    metaMap.lastPaymentTime = profile.lastPaymentTime > 0 ? new Date(profile.lastPaymentTime * 1000L) : null
    metaMap.vipExpiryTime = profile.vipExpiryTime
    metaMap.renameVipAct = profile.renameVipAct
    metaMap.deposits = depositService == null ? [] : depositService.getAllDepositsFor(profile).collect {
        [
                id             : it.id,
                moneyType      : MoneyType.valueOf(it.moneyType),
                dividendsByDays: it.dividendsByDays?.split(' '),
                startDate      : it.startDate,
                progress       : it.progress,
                lastPayDate    : it.lastPayDate,
                paidOff        : it.paidOff
        ]
    }
    metaMap.blocks = restrictionService.aggregateBlocks(restrictionService.getRestrictions(profile.id))
    metaMap.reactionLevel = reactionRateService.getReactionLevel(userProfileStructure.reactionRate)
    metaMap.bossWinAwardToken = dailyRegistry.getBossWinAwardToken(profile.getId())
    metaMap.wagerWinAwardToken = dailyRegistry.getWagerWinAwardToken(profile.getId())
    metaMap.levelUpTime = profile.levelUpTime

    def dailyRatings = profileStruct.dailyRatings
    metaMap.dailyRatings = [
            WAGER_15_DUEL: dailyRatings ? dailyRatings[BattleWager.WAGER_15_DUEL.ordinal()] : 0,
            WAGER_50_DUEL: dailyRatings ? dailyRatings[BattleWager.WAGER_50_DUEL.ordinal()] : 0,
            WAGER_50_2x2 : dailyRatings ? dailyRatings[BattleWager.WAGER_50_2x2.ordinal()] : 0,
    ]

    profileStruct.meta = AdminHandler.xstream.toXML(metaMap)

    console.print("OK")

    return new InteropSerializer().toString(profileStruct)
}

private String getProfileName(ApplicationContext context, long profileId, byte socialNetId, String defaultName) {
    PvpService pvpService = context.getBean(PvpService.class)
    ClanServiceImpl clanService = context.getBean(ClanServiceImpl.class)
    ClanMember member = clanService.getClanMember((short) socialNetId, (int) profileId)
    return member != null ? member.name : pvpService.userIdToNameMap.get(PvpService.getPvpUserId(profileId, socialNetId)) ?: defaultName
}


def getShopStatistic(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def params = AdminHandler.xstream.fromXML(request.scriptParams) as Map
    def profile = getUserProfile(context, params.profileId as String)
    int limit = params.limit as int
    def jdbcTemplate = context.getBean(JdbcTemplate.class)

    // история покупок
    List<Map> purchases = []
    /*
  id integer NOT NULL,
  count integer NOT NULL,
  date timestamp without time zone NOT NULL,
  item_id integer NOT NULL,
  item_type integer NOT NULL,
  money_type integer NOT NULL,
  price integer NOT NULL,
  profile_id bigint NOT NULL,
  level smallint
     */
    String limitExp = limit > 0 ? "order by id desc limit " + limit : ""
    jdbcTemplate.query("select * from shop_statistic_parent where profile_id = ${profile.getId()} ${limitExp}", { ResultSet res ->
        def buy = [:]
        buy.itemTypeId = res.getInt("item_type")
        buy.itemId = res.getInt("item_id")
        buy.moneyType = res.getInt("money_type")
        buy.price = res.getInt("price")
        buy.count = res.getInt("count")
        buy.date = res.getTimestamp("date")
        buy.level = res.getShort("level")

        purchases.add(buy)
    } as RowCallbackHandler)

    jdbcTemplate.query("select date, level, cmd from wormswar.wipe_statistic where profile_id = ${profile.getId()}", { ResultSet res ->
        def buy = [:]
        buy.date = res.getTimestamp(1)
        buy.level = res.getInt(2)
        buy.itemTypeId = ItemType.WIPE.type
        buy.itemName = res.getString(3)
        if (buy.itemName != null && !buy.itemName.isEmpty()) {
            if ((buy.itemName as String).startsWith("clone ")) {
                buy.itemTypeId = 100
            } else {
                buy.itemId = buy.itemName as int // модификация профиля
            }
        } else {
            // обнуление
            buy.moneyType = MoneyType.REAL_MONEY.type
            buy.price = 5
            buy.count = 1
        }
        purchases.add(buy)
    } as RowCallbackHandler)

    console.print("OK")

    AdminHandler.xstream.toXML(purchases)
}

def getAwardStatistic(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def params = AdminHandler.xstream.fromXML(request.scriptParams) as Map
    def profile = getUserProfile(context, params.profileId as String)
    int limit = params.limit as int
    def weaponsCreator = context.getBean(WeaponsCreator.class)
    def stuffCreator = context.getBean(StuffCreator.class)
    def jdbcTemplate = context.getBean(JdbcTemplate.class)
    // история наград
    /*
  id bigint NOT NULL,
  award_type integer NOT NULL,
  date timestamp without time zone NOT NULL,
  item_id bigint NOT NULL,
  money integer NOT NULL,
  profile_id bigint NOT NULL,
  realmoney integer NOT NULL,
  note character varying(1024)
     */
    List<AwardStructure> awards = []
    String limitExp = limit > 0 ? "order by id desc limit " + limit : ""
    jdbcTemplate.query("select * from award_statistic_parent where profile_id = ${profile.getId()} ${limitExp}", { ResultSet res ->
        AwardStructure award = new AwardStructure()
        award.date = res.getTimestamp("date")
        award.awardTypeId = res.getInt("award_type")
        award.itemId = res.getInt("item_id")
        award.money = res.getInt("money")
        award.realmoney = res.getInt("realmoney")
        award.note = res.getString("note")
        awards.add(award)
    } as RowCallbackHandler)

    jdbcTemplate.query("select date, referral_link.* from wormswar.referral_link_visit " +
            "inner join wormswar.referral_link on id = referral_link_id  where profile_id = ${profile.getId()} ${limitExp}", { ResultSet res ->
        AwardStructure award = new AwardStructure()
        award.date = res.getTimestamp("date")
        award.awardTypeId = AwardTypeEnum.REFERRAL_LINK.type
        award.money = res.getInt("fuzy")
        award.realmoney = res.getInt("ruby")
        def battles = res.getInt("battles")
        def reaction = res.getInt("reaction")
        def reagents = res.getString("reagents")
        def weapons = res.getString("weapons")
        award.note = ""
        if (battles > 0) award.note += ", боёв: " + battles
        if (reaction > 0) award.note += ", реакция: " + reaction
        if (!reagents.isEmpty()) {
            String str = (reagents.split(" ") as List).groupBy({ r -> r }).collect { getReagentName(it.key as int) + ":" + it.value.size }
            award.note += ", " + str.substring(1, str.length() - 1)
        }
        if (!weapons.isEmpty()) {
            weapons.split(" ").each { weapon ->
                int itemId = weapon.split(":")[0] as int
                def itemBean = weaponsCreator.getWeapon(itemId)
                if (itemBean == null)
                    itemBean = stuffCreator.getStuff(itemId as short)
                award.note += ", " + itemBean.name.toLowerCase() + ":" + weapon.split(":")[1]
            }
        }
        award.note = award.note.replaceFirst(",", "").trim()

        awards.add(award)
    } as RowCallbackHandler)

    console.print("OK")

    new InteropSerializer().toString(awards.toArray(new AwardStructure[awards.size()]))
}

def getReagentName(int reagentId) {
    def reagentName = Reagent.valueOf(reagentId).name()
    switch (reagentName) {
        case "battery": return "Батарейка"
        case "gear": return "Шестерёнка"
        case "wood": return "Доска"
        case "stone": return "Камень"
        case "metal_plate": return "Пластинка"
        case "sand": return "Песок"
        case "coal": return "Уголь"
        case "screw_bolt": return "Шуруп"
        case "screw_nut": return "Гайка"
        case "pipe": return "Трубка"
        case "spring": return "Пружина"
        case "umbrella": return "Зонтик"
        case "accumulator": return "Аккумулятор"
        case "hammer": return "Молоток"
        case "gunpowder": return "Порох"
        case "steel_bar": return "Сталь"
        case "screwdriver": return "Отвертка"
        case "wrench": return "Гаечный ключ"
        case "crystal": return "Кристалл"
        case "generator": return "Генератор"
        case "microchip": return "Микрочип"
        case "sniper_scope": return "Оптический прицел"
        case "titan_bar": return "Титан"
        case "medal": return "Эмблема Вормикс"
        case "prize_key": return "Ключ"
        case "mutagen": return "Мутаген"

        default: return reagentName
    }

}

def getBattleStatistic(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> params = serializer.fromString(request.scriptParams, Map.class)
    int profileId = params.get("profileId") as Integer
    String forDate = params.get("forDate")
    String serverId = params.get("serverId")
    String socialIdPattern = "[0-9][0-9]?"
    if (serverId == null || serverId.isEmpty()) {
        def socialService = context.getBean(SocialService.class)
        if (socialService.socialServicesMap != null && socialService.socialServicesMap.size() == 1)
            socialIdPattern = "" + socialService.socialServicesMap.values().iterator().next().getSocialId().type
    } else if ((serverId as int) < 100) {
        socialIdPattern = serverId
    }
    synchronized (context) {
        def cdrPvpDir = new File("logs/cdr/cdr-pvp")
        if (!cdrPvpDir.exists())
            cdrPvpDir = new File("logs/cdr")
        def command
        //println("[${profileId}] [${socialIdPattern}] [${cdrPvpDir}]")
        if (forDate == "today") {
            command = ["bash", "-c", "grep -v LEAVE_LOBBY ${cdrPvpDir}/pvp_stat.log | grep -P '\\t${socialIdPattern}:${profileId}\\t'"]
        } else {
            command = ["bash", "-c", "zgrep -v LEAVE_LOBBY ${cdrPvpDir}/pvp_stat-${forDate}.log.zip | grep -P '\\t${socialIdPattern}:${profileId}\\t'"]
        }
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
        return stdin
    }
}

def getPvpBattlesDetails(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    Map<String, Object> params = AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, Object>
    int profileId = params.get("profileId") as Integer
    String forDate = params.get("forDate")
    def mapper = new ObjectMapper()
    String serverId = params.get("serverId")

    Integer socialNetRequired = null
    if (serverId == null || serverId.isEmpty()) {
        def socialServicesMap = context.getBean(SocialService.class).socialServicesMap
        if (socialServicesMap != null && socialServicesMap.size() == 1)
            socialNetRequired = socialServicesMap.values()[0].socialId.type
    } else if ((serverId as int) < 100) {
        socialNetRequired = serverId as int
    }

    String profileFilter = new ObjectMapper().writeValueAsString(socialNetRequired ? [profileId: profileId, socialNetId: socialNetRequired] : [profileId: profileId])
    String jqFilter = "select( [.participants[].profile] | contains([${profileFilter}]) )"

    synchronized (context) {
        def cdrPvpDir = "logs/cdr/cdr-pvp"
        if (Files.notExists(Paths.get(cdrPvpDir))) {
            cdrPvpDir = "logs/cdr"
        }
        def command
        if (forDate == "today") {
            command = ["bash", "-c", "grep '\\b${profileId}\\b' ${cdrPvpDir}/pvp/pvp-details.log | jq -c '${jqFilter}'"]
        } else {
            command = ["bash", "-c", "zgrep '\\b${profileId}\\b' ${cdrPvpDir}/pvp/pvp-details-${forDate}.log.zip | jq -c '${jqFilter}'"]
        }
        console.println "executing command: " + command
        long startTime = System.currentTimeMillis()

        def proc = command.execute()
        def result = proc.in.text.split("\n").findAll { it }.collect { mapper.readValue(it, Map.class) }
        result.each {
            (it.participants as List<Map>).each {
                def profile = it.profile as Map
                profile.name = getProfileName(context, PvpService.getProfileId(profile.profileId as Long), profile.socialNetId as byte, "" + profile.profileId)
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

def getBossBattleStatistic(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> params = serializer.fromString(request.scriptParams, Map.class)
    int profileId = params.get("profileId") as Integer
    String forDate = params.get("forDate")
    synchronized (context) {
        def command
        if (forDate == "today") {
            command = ["grep", "-P", "EndBattle\\t${profileId}\\t", "logs/cdr/battles_stat.log"]
        } else {
            command = ["zgrep", "-P", "EndBattle\\t${profileId}\\t", "logs/cdr/battles_stat-${forDate}.log.zip"]
        }

        console.println "executing command: " + command
        long startTime = System.currentTimeMillis()
        def proc = command.execute()
        String stdin = proc.in.text
        String stderr = proc.err.text
        proc.waitFor()
        long execTime = System.currentTimeMillis() - startTime

        console.println "finished command: $command in $execTime ms"
        console.println "return code: " + proc.exitValue()
        console.println "stderr:" + stderr
        return stdin
    }
//    return new String(new File("1/battles_stat.log").readBytes(), "UTF-8")
}

def getPaymentStatistic(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    UserProfile profile = getUserProfileFromParams(context, request.scriptParams)
    DaoService daoService = context.getBean(DaoService.class)
    // история платежей
    List<PaymentStatisticEntity> paymentStatisticEntities = daoService.getPaymentStatisticDao().selectPaymentStatistics(profile.getId());
    PaymentStructure[] paymentStructures = new PaymentStructure[paymentStatisticEntities.size()]
    i = 0
    paymentStatisticEntities.each {
        PaymentStructure payment = new PaymentStructure()
        payment.date = it.date
        payment.moneyType = null
        payment.votes = it.votes
        payment.transactionId = it.transactionId
        payment.amount = it.amount
        payment.balanse = it.balanse
        def item = (!it.completed ? "ОТОЗВАН! " : "") + it.item
        payment.item = AdminHandler.xstream.toXML([item: item, paymentType: it.moneyType])
        if (payment.amount == 0) {
            payment.amount = payment.moneyType == MoneyType.REAL_MONEY ? payment.votes * 3 : payment.votes * 300;
        }
        paymentStructures[i] = payment
        i++
    }
    console.print("OK")

    new InteropSerializer().toString(paymentStructures)
}

def getAchievements(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def profileService = context.getBean(ProfileService.class)
    def achieveAwardService = context.getBean(AchieveAwardService.class)
    def achieveCommandService = context.getBean(AchieveCommandService.class)
    def achievementService = achieveCommandService.getService(new WormixAchievements(""))

    int profileId = new InteropSerializer().fromString(request.scriptParams, String.class) as int
    def stringId = profileService.getProfileAchieveId(profileId)
    ProfileAchievements profileAchievements = achieveCommandService.getProfileAchievementsOrCreateNew(stringId, WormixAchievements.class);

    final int maxBoolAchieveIndex = Math.min(WormixAchievements.MAX_BOOL_ACHIEVE_INDEX, achievementService.maxBoolAchieveIndex)
    Map<Integer, Boolean> boolAchievements = new LinkedHashMap<>()
    for (int i = 0; i <= maxBoolAchieveIndex; i++) {
        boolAchievements.put(i, AchieveUtils.haveBoolAchievement(profileAchievements.boolAchievements, i))
    }

    final int maxAchieveIndex = Math.min(WormixAchievements.MAX_ACHIEVE_INDEX, achievementService.maxAchieveIndex)
    Map<Integer, Integer> achievements = new TreeMap<>()
    for (int i = 0; i <= maxAchieveIndex; i++) {
        achievements.put(i, AchieveUtils.shortToInt(profileAchievements.achievements[i]));
    }
    for (int i = 0; i <= WormixAchievements.MAX_STAT_INDEX; i++) {
        achievements.put(ProfileAchievements.STAT_FIRST_INDEX + i, profileAchievements.statistics[i]);
    }

    Map<String, Object> result = new HashMap<>()
    result.put("boolAchievements", boolAchievements)
    result.put("achievements", achievements)
    result.put("investedAwardPoints", profileAchievements.investedAwardPoints)
    result.put("achievePoints", achievementService.countAchievePoints(profileAchievements))

    def userProfile = profileService.getUserProfile(profileId)
    if (userProfile != null)
        result.put("bonusItemCount", achieveAwardService.countBonusItemInBackpack(userProfile))

    new InteropSerializer().toString(result)
}

def getBlackList(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ProfileService profileService = context.getBean(ProfileService.class)
    UserProfile profile = getUserProfileFromParams(context, request.scriptParams)
    byte socialNetId = profileService.getShortSocialIdFor(profile) as byte

    Map blackListMaps = getBlackListMaps(context, profile, socialNetId)
    List candidats = blackListMaps.candidats.collect {
        [
                id  : PvpService.getProfileId(it as Long),
                name: getProfileName(context, PvpService.getProfileId(it as Long), socialNetId, "" + PvpService.getProfileId(it as Long))
        ]
    }
    List blackList = blackListMaps.blackList.collect {
        [
                id        : PvpService.getProfileId(it.key as Long),
                name      : getProfileName(context, PvpService.getProfileId(it.key as Long), socialNetId, "" + PvpService.getProfileId(it.key as Long)),
                createTime: it.value
        ]
    }

    return AdminHandler.xstream.toXML([candidats: candidats, blackList: blackList] as Map)
}

private Map getBlackListMaps(ApplicationContext context, UserProfile profile, byte socialNetId) {
    BlackListService blackListService = context.getBean(BlackListService.class)
    Long profileId = PvpService.getPvpUserId(profile.id, socialNetId)

    Map<Long, byte[]> battleResults = blackListService.getBattlesResultsForUsers().get(profileId)
    List<Long> candidats = battleResults.findAll { Arrays.equals(it.value, [1, 1, 1] as byte[]) }.collect { it.key }

    Map<Long, Integer> blackList = blackListService.blackListsForUsers.get(profileId) ?: [:] as Map

    return [candidats: candidats, blackList: blackList] as Map
//    return [candidats: [58027749l], blackList: [58027749l: 1381867200, 58027748l: 1381867200, 58027747l: 1381780800, 58027746l: 1381780800 ]] as Map
}

def UserProfile getUserProfile(ApplicationContext context, String profileId) {
    ProfileService profileService = context.getBean(ProfileService.class)
    UserProfile profile = profileService.getUserProfile(profileId)

    if (profile == null) {
        throw new ExecAdminScriptException(ServiceResult.ERR_PROFILE_NOT_FOUND, "UserProfile not found by id ${profileId}")
    }
    return profile
}

def UserProfile getUserProfileFromParams(ApplicationContext context, String params) {
    InteropSerializer serializer = new InteropSerializer()
    String profileId = serializer.fromString(params, String.class)
    return getUserProfile(context, profileId)
}
