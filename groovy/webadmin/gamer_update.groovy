import com.pragmatix.achieve.domain.ProfileAchievements
import com.pragmatix.achieve.domain.WormixAchievements
import com.pragmatix.achieve.services.AchieveCommandService
import com.pragmatix.app.common.AwardTypeEnum
import com.pragmatix.app.common.Race
import com.pragmatix.app.dao.UserProfileDao
import com.pragmatix.app.domain.BackpackItemEntity
import com.pragmatix.app.domain.DepositEntity
import com.pragmatix.app.init.LevelCreator
import com.pragmatix.app.init.UserProfileCreator
import com.pragmatix.app.messages.client.ToggleTeamMember
import com.pragmatix.app.model.*
import com.pragmatix.app.services.*
import com.pragmatix.app.services.rating.RatingService
import com.pragmatix.app.services.rating.RatingServiceImpl
import com.pragmatix.app.settings.AppParams
import com.pragmatix.app.settings.GenericAward
import com.pragmatix.clanserver.messages.request.UpdateRatingRequest
import com.pragmatix.clanserver.services.ClanService
import com.pragmatix.common.utils.AppUtils
import com.pragmatix.craft.domain.Reagent
import com.pragmatix.craft.domain.ReagentsEntity
import com.pragmatix.craft.model.Recipe
import com.pragmatix.craft.services.CraftService
import com.pragmatix.gameapp.cache.SoftCache
import com.pragmatix.gameapp.common.SimpleResultEnum
import com.pragmatix.pvp.BattleWager
import com.pragmatix.webadmin.AdminHandler
import com.pragmatix.webadmin.ExecAdminScriptException
import com.pragmatix.wormix.webadmin.interop.CommonResponse
import com.pragmatix.wormix.webadmin.interop.InteropSerializer
import com.pragmatix.wormix.webadmin.interop.ServiceResult
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest
import org.apache.commons.lang.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionCallbackWithoutResult
import org.springframework.transaction.support.TransactionTemplate

import java.util.concurrent.TimeUnit

import static com.pragmatix.app.services.ProfileEventsService.Param

def update(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    String profileId = map.get("id") as String
    UserProfile profile = getUserProfile(context, profileId)

    def levelCreator = context.getBean(LevelCreator.class)
    def userRegistry = context.getBean(UserRegistry.class)
    def searchTheHouseService = context.getBean(SearchTheHouseService.class)
    def dailyRegistry = context.getBean(DailyRegistry.class)
    def stuffService = context.getBean(StuffService.class)
    def ratingService = context.getBean(RatingService.class)
    def transactionTemplate = context.getBean(TransactionTemplate.class)
    def userProfileDao = context.getBean(UserProfileDao.class)
    def profileService = context.getBean(ProfileService.class)
    def clanService = context.getBean(ClanService.class)
    def groupService = context.getBean(GroupService.class)
    def battleService = context.getBean(BattleService.class)
    def userProfileCreator = context.getBean(UserProfileCreator.class)

    String name = map.get("name") as String
    Byte renameAct = map.get("renameAct") as Byte
    Integer money = map.get("money") as Integer
    Integer realMoney = map.get("realMoney") as Integer
    Integer rating = map.get("rating") as Integer

    Map<String, Integer> dailyRatingsMap = map.entrySet().findAll { Map.Entry<String, ?> it -> it.key.startsWith("dailyRating_") && (it.value as int) != 0 }
            .collectEntries { [(it.key - "dailyRating_"): it.value as int] }

    Integer rankPoints = map.get("rankPoints") as Integer
    Integer bestRank = map.get("bestRank") as Integer
    boolean propagateToTop = map.get("propagateToTop") as Boolean ?: false
    // флаг "должно ли изменение рейтинга приводить к перестройке топа?"
    Integer reactionRate = map.get("reactionRate") as Integer
    Integer battlesCount = map.get("battlesCount") as Integer
    Integer loginSequence = map.get("loginSequence") as Integer
    Integer currentMission = map.get("currentMission") as Integer
    Integer currentNewMission = map.get("currentNewMission") as Integer
    Integer armor = map.get("armor") as Integer
    Integer attack = map.get("attack") as Integer
    Integer levelValue = map.get("level") as Integer
    Integer experience = map.get("experience") as Integer
    Integer hatId = map.get("hatId") as Integer
    Integer kitId = map.get("kitId") as Integer
    Integer searchKeys = map.get("searchKeys") as Integer
    Integer howManyPumped = map.get("howManyPumped") as Integer
    String raceName = map.get("race") as String
    Race race = raceName != null ? Race.valueOf(raceName) : null
    String racesStr = map.get("races") as String
    String skinsStr = map.get("skins") as String
    String activeSkinsStr = map.get("activeSkins") as String
    Integer vipExpiryTime = map.get("vipExpiryTime") as Integer
    Byte renameVipAct = map.get("renameVipAct") as Byte
    Integer bossWinAwardToken = map.get("bossWinAwardToken") as Byte
    Integer wagerWinAwardToken = map.get("wagerWinAwardToken") as Byte

    if (levelValue != null && levelValue > 0) {
        Level level = levelCreator.getLevel(levelValue)
        if (level == null) throw new ExecAdminScriptException(ServiceResult.ERR_INVALID_ARGUMENT, "level [${levelValue}] not exist")
        profile.setLevel(levelValue)
        profile.setArmor(userProfileCreator.getProfileInitParams().defaultArmorForLevel(levelValue))
        profile.setAttack(userProfileCreator.getProfileInitParams().defaultAttackForLevel(levelValue))
        profile.setExperience(0)
        //обновляем уровень игрока в глобальном кеше уровней
        userRegistry.updateLevel(profile)
        // даем возможность заработать на обыске, если достигнут "зачетный" уровень
        searchTheHouseService.fireLevelUp(profile)
        // При уменьшении уровня игрока автоматически удалять лишних членов команды
        def maxGroupCountOnLevel = 1
        if (levelValue >= 5 && levelValue < 13)
            maxGroupCountOnLevel = 2
        else if (levelValue >= 13 && levelValue < 21)
            maxGroupCountOnLevel = 3
        else if (levelValue >= 21)
            maxGroupCountOnLevel = 4

        while (profile.wormsGroup.length > maxGroupCountOnLevel + profile.extraGroupSlotsCount) {
            def wormsGroup = profile.wormsGroup
            def memberIndex = wormsGroup[wormsGroup.length - 1] == profile.id.intValue() ? 0 : wormsGroup.length - 1
            def memberId = wormsGroup[memberIndex]
            def result = groupService.removeFromGroup(profile, memberId)
            if (result != SimpleResultEnum.SUCCESS)
                throw new IllegalStateException("не удалось сократить команду! Ошибка при удалении [$memberId]")
        }
        def deactivateIndex = profile.wormsGroup.length - 1
        while (profile.activeTeamMembersCount > (maxGroupCountOnLevel as byte) && deactivateIndex >= 0) {
            def wormsGroup = profile.wormsGroup
            def memberId = wormsGroup[deactivateIndex]
            if (memberId != profile.id.intValue() && profile.teamMembers[deactivateIndex]?.active) {
                def result = groupService.toggleTeamMember(new ToggleTeamMember(memberId, false), profile)
                if (result != SimpleResultEnum.SUCCESS)
                    throw new IllegalStateException("не удалось сократить команду! Ошибка при деактивации [$memberId]")
            }
            deactivateIndex--;
        }
    }
    if (name != null) profileService.setName(profile, name)
    if (renameAct != null && renameAct >= (byte) 0) profile.renameAct = renameAct
    if (renameVipAct != null && renameVipAct >= (byte) 0) profile.renameVipAct = renameVipAct
    if (armor != null && armor >= 0) profile.setArmor(armor)
    if (attack != null && attack >= 0) profile.setAttack(attack)
    if (money != null && money >= 0) profile.setMoney(money)
    if (realMoney != null && realMoney >= 0) profile.setRealMoney(realMoney)
    if (battlesCount != null && battlesCount >= 0) {
        profile.setLastBattleTime(System.currentTimeMillis())
        profile.setBattlesCount(battlesCount)
        battleService.checkBattleCount(profile)
    }
    if (reactionRate != null && reactionRate >= 0) profile.setReactionRate(reactionRate)
    if (searchKeys != null && searchKeys >= 0) dailyRegistry.setSearchKeys(profile.getId(), searchKeys.byteValue())
    if (howManyPumped != null && howManyPumped >= 0) dailyRegistry.setHowManyPumped(profile.getId(), howManyPumped.byteValue())
    if (loginSequence != null && loginSequence >= 0) {
        profile.setLoginSequence(loginSequence.byteValue())
        profile.setLastLoginTime(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)))
        profile.setPickUpDailyBonus(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)))
    }
    if (race != null) profile.setRace(race)
    if (experience != null && experience >= 0) profile.setExperience(experience)
    if (currentMission != null) {
        profile.setCurrentMission(currentMission.shortValue())
        dailyRegistry.clearMission(profile.getId())
    }
    if (currentNewMission != null && currentNewMission >= 0) {
        profile.setCurrentNewMission(currentNewMission.shortValue())
        dailyRegistry.clearMission(profile.getId())
    }
    if (hatId != null)
        if (hatId > 0)
            stuffService.selectHat(profile, profile.getId().intValue(), hatId.shortValue())
        else
            stuffService.deselectHat(profile, profile.getId().intValue())
    if (kitId != null)
        if (kitId > 0)
            stuffService.selectKit(profile, profile.getId().intValue(), kitId.shortValue())
        else
            stuffService.deselectKit(profile, profile.getId().intValue())
    if (rating != null) {
        int oldRating = profile.getRating()
        def ratingPoints = rating - oldRating
        int newRating = Math.max(0, profile.getRating() + ratingPoints)
        profile.setRating(newRating)
        if (propagateToTop) {
            boolean maybeAdd = ratingPoints > 0
            onUpdateRating(ratingService, profile, maybeAdd)
            short socialId = profileService.getSocialIdForClan(profile)
            clanService.updateRating(new UpdateRatingRequest(socialId, profile.getId().intValue(), newRating, ratingPoints))
        }
    }
    if (rankPoints != null && ratingService instanceof RatingServiceImpl) {
        if (propagateToTop) {
            (ratingService as RatingServiceImpl).addSeasonRating(profile, rankPoints - profile.rankPoints)
        } else {
            profile.rankPoints = rankPoints
        }
    }
    if (bestRank != null && ratingService instanceof RatingServiceImpl) {
        profile.bestRank = bestRank as byte
        if (propagateToTop) {
            (ratingService as RatingServiceImpl).addSeasonRating(profile, 0)
        }
    }

    if (racesStr) {
        Race[] races = racesStr.split(',').collect { Race.valueOf(it as int) }.findAll { it != null } as Race[]
        profile.setRaces(Race.setRaces(races))
    }
    if (skinsStr) {
        byte[] skins = skinsStr.split(',').collect { it as byte } as byte[]
        profile.setSkins(skins)
    }
    if (activeSkinsStr) {
        def activeSkins = activeSkinsStr.split(',').collect { it as byte }
        byte[] skins = profile.getSkins().collect {
            byte skin = Math.abs(it)
            if (activeSkins.contains(skin))
                -skin
            else
                skin
        } as byte[]
        profile.setSkins(skins)
    }

    if (dailyRatingsMap) {
        dailyRatingsMap.entrySet().forEach {
            def battleWager = BattleWager.valueOf(it.key)
            int newRating = it.value
            int oldRating = dailyRegistry.getDailyRating(profile.getId(), battleWager)

            if (propagateToTop) {
                ratingService.updateDailyRating(profile, newRating - oldRating, BattleWager.NO_WAGER)
                ratingService.updateDailyRating(profile, newRating - oldRating, battleWager)
            } else {
                dailyRegistry.setDailyRating(profile.getId(), newRating, battleWager);
            }
        }
    }
//    if (dailyRatings.findAll(){it != null && it == 0}.size() == dailyRatings.size()){
//        // обнуление ежедневного рейтнга игроку
//        ratingService.wipeDailyRatings(profile.getId())
//    }
    if (vipExpiryTime != null) {
        profileService.setVipExpiryTime(profile, vipExpiryTime)
    }
    if (bossWinAwardToken != null) {
        dailyRegistry.setBossWinAwardToken(profile.getId(), bossWinAwardToken)
    }
    if (wagerWinAwardToken != null) {
        dailyRegistry.setWagerWinAwardToken(profile.getId(), wagerWinAwardToken)
    }

    transactionTemplate.execute({ status -> userProfileDao.updateProfile(profile) } as TransactionCallbackWithoutResult)

    return "[${profileId}] successfully updated"
}

def onUpdateRating(RatingService ratingService, UserProfile profile, boolean maybeAdd) {
    ratingService.onUpdateRating(profile, maybeAdd)
}

/*
actionId=1: добавить предмет
  weaponId
  count - количество
возвращаешь status=1 если предмет был добавлен ранее

actionId=3: удалить предмет
  weaponId
возвращаешь status=3 если предмета нет
 */

def updateStuff(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    String profileId = map.get("profileId") as String
    UserProfile profile = getUserProfile(context, profileId)

    StuffService stuffService = context.getBean(StuffService.class)
    TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class)
    UserProfileDao userProfileDao = context.getBean(UserProfileDao.class)

    int actionId = map.get("actionId") as Integer;
    short stuffId = (map.get("stuffId") as Integer).shortValue()
    int expire = map.get("expire") as Integer
    TimeUnit timeUnit = TimeUnit.valueOf(map.get("timeUnit").toString())
    // добавить предмет
    if (actionId == 1) {
        boolean result = false
        Stuff stuff = stuffService.getStuff(stuffId)
        if (stuff != null) {
            if (stuff.isTemporal()) {
                if (stuff.boost) {
                    result = stuffService.addBooster(profile, stuff)
                } else {
                    result = stuffService.addStuff(profile, stuffId, expire, timeUnit, true)
                }
            } else {
                result = stuffService.addStuff(profile, stuffId)
            }
        }
        if (!result) {
            return new CommonResponse(ServiceResult.ERR_RESPONSE, "[${profileId}] failure add stuff [${stuffId}]", "1")
        }
    } else if (actionId == 3) {
        // удалить предмет
        boolean result = stuffService.removeStuff(profile, stuffId);
        if (!result) {
            return new CommonResponse(ServiceResult.ERR_RESPONSE, "[${profileId}] failure remove stuff [${stuffId}]", "3")
        }
    } else {
        throw new ExecAdminScriptException(ServiceResult.ERR_INVALID_ARGUMENT, "invalid actionId [${actionId}]")
    }

    profile.dirty = true
    transactionTemplate.execute({ status -> userProfileDao.updateProfile(profile) } as TransactionCallbackWithoutResult);

    return "[${profileId}] stuffs successfully updated"
}

/*
actionId=1: добавить оружие
  weaponId
  count - количество
возвращаешь status=1 если оружие было добавлено ранее

actionId=3: удалить оружие
  weaponId
возвращаешь status=3 если оружия нет
 */

def updateWeapon(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    String profileId = map.get("profileId") as String
    UserProfile profile = getUserProfile(context, profileId);

    WeaponService weaponService = context.getBean(WeaponService.class);
    CraftService craftService = context.getBean(CraftService.class);
    ProfileService profileService = context.getBean(ProfileService.class);

    int actionId = map.get("actionId") as Integer;
    // добавить оружие
    if (actionId == 1) {
        int weaponId = map.get("weaponId") as Integer;
        int count = map.get("count") as Integer;
        // сначала удаляем
        weaponService.removeWeapon(profile, weaponId);
        // потом добавляем
        def weapon = weaponService.getWeapon(weaponId);
        if (weapon.isSeasonal()) {
            def item = profile.getBackpackItemByWeaponId(weaponId);
            if (item != null) {
                profile.setBackpackItemCount(item.getWeaponId(), item.getCount() + count);
            } else {
                profile.addBackpackItem(new BackpackItem(weaponId, count, true));
            }
        } else if (weapon.isType(Weapon.WeaponType.COMPLEX)) {
            if (count > 0) {
                def item = profile.getBackpackItemByWeaponId(weaponId);
                if (item != null) {
                    profile.setBackpackItemCount(item.getWeaponId(), count);
                } else {
                    profile.addBackpackItem(new BackpackItem(weaponId, count, true));
                }
            } else if (count == -1) {
                weaponService.addOrUpdateWeapon(profile, weaponId, weapon.maxWeaponLevel)
            } else if (count < -10) {
                weaponService.addOrUpdateWeapon(profile, weaponId, -count - 10)
            }
        } else {
            boolean result = weaponService.addOrUpdateWeapon(profile, weaponId, count);
            if (!result) {
                return new CommonResponse(ServiceResult.ERR_RESPONSE, "[${profileId}] failure add or update weapon [${weaponId}]", "1")
            }
        }
    } else if (actionId == 3) {
        // убрать оружие
        int weaponId = map.get("weaponId") as Integer;
        boolean result = weaponService.removeWeapon(profile, weaponId);
        if (!result) {
            return new CommonResponse(ServiceResult.ERR_RESPONSE, "[${profileId}] failure remove weapon [${weaponId}]", "3")
        }
        craftService.rollbackRecipesForWeapon(profile, weaponId);
    } else if (actionId == 2) {
        short recipeId = (map.get("recipeId") as Integer).shortValue();
        // применить рецепт
        if (recipeId > (short) 0) {
            Recipe recipe = null;
            def sb = new StringBuilder()
            try {
                recipe = validateRecipe(profile, recipeId, context, sb);
            } catch (ExecAdminScriptException e) {
                return new CommonResponse(e.result, sb.toString(), e.getMessage())
            }
            // разбираем оружие до базового
            craftService.rollbackRecipesForWeapon(profile, recipe.getWeaponId());
            applyBaseRecipes(profile, recipe, context);
            applyRecipe(profile, recipe.getId());
        } else if (recipeId == (short) 0) {
            // разобрать оружие до базового
            int weaponId = map.get("weaponId") as Integer;
            craftService.rollbackRecipesForWeapon(profile, weaponId);
        } else {
            throw new ExecAdminScriptException(ServiceResult.ERR_INVALID_ARGUMENT, "invalid recipeId [${recipeId}]")
        }
    } else {
        throw new ExecAdminScriptException(ServiceResult.ERR_INVALID_ARGUMENT, "invalid actionId [${actionId}]")
    }

    profile.setDirty(true);
    profileService.updateSync(profile)

    return "[${profileId}] backpack successfully updated"
}

/*
actionId=1: добавить вклад
  key - ключ бина
  startDate - дата начала
возвращаешь status=1 если нет такого бина

actionId=-1: закрыть вклад
  depositId - DepositEntity.id
возвращаешь status=-1 если вклада нет
 */

def updateDeposit(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    Map<String, Object> params = AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, Object>
    String profileId = params.profileId
    UserProfile profile = getUserProfile(context, profileId)

    DepositService depositService = context.getBean(DepositService.class)
    ProfileService profileService = context.getBean(ProfileService.class)
    TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class);

    int actionId = params.actionId as int
    String message;
    switch (actionId) {
        case 1:
            String key = params.key as String
            Date startDate = params.startDate as Date
            DepositBean depositBean = depositService.getDepositBean(key)
            if (!depositBean) {
                return new CommonResponse(ServiceResult.ERR_RESPONSE, "[${profileId}] failed to add deposit [${key}]", "1")
            }
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            int newId = jdbcTemplate.queryForObject("SELECT nextval('payment_sequence')", Integer.class)
            transactionTemplate.execute { status ->
                depositService.openDeposit(depositBean, newId, startDate, profile)
            }
            message = "Deposit ${key} successfully opened"
            break;
        case -1:
            int depositId = params.depositId as int
            DepositEntity[] deposits = depositService.getDepositsFor(profile)
            DepositEntity deposit = deposits.find { it.id == depositId }
            if (!deposit) {
                return new CommonResponse(ServiceResult.ERR_RESPONSE, "[${profileId}] not found deposit [${depositId}]", "-1")
            }
            DaoService daoService = context.getBean(DaoService.class)
            deposit.paidOff = true
            transactionTemplate.execute { status ->
                daoService.depositDao.updateProgress(deposit)
            }
            message = "Deposit #${depositId} successfully closed"
            break;
        default:
            throw new ExecAdminScriptException(ServiceResult.ERR_INVALID_ARGUMENT, "invalid actionId [${actionId}]")
    }

    profileService.updateSync(profile)

    return AdminHandler.xstream.toXML(profileId + ': ' + message)
}


def cleanBackpack(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    Map<String, Object> map = new InteropSerializer().fromString(request.scriptParams, Map.class)
    String profileId = map.get("profileId") as String
    UserProfile profile = getUserProfile(context, profileId);

    def userProfileCreator = context.getBean(UserProfileCreator.class);
    def profileService = context.getBean(ProfileService.class);

    // удаляем всё оружие
    profile.setRecipes(new short[0])
    profile.backpack.each {
        it.count = 0;
        it.dirty = true;
    }
    profileService.updateSync(profile)

    // заполняем оружием выдаваеиым на старте
    List<BackpackItemEntity> backpack = userProfileCreator.createDefaultBackpack(profile.id);
    profile.setBackpack(userProfileCreator.initBackpack(backpack))
    profile.setUserProfileStructure(null);

    profileService.updateSync(profile)

    return "[${profileId}] backpack successfully cleaned"
}

def downgradeAll(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    String profileId = map.get("profileId") as String
    UserProfile profile = getUserProfile(context, profileId);

    ProfileService profileService = context.getBean(ProfileService.class);

    profile.setRecipes(new short[0])

    profile.setDirty(true);
    profileService.updateSync(profile)

    return "[${profileId}] all weapons in backpack successfully downgraded"
}

def applyBaseRecipes(UserProfile profile, Recipe recipe, ApplicationContext context) {
    // применяем все базовые рецепты (если они есть)
    Recipe baseRecipe = recipe.getBaseRecipe();
    while (baseRecipe != null) {
        applyRecipe(profile, baseRecipe.getId());
        baseRecipe = baseRecipe.getBaseRecipe();
    }
}

def applyRecipe(UserProfile profile, short recipeId) {
    short[] recipes = profile.getRecipes();
    if (recipes.contains(recipeId)) return;
    profile.setRecipes(ArrayUtils.add(recipes, recipeId));
}

def validateRecipe(UserProfile profile, short recipeId, ApplicationContext context, StringBuilder console) {
    def craftService = context.getBean(CraftService.class);
    def weaponService = context.getBean(WeaponService.class);
    def recipes = craftService.getAllRecipesMap();
    Recipe recipe = recipes.get(recipeId);
    if (recipe == null) {
        console.append("[${profile.getId()}] recipe not found by id [${recipeId}]");
        // не найден рецепт
        throw new ExecAdminScriptException(ServiceResult.ERR_RESPONSE, "3")
    }
    // проверяем наличие базового (бесконечного) оружия в рюкзаке
    BackpackItem backpackItem = profile.getBackpackItemByWeaponId(recipe.getWeaponId());
    if (backpackItem == null || !weaponService.isPresentInfinitely(backpackItem)) {
        console.append("[${profile.getId()}] error apply recipe [${recipeId}]: base weapon absent in backpack or not infinite [${backpackItem}]");
        // не применен родительский рецепт
        throw new ExecAdminScriptException(ServiceResult.ERR_RESPONSE, "3")
    }

    for (short presentRecipeId : profile.getRecipes()) {
        if (recipe == recipeId) {
            console.append("[${profile.getId()}] apply recipe failure because recipe [${recipeId}] alredy applied!");
            throw new ExecAdminScriptException(ServiceResult.ERR_RESPONSE, "2")
        }
    }

    return recipe;
}

def grantAward(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)

    Set<String> profileIds = new TreeSet<>((map.get("profileId") as String).split("[ \n\t]") as List)

    int money = map.get("money") as Integer ?: 0;
    int realmoney = map.get("realmoney") as Integer ?: 0;
    int battlesCount = map.get("battlesCount") as Integer ?: 0;
    int wagerToken = map.get("wagerToken") as Integer ?: 0;
    int bossToken = map.get("bossToken") as Integer ?: 0;
    int rating = map.get("rating") as Integer ?: 0;
    int rankPoints = map.get("rankPoints") as Integer ?: 0;
    int medal = map.get("medal") as Integer ?: 0;
    int key = map.get("key") as Integer ?: 0;
    int mutagen = map.get("mutagen") as Integer ?: 0;
    int antitoxin = map.get("antitoxin") as Integer ?: 0;
    int bandage = map.get("bandage") as Integer ?: 0;
    int vip = map.get("vip") as Integer ?: 0;
    int exp = map.get("exp") as Integer ?: 0;
    int rename = map.get("rename") as Integer ?: 0;
    String note = map.get("note") ?: "";
    int reaction = map.get("reaction") as Integer ?: 0;
    String bundleCode = map.get("bundleCode") as String
    String reagents = map.get("reagents") as String
    String weaponShoots = map.get("weaponShoots") as String
    String temporalItems = map.get("temporalItem") as String
    int awardType = AwardTypeEnum.ADMIN.type;
    // проверяем, что админ написал причину
    if (note.isEmpty()) {
        return new CommonResponse(ServiceResult.ERR_RESPONSE, "note is empty", "1");
    }

    def ratingService = context.getBean(RatingService.class)
    def profileService = context.getBean(ProfileService.class);
    def clanService = context.getBean(ClanService.class)
    def profileBonusService = context.getBean(ProfileBonusService.class)
    def bundleService = context.getBean(BundleService.class)
    def stuffService = context.getBean(StuffService.class)

    def grantedProfiles = []

    for (profileId in profileIds) {
        UserProfile profile = profileService.getUserProfile(profileId)
        if (profile != null) {
            grantedProfiles += profile.id

            if (rating != 0) {
                int newRating = Math.max(0, profile.getRating() + rating)
                profile.setRating(newRating)
                boolean maybeAdd = rating > 0
                onUpdateRating(ratingService, profile, maybeAdd)
                short socialId = profileService.getSocialIdForClan(profile)
                clanService.updateRating(new UpdateRatingRequest(socialId, profile.getId().intValue(), newRating, rating))
            }
            if (rankPoints != 0) {
                int newRankPoints = Math.max(0, profile.rankPoints + rankPoints)
                profile.rankPoints = newRankPoints
                boolean maybeAdd = rankPoints > 0
                onUpdateRating(ratingService, profile, maybeAdd)
            }
            if (vip) {
                int vipFrom = Math.max(profile.getVipExpiryTime(), AppUtils.currentTimeSeconds())
                def expiryTimeSeconds = vipFrom + (int) TimeUnit.DAYS.toSeconds(vip)
                profileService.setVipExpiryTime(profile, expiryTimeSeconds)
            }

            def builder = GenericAward.builder()
            if (money > 0)
                builder.addMoney(money)
            else if (money < 0) {
                money = profile.money + money < 0 ? -profile.money : money
                profile.setMoney(profile.money + money)
            }
            if (realmoney > 0)
                builder.addRealMoney(realmoney)
            else if (realmoney < 0) {
                realmoney = profile.realMoney + realmoney < 0 ? -profile.realMoney : realmoney
                profile.setRealMoney(profile.realMoney + realmoney)
            }
            if (reaction > 0) builder.addReactionRate(reaction)
            if (exp > 0) builder.addExperience(exp)
            if (medal > 0) builder.addReagent(Reagent.medal.index, medal)
            if (key > 0) builder.addReagent(Reagent.prize_key.index, key)
            if (mutagen > 0) builder.addReagent(Reagent.mutagen.index, mutagen)
            if (antitoxin > 0) builder.addWeapon(80, antitoxin)
            if (bandage > 0) builder.addWeapon(72, bandage)

            def bundleItems = []
            if (bundleCode) {
                def validBundle = bundleService.getValidBundle(bundleCode)
                if (!validBundle) {
                    return new CommonResponse(ServiceResult.ERR_INVALID_ARGUMENT, "bundle with this code does not exists or is not valid", bundleCode)
                }
                if (bundleService.preConfiguredBundles.containsKey(validBundle.code)) {
                    return new CommonResponse(ServiceResult.ERR_INVALID_ARGUMENT, "preconfigured bundle is not allowed", bundleCode)
                }
                def res = bundleService.issueBundle(profile, validBundle)
                bundleItems = res._1
                def reparationRealMoney = res._2
                if (reparationRealMoney > 0) {
                    // была выплачена компенсация за уже имеющиеся предметы - но для набора в качестве награды её не нужно было выплачивать
                    profile.realMoney = profile.realMoney - reparationRealMoney
                }
            }
            if (rename) {
                builder.addRename(rename)
            }

            def award = builder.build()
            if (reagents) {
                Map<Byte, Integer> reagentsMap = new HashMap<>();
                for (String s : StringUtils.split(reagents)) {
                    String[] item = s.split(":");
                    Reagent reagentName = Reagent.valueOf(Integer.parseInt(item[0]));
                    byte reagentIndex = reagentName.getIndex();
                    reagentsMap.put(reagentIndex, Integer.parseInt(item[1]));
                }
                award.setReagents(reagentsMap);
            }
            if (weaponShoots) {
                StringUtils.split(weaponShoots)
                        .collect { it.split(":") }
                        .each { ss -> award.addWeapon(ss[0] as int, ss[1] as int) }
            }
            if (temporalItems) {
                temporalItems.split().each { temporalItem ->
                    try {
                        short itemId = temporalItem.split(":")[0] as short
                        def item = stuffService.getStuff(itemId)

                        if (!item || !item.isTemporal()) {
                            return new CommonResponse(ServiceResult.ERR_INVALID_ARGUMENT, "Предмет ${item} c id ${itemId} должен быть временным", bundleCode)
                        }

                        def duration = temporalItem.split(":")[1]
                        int hours
                        if (duration.endsWith("d")) {
                            hours = (duration.replace("d", "") as int) * 24
                        } else {
                            hours = duration.replace("h", "") as int
                        }
                        award.addStuffForHours(itemId, hours)
                    } catch (Exception e) {
                        return new CommonResponse(ServiceResult.ERR_INVALID_ARGUMENT, "Не верный формат для выдачи временного предмета [itemId:duration]", bundleCode)
                    }
                }
            }

            if (battlesCount > 0) award.battlesCount = battlesCount
            if (wagerToken > 0) award.wagerWinAwardToken = wagerToken
            if (bossToken > 0) award.bossWinAwardToken = bossToken
            
            profileBonusService.awardProfile(award, profile, AwardTypeEnum.ADMIN,
                    "rating", rating,
                    Param.money, money < 0 ? money : 0,
                    Param.realMoney, realmoney < 0 ? realmoney : 0,
                    "rankPoints", rankPoints,
                    "reaction", reaction,
                    "medal", medal,
                    "key", key,
                    "mutagen", mutagen,
                    "antitoxin", antitoxin,
                    "bandage", bandage,
                    "vip", vip,
                    "exp", exp,
                    "bundleCode", bundleCode,
                    "reagents", reagents,
                    "weaponShoots", weaponShoots,
                    "temporalItems", temporalItems,
                    "bundleItems", bundleItems,
                    "adminUser", request.adminUser,
                    Param.note, note,
            )
            profile.dirty = true
            profileService.updateSync(profile)
        }
    }
    return "${grantedProfiles.join(",")}"
}

def updateAchievements(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def achieveCommandService = context.getBean(AchieveCommandService.class)
    def profileService = context.getBean(ProfileService.class)
    def softCache = context.getBean(SoftCache.class)
    Map<String, Object> map = new InteropSerializer().fromString(request.scriptParams, Map.class)

    int profileId = map.profileId as int
    boolean cleanInvestedAwardPoints = map.investedAwardPoints != null ? map.investedAwardPoints as int == 0 : false
    Map<Integer, Boolean> boolAchievements = map.boolAchievements != null ? map.boolAchievements as Map : [:] as Map
    def achievementsIndex = []
    def achievementsValues = []

    map.achievements.each {
        achievementsIndex.add(it.key)
        achievementsValues.add(it.value)
    }

    println(new TreeMap(map.achievements as Map))

    def stringId = profileService.getProfileAchieveId(profileId)
    ProfileAchievements profileAchievements = softCache.get(WormixAchievements.class, stringId)
    if (profileAchievements != null && map.investedAwardPoints != null && !cleanInvestedAwardPoints) {
        achieveCommandService.setInvestedAwardPoints(stringId, profileAchievements.class, map.investedAwardPoints as byte)
    }
    achieveCommandService.setAchievements(stringId, cleanInvestedAwardPoints, achievementsIndex as int[], achievementsValues as int[], boolAchievements, WormixAchievements.class)

    def userProfile = profileService.getUserProfile(profileId)
    if (userProfile)
        userProfile.grantedAchieveAwards = [] as int[]

    if (cleanInvestedAwardPoints) {
        if (userProfile) {
            def purchaseService = context.getBean(PurchaseService.class)
            purchaseService.removeBonusItems(userProfile)
            userProfile.cleanGrantedAchieveAwards()
            profileService.updateSync(userProfile)
        }
    }
    "0"
}

def updateReagents(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    def profileService = context.getBean(ProfileService.class);

    String profileId = map.profileId
    UserProfile profile = getUserProfile(context, "" + map.profileId)

    ReagentsEntity reagents = profile.getReagents()
    map.reagents.each { reagents.setReagentValue(it.key as byte, it.value as int) }
    reagents.dirty = true

    profileService.updateSync(profile)

    return "[${profileId}] reagents successfully updated"
}

def cleanPaymetsStat(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)

    TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class)
    JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class)

    transactionTemplate.execute({ status ->
        jdbcTemplate.update("delete from wormswar.wipe_statistic where profile_id = ${map.profileId}")
        jdbcTemplate.update("delete from wormswar.shop_statistic where profile_id = ${map.profileId}")
    } as TransactionCallbackWithoutResult)

    return "[${map.profileId}] payments stat cleaned"
}

def UserProfile getUserProfile(ApplicationContext context, String profileId) {
    ProfileService profileService = context.getBean(ProfileService.class)
    UserProfile profile = profileService.getUserProfile(profileId)

    if (profile == null) {
        throw new ExecAdminScriptException(ServiceResult.ERR_PROFILE_NOT_FOUND, "UserProfile not found by id ${profileId}")
    }
    return profile
}