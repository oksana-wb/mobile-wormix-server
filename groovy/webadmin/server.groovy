import com.google.gson.Gson
import com.pragmatix.app.common.Locale
import com.pragmatix.app.domain.AppParamsEntity
import com.pragmatix.app.domain.BanEntity
import com.pragmatix.app.domain.RestrictionEntity
import com.pragmatix.app.init.InitVkontakteServiceInProduction
import com.pragmatix.app.init.StuffCreator
import com.pragmatix.app.init.WeaponsCreator
import com.pragmatix.app.model.UserProfile
import com.pragmatix.app.services.*
import com.pragmatix.app.settings.AppParams
import com.pragmatix.craft.services.CraftService
import com.pragmatix.gameapp.GameApp
import com.pragmatix.gameapp.services.OnlineService
import com.pragmatix.gameapp.sessions.Session
import com.pragmatix.webadmin.AdminHandler
import com.pragmatix.wormix.webadmin.interop.CommonResponse
import com.pragmatix.wormix.webadmin.interop.InteropSerializer
import com.pragmatix.wormix.webadmin.interop.ServiceResult
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest
import com.pragmatix.wormix.webadmin.interop.response.structure.*
import org.springframework.context.ApplicationContext

import javax.persistence.EntityManager
import javax.persistence.Query

def ping(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    String params = request.scriptParams
    InteropSerializer serializer = new InteropSerializer()
    String value = serializer.fromString(params, String.class)
    console.print("OK")
    serializer.toString("Pong(param=" + value + ")")
}

def String weaponType(com.pragmatix.app.model.Weapon weapon) {
    switch (weapon.type) {
        case com.pragmatix.app.model.Weapon.WeaponType.INFINITE: return "беск."
        case com.pragmatix.app.model.Weapon.WeaponType.CONSUMABLE: return "штч."
        case com.pragmatix.app.model.Weapon.WeaponType.SEASONAL: return "сез."
        case com.pragmatix.app.model.Weapon.WeaponType.COMPLEX: return "сост.[${weapon.maxWeaponLevel}]"
    }
    return ""
}

def getContext(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    List<Weapon> weapons = new ArrayList<>()
    WeaponsCreator weaponsCreator = context.getBean(WeaponsCreator.class)
    weaponsCreator.weapons.each {
        Weapon weapon = new Weapon()
        weapon.id = it.weaponId
        weapon.name = it.name + " (" + weaponType(it) + ")"
        weapon.price = it.price
        weapon.realprice = it.realprice
        weapon.requiredLevel = it.requiredLevel
        weapon.infinite = it.isType(com.pragmatix.app.model.Weapon.WeaponType.INFINITE)
        weapons.add(weapon)
    }

    List<Stuff> stuffs = new ArrayList<>()
    StuffCreator stuffCreator = context.getBean(StuffCreator.class)
    stuffCreator.stuffs.each {
        Stuff stuff = new Stuff()
        stuff.name = it.name
        stuff.id = it.stuffId
        stuff.price = it.price
        stuff.realprice = it.realprice
        stuff.requiredLevel = it.requiredLevel
        stuff.hp = it.hp
        stuff.reaction = it.reaction
        stuff.kit = it.kit
        stuff.boost = it.boost
        stuff.temporal = it.temporal
        stuff.expire = it.expireTime
        stuffs.add(stuff)
    }

    List<Recipe> recipes = new ArrayList<>()
    CraftService craftService = context.getBean(CraftService.class)
    craftService.getAllRecipesMap().values().each {
        Recipe recipe = new Recipe()
        recipe.id = it.id
        recipe.baseRecipeId = it.baseRecipeId
        recipe.name = it.name
        recipe.weaponId = it.weaponId
        recipe.requiredLevel = it.needLevel()
        List<ReagentRef> reagents = new ArrayList<>()
        it.reagentsMap.entrySet().each {
            ReagentRef reagent = new ReagentRef()
            reagent.reagentId = it.key
            reagent.quantity = it.value
            reagents.add(reagent)
        }
        recipe.reagentRefs = reagents.toArray(new ReagentRef[reagents.size()])
        recipes.add(recipe)
    }

    List<Reagent> reagents = new ArrayList<>()
    com.pragmatix.craft.domain.Reagent.values().each {
        Reagent reagent = new Reagent()
        reagent.id = it.index
        reagent.name = it.name()
        reagent.priceInMoney = craftService.getReagentsPrice().getOrDefault(it, -1)
        reagents.add(reagent)
    }

    SkinService skinService = context.getBean(SkinService.class)
    List<Skin> skins = skinService.skinsMap.values().collect {
        Skin skin = new Skin()
        skin.id = it.id
        skin.targetRaceId = it.targetRace.type
        skin
    }

    Map resultMap = new LinkedHashMap<String, Object>();
    resultMap.put("weapons", weapons.toArray(new Weapon[weapons.size()]))
    resultMap.put("stuffs", stuffs.toArray(new Stuff[stuffs.size()]))
    resultMap.put("recipes", recipes.toArray(new Recipe[recipes.size()]))
    resultMap.put("reagents", reagents.toArray(new Reagent[reagents.size()]))
    resultMap.put("currentTimeMillis", System.currentTimeMillis())
    resultMap.put("skins", skins.toArray(new Skin[skins.size()]))

    OnlineService onlineService = context.getBean(OnlineService.class)
    resultMap.put("online", onlineService.get())

    resultMap.put("fingerPrint", onlineService.hashCode())

    InteropSerializer serializer = new InteropSerializer();

    console.print("OK")

    serializer.toString(resultMap)
}

def getBanStatistic(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    // исторя банов за период
    ProfileService profileService = context.getBean(ProfileService.class)
    BanService banService = context.getBean(BanService.class)
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> params = serializer.fromString(request.scriptParams, Map.class)
    Date fromDate = new Date(params.get("fromDate") as long)
    Date toDate = new Date(params.get("toDate") as long)
    int type = (params.get("type") ?: -1) as Integer
    String admin = (params.get("admin") ?: "") as String
    String note = (params.get("note") ?: "") as String
    int banned = params.get("banned") as int

    DaoService daoService = context.getBean(DaoService.class)
    EntityManager em = daoService.getBanDao().em;
    String qStr = "select e from BanEntity e where date >= :fromDate and date < :toDate "
    if (type >= 0) {
        qStr += " and type = :type "
    }
    if (!admin.isEmpty()) {
        if (admin.equals("!server")) {
            qStr += " and admin != 'server' "
        } else {
            qStr += " and admin = :admin "
        }
    }
    if (!note.isEmpty()) {
        qStr += " and upper(note) like :note "
    }
    Query query = em.createQuery(qStr)
    query.setParameter("fromDate", fromDate)
    query.setParameter("toDate", toDate)
    if (type >= 0) {
        query.setParameter("type", type)
    }
    if (!admin.isEmpty() && !admin.equals("!server")) {
        query.setParameter("admin", admin)
    }
    if (!note.isEmpty()) {
        query.setParameter("note", "%" + note.toUpperCase() + "%")
    }

    def bans = query.getResultList().findAll { BanEntity it ->
        banned == -1 || (banned == 0 && !banService.isBanned(it.profileId)) || (banned == 1 && banService.isBanned(it.profileId))
    }.collect { BanEntity it ->
        BanItem item = new BanItem()
        item.startDate = it.date
        item.endDate = it.endDate
        item.note = it.note
        item.adminUser = it.admin
        item.type = it.type
        item.attachments = it.attachments
        item.profileId = profileService.getProfileSocialId(it.profileId)

        item
    }
    bans.addAll(getRestrictions(context, fromDate, toDate, type, admin, note, banned))
    serializer.toString(bans.toArray(new BanItem[bans.size()]))
}

List<BanItem> getRestrictions(ApplicationContext context, Date fromDate, Date toDate, int type, String admin, String note, int banned) {
    def profileService = context.getBean(ProfileService.class)
    def restrictionService = context.getBean(RestrictionService.class)

    def daoService = context.getBean(DaoService.class)
    def em = daoService.getBanDao().em;
    String qStr = "select e from RestrictionEntity e where startDate >= :fromDate and startDate < :toDate "
    if (type >= 0) {
        qStr += " and reason = :type "
    }
    if (!admin.isEmpty() && admin != "!server") {
        qStr += " and history like :admin "
    }
    if (!note.isEmpty()) {
        qStr += " and upper(history) like :note "
    }
    def query = em.createQuery(qStr)
    query.setParameter("fromDate", fromDate)
    query.setParameter("toDate", toDate)
    if (type >= 0) {
        query.setParameter("type", type)
    }
    if (!admin.isEmpty() && admin != "!server") {
        query.setParameter("admin", "%\"admin\":\"" + admin + "\"%")
    }
    if (!note.isEmpty()) {
        query.setParameter("note", "%\"NOTE\":\"%" + note.toUpperCase() + "%\"%")
    }

    def restrictions = query.getResultList().findAll { RestrictionEntity it ->
        banned == -1 || (banned == 0 && restrictionService.getRestrictions(it.profileId).isEmpty()) || (banned == 1 && !restrictionService.getRestrictions(it.profileId).isEmpty())
    }.collect { RestrictionEntity it ->
        List historyItems = new Gson().fromJson(it.history, List.class)
        BanItem item = new BanItem()
        item.startDate = it.startDate
        item.endDate = it.endDate
        item.note = historyItems.get(0).note
        item.adminUser = historyItems.get(0).admin
        item.type = historyItems.get(0).operation
        item.profileId = profileService.getProfileSocialId(it.profileId)

        item
    }
    restrictions
}

def getOnline(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    OnlineService onlineService = context.getBean(OnlineService.class);
    return "" + onlineService.get();
}

def refreshContext(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    updateOnlineCounter(context)

    Map resultMap = new LinkedHashMap<String, Object>();
    OnlineService onlineService = context.getBean(OnlineService.class)
    resultMap.put("online", onlineService.get())
    resultMap.put("fingerPrint", onlineService.hashCode())
    resultMap.put("currentTimeMillis", System.currentTimeMillis())

    InteropSerializer serializer = new InteropSerializer()
    return serializer.toString(resultMap);
}

def updateOnlineCounter(ApplicationContext context) {
    GameApp gameApp = context.getBean(GameApp.class)
    OnlineService onlineService = context.getBean(OnlineService.class)
    def sessionsBySessionKeys = gameApp.getSessions().sessionsBySessionKeys
    int online = 0

    for (Session session : sessionsBySessionKeys.values()) {
        if (session.hasConnections()) {
            if (session.user instanceof UserProfile) {
                online++
            }
        } else {
            sessionsBySessionKeys.remove(session.key)
        }
    }
    onlineService.online.set(online)
}

def getBonusDays(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def appParams = context.getBean(AppParams.class);
    def result = appParams.getBonusPeriodSettings().collect { bonusPeriodSettings ->
        [
                id           : bonusPeriodSettings.id,
                startDate    : bonusPeriodSettings.startBonusDay,
                endDate      : bonusPeriodSettings.endBonusDay,
                levelMin     : bonusPeriodSettings.levelMin,
                levelMax     : bonusPeriodSettings.levelMax,
                message      : bonusPeriodSettings.getBonusMessage(Locale.RU),
                messageEn    : bonusPeriodSettings.getBonusMessage(Locale.EN),
                money        : bonusPeriodSettings.money,
                realMoney    : bonusPeriodSettings.realMoney,
                battlesCount : bonusPeriodSettings.exactBattlesCount,
                wagerToken   : bonusPeriodSettings.wagerWinAwardToken,
                bossToken    : bonusPeriodSettings.bossWinAwardToken,
                keysCount    : bonusPeriodSettings.reagents.getOrDefault(com.pragmatix.craft.domain.Reagent.prize_key.index, 0),
                reagents     : bonusPeriodSettings.reagents.entrySet().collect { "${it.key}:${it.value}" }.sort().join(", "),
                weaponShoots : bonusPeriodSettings.awardItemsStr,
                temporalItems: bonusPeriodSettings.temporalItems,
                reaction     : bonusPeriodSettings.reactionRate,
        ]
    }
    AdminHandler.xstream.toXML(result)
}

def setBonusDays(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def appParams = context.getBean(AppParams.class)
    def daoService = context.getBean(DaoService.class)
    def appParamsDao = daoService.getAppParamsDao()

    def serializer = new InteropSerializer()
    Map<String, Object> params = serializer.fromString(request.scriptParams, Map.class)

    AppParamsEntity entity
    Integer id = params.id as Integer
    def action = params.action as String

    if (action in ["modify", "remove"] && id) {
        entity = appParamsDao.selectAppParams(id)
    } else {
        entity = new AppParamsEntity()
    }
    if (entity != null) {
        if ("remove" == action) {
            daoService.doInTransactionWithoutResult { appParamsDao.deleteById(entity.id) }
        } else {
            entity.setStartBonusDay(new Date(params.startDate as long))
            entity.setEndBonusDay(new Date(params.endDate as long))
            entity.setLevelMin(params.levelMin as Integer)
            entity.setLevelMax(params.levelMax as Integer)
            entity.setBonusMoney(params.money as Integer)
            entity.setBonusRealMoney(params.realMoney as Integer)
            entity.setBonusBattlesCount(params.battlesCount as Integer)
            entity.setWagerToken(params.wagerToken as Integer)
            entity.setBossToken(params.bossToken as Integer)
            entity.setKeysCount(params.keysCount as Integer)
            entity.setReagents(params.reagents as String)
            entity.setWeaponShoots(params.weaponShoots as String)
            entity.setTemporalItems(params.temporalItems as String)
            entity.setReaction(params.reaction as Integer)

            entity.setMessage(params.message as String)
            entity.setMessageEn(params.messageEn as String)

            daoService.doInTransactionWithoutResult {
                if ("insert" == action && !entity.id) {
                    entity.id = daoService.jdbcTemplate.queryForObject("select max(id) from wormswar.app_params", Integer.class) + 1
                }
                appParamsDao.update(entity)
            }
        }

        appParams.init()

        AdminHandler.xstream.toXML([
                action: params.action,
                id    : entity.id,
        ])
    } else {
        return new CommonResponse(ServiceResult.ERR_RUNTIME, "entity not found", "")
    }
}

def getAppParams(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def appParams = context.getBean(AppParams.class);

    Map resultMap = new LinkedHashMap<String, Object>();
    resultMap.put("version", appParams.getVersionAsString());

    def serializer = new InteropSerializer()
    return serializer.toString([
            version     : appParams.getVersionAsString(),
            vkAuthSecret: appParams.vkAuthSecret,
    ]);
}

def setAppParams(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def appParams = context.getBean(AppParams.class);
    def daoService = context.getBean(DaoService.class);

    def serializer = new InteropSerializer();
    Map<String, Object> params = serializer.fromString(request.scriptParams, Map.class);
    AppParamsEntity appParamsEntity = daoService.getAppParamsDao().selectAppParams();
    if (appParamsEntity != null) {
        appParamsEntity.appVersion = params.version as String
        appParamsEntity.vkAuthSecret = (params.vkAuthSecret as String) ?: ""

        daoService.doInTransactionWithoutResult { daoService.getAppParamsDao().update(appParamsEntity) }

        // загружаем из бызы для верности
        appParams.init()

        if (context.containsBean("initVkontakteServiceInProduction")) {
            context.getBean(InitVkontakteServiceInProduction.class).init()
        }
        return serializer.toString([
                version     : appParams.getVersionAsString(),
                vkAuthSecret: appParams.vkAuthSecret,
        ]);
    } else {
        return new CommonResponse(ServiceResult.ERR_RUNTIME, "appParamsEntity not found", "");
    }
}

