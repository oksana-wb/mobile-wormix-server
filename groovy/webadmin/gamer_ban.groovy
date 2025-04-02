import com.pragmatix.app.domain.BanEntity
import com.pragmatix.app.model.BanItem
import com.pragmatix.app.model.RestrictionItem
import com.pragmatix.app.model.UserProfile
import com.pragmatix.app.services.*
import com.pragmatix.app.services.rating.RatingService
import com.pragmatix.gameapp.services.TaskService
import com.pragmatix.gameapp.sessions.Sessions
import com.pragmatix.pvp.BattleWager
import com.pragmatix.webadmin.AdminHandler
import com.pragmatix.webadmin.ExecAdminScriptException
import com.pragmatix.wormix.webadmin.interop.CommonResponse
import com.pragmatix.wormix.webadmin.interop.InteropSerializer
import com.pragmatix.wormix.webadmin.interop.ServiceResult
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest
import com.pragmatix.wormix.webadmin.interop.response.structure.BanType
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.transaction.support.TransactionCallback

import java.sql.ResultSet

def batchBan(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    return batchBanOrUnbun(context, request, console, true)
}

def batchUnban(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    return batchBanOrUnbun(context, request, console, false)
}

private batchBanOrUnbun(ApplicationContext context, ExecScriptRequest request, PrintWriter console, boolean needBan) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    String[] profiles = (map.get("profiles") as String).split(" ");
    // статья бана
    int reason = map.get("reason") as Integer ?: 0;
    // длительность бана дней, 0 - навсегда
    int durationInDays = map.get("durationInDays") as Integer ?: 0;
    // причина бана, заметка от админа
    String note = map.get("note") ?: "";
    // если есть доказательсва (изображение)
    String attachments = map.get("attachments") ?: "";

    if (note.isEmpty()) {
        return new CommonResponse(ServiceResult.ERR_RESPONSE, "note is empty", "1");
    }

    BanService banService = context.getBean(BanService.class)
    TaskService taskService = context.getBean(TaskService.class)
    DaoService daoService = context.getBean(DaoService.class)
    ProfileService profileService = context.getBean(ProfileService.class)
    JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class)

    final List<Runnable> transactionTasksList = new ArrayList<Runnable>();

    String result;
    int rebanned = 0;
    int banned = 0;
    int unbanned = 0;
    int skipped = 0;
    int absent = 0;
    List<Object[]> resultsList = new ArrayList<Object[]>();
    Set<Long> profileIds = new HashSet<>()
    String ids = "";
    // переводим стринги в лонги
    for (String profileId : profiles) {
        Long profileLongId = profileService.getProfileLongId(profileId)
        if (profileLongId != null) {
            profileIds.add(profileLongId);
        }
        ids += "," + profileLongId;
        resultsList.add([profileId, "", profileLongId, 0] as Object[]);
    }

    Map<Long, Integer> prevBans = new HashMap<>()
    ids = ids.replaceFirst(",", "")
    jdbcTemplate.query("select profile_id, count(*) from wormswar.ban_list where profile_id in (${ids}) group by 1", { ResultSet res ->
        prevBans.put(res.getLong(1), res.getInt(2))
    } as RowCallbackHandler)

    // проверяем профили по базе
    Collection<Long> existenProfiles = daoService.getUserProfileDao().checkProfiles(profileIds);
    for (Object[] resultItem : resultsList) {
        Long profileLongId = resultItem[2] as Long;
        resultItem[3] = prevBans.get(profileLongId) ?: 0
        if (profileLongId == null || !existenProfiles.contains(profileLongId)) {
            resultItem[1] = "absent"
            absent++;
            continue;
        }
        if (needBan) {
            // баним, с сохранением истории банов
            com.pragmatix.app.model.BanItem ban = banService.get(profileLongId);
            if (ban != null) {
                // баним навсегда, но игрок уже и так в вечном бане
                if (durationInDays <= 0 && ban.getEndDate() == null) {
                    resultItem[1] = "skipped"
                    skipped++;
                } else if (durationInDays <= 0 && ban.getEndDate() != null) {
                    // перебаниваем навсегда
                    resultItem[1] = "rebanned"
                    rebanned++;
                    // сначала выполняем разбан
                    transactionTasksList.add(changeBanDurationReturnTask(ban, profileLongId, 0, "will banned again", request.adminUser, context));
                    // потом баним
                    transactionTasksList.add(addToBanListReturnTask(profileLongId, reason, 0, note, request.adminUser, attachments, context));
                } else {
                    long endDate = System.currentTimeMillis() + (durationInDays * 24l * 60l * 60l * 1000l);
                    if (ban.getEndDate() == null || ban.getEndDate() > endDate) {
                        // находится в бане по боллее тяжелой статье
                        resultItem[1] = "skipped"
                        skipped++;
                    } else {
                        // перебаниваем на новый срок
                        resultItem[1] = "rebanned"
                        rebanned++;
                        // сначала выполняем разбан
                        transactionTasksList.add(changeBanDurationReturnTask(ban, profileLongId, 0, "will banned again", request.adminUser, context));
                        // потом баним
                        transactionTasksList.add(addToBanListReturnTask(profileLongId, reason, durationInDays, note, request.adminUser, attachments, context));
                    }
                }
            } else {
                transactionTasksList.add(addToBanListReturnTask(profileLongId, reason, durationInDays, note, request.adminUser, attachments, context));
                resultItem[1] = "banned"
                banned++;
                def profile = profileService.getUserProfile(profileLongId, false)
                if (profile) {
                    def session = Sessions.get(profile)
                    if (session) {
                        session.close()
                    }
                }
            }
        } else {
            // снимаем бан, с сохранением истории банов
            com.pragmatix.app.model.BanItem ban = banService.get(profileLongId);
            if (ban != null) {
                resultItem[1] = "unbanned"
                unbanned++;
                transactionTasksList.add(changeBanDurationReturnTask(ban, profileLongId, 0, note, request.adminUser, context));
            } else {
                resultItem[1] = "skipped"
                skipped++;
            }
        }
    }

    console.printf("successfully banned: %s, rebanned: %s, unbanned: %s, skipped: %s\n", banned, rebanned, unbanned, skipped);

    if (!transactionTasksList.isEmpty()) {
        taskService.addTransactionTask({ status ->
            for (Runnable task : transactionTasksList) {
                task.run();
            }
            return null;
        } as TransactionCallback);
    }

    StringBuilder sb = new StringBuilder();
    resultsList.each {
        sb.append(it[0]).append(' ').append(it[1]).append(' ').append(it[3]).append('\n')
    }
    return sb.toString();
}

def banProfile(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def dailyRegistry = context.getBean(DailyRegistry.class)
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    String profileId = map.get("profileId")
    // статья бана
    int reason = map.get("reason") as int
    // длительность бана дней, 0 - навсегда
    int durationInDays = map.get("durationInDays") as int
    // причина бана, заметка от админа
    String note = map.get("note") ?: ""
    // если есть доказательсва (изображение)
    String attachments = map.get("attachments") ?: ""

    UserProfile profile = getUserProfile(context, profileId)

    BanService banService = context.getBean(BanService.class)
    RatingService ratingService = context.getBean(RatingService.class)

    BanItem ban = banService.get(profile.getId())
    if (ban != null) {
        return new CommonResponse(ServiceResult.ERR_RESPONSE, "[${profileId}] already banned", "2")
    }
    // проверяем, что админ написал причину бана
    if (note.isEmpty()) {
        return new CommonResponse(ServiceResult.ERR_RESPONSE, "note is empty", "1")
    }
    if (reason == 0) {
        reason = BanType.BAN_BY_ADMIN.type
    }
    banService.addToBanList(profile.getId(), reason, durationInDays, note, request.adminUser, attachments)
    ratingService.onBan(profile.getId())
    BattleWager.values().each { battleWager -> dailyRegistry.setDailyRating(profile.getId(), 0, battleWager) }

    def session = Sessions.get(profile)
    if (session) {
        session.close()
    }

    return "[${profileId}] successfully banned"
}

def unbanProfile(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> map = serializer.fromString(request.scriptParams, Map.class)
    String profileId = map.get("profileId");
    // когда заканчивается срок бана в днях, 0 - бан снимается немедленно
    int newDurationInDays = map.get("durationInDays") as int;
    // причина бана, заметка от админа
    String note = map.get("note") ?: "";

    UserProfile profile = getUserProfile(context, profileId)

    BanService banService = context.getBean(BanService.class)

    // проверяем, что админ написал причину
    if (note.isEmpty()) {
        return new CommonResponse(ServiceResult.ERR_RESPONSE, "note is empty", "1");
    }

    com.pragmatix.app.model.BanItem ban = banService.get(profile.getId());
    if (ban != null) {
        changeBanDuration(ban, profile.getId(), newDurationInDays, note, request.adminUser, context);
        String action = newDurationInDays <= 0 ? "unbanned" : "changed ban duration";
        return "[${profileId}] successfully ${action}";
    } else {
        return new CommonResponse(ServiceResult.ERR_RESPONSE, "${profileId}] active bun not found", "3");
    }
}

def getRestrictionsHistory(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    DaoService daoService = context.getBean(DaoService.class)
    def params = AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, Object>
    Long profileId = params.profileId as Long
    def entities = daoService.restrictionDao.getRestrictionsHistoryFor(profileId)
    console.println("Found ${entities.size()} restriction items for ${profileId}")
    def result = entities.collect {
        def item = new RestrictionItem(it)
        [
                restrictionId: item.id,
                profileId    : item.profileId,
                blocks       : item.blocks,
                startDate    : item.startDate,
                endDate      : item.endDate,
                expired      : item.expired,
                reason       : item.reason,
                history      : item.history?.collect {
                    [
                            operation : it.operation,
                            date      : it.date,
                            newEndDate: it.newEndDate,
                            note      : it.note,
                            admin     : it.admin,
                    ]
                }
        ]
    }
    return AdminHandler.xstream.toXML(result)
}

def addRestriction(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    RestrictionService restrictionService = context.getBean(RestrictionService.class)
    def params = AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, Object>
    String profileId = params.profileId as String
    UserProfile profile = getUserProfile(context, profileId)

    int reason = (params.reason as Integer) ?: BanType.BAN_BY_ADMIN.type
    def blocks = params.blocks as short

    // проверяем, что админ написал причину
    def note = params.note as String
    if (note == null || note.isEmpty()) {
        return new CommonResponse(ServiceResult.ERR_INVALID_ARGUMENT, "note is empty", "note=${note.inspect()}");
    }

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
            return new CommonResponse(ServiceResult.ERR_INVALID_ARGUMENT, "gamer [$profileId] already restricted for [${RestrictionItem.BlockFlag.mkString(alreadyBlocked)}]", "blocks=[${RestrictionItem.BlockFlag.mkString(blocks)}]");
        } else {
            return new CommonResponse(ServiceResult.ERR_RESPONSE, "failed to save restriction", "1");
        }
    }
    String result = "OK [$profileId] successfully restricted for [${RestrictionItem.BlockFlag.mkString(newRestriction.blocks)}], resctrictionId=[${newRestriction.id}]"
    return AdminHandler.xstream.toXML(result)
}

def editRestriction(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    RestrictionService restrictionService = context.getBean(RestrictionService.class)
    def params = AdminHandler.xstream.fromXML(request.scriptParams) as Map<String, Object>

    def profileId = params.profileId as Long
    def restrictionId = params.restrictionId as int
    boolean success = restrictionService.changeRestrictionDuration(
            profileId,
            restrictionId,
            params.days as Long,
            params.note as String,
            request.adminUser,
    )
    if (!success) {
        def restriction = restrictionService.getRestrictions(profileId).find { it.id == restrictionId }
        if (!restriction) {
            return new CommonResponse(ServiceResult.ERR_INVALID_ARGUMENT, "NOT FOUND: gamer [$profileId] has no restriction [$restrictionId]", "restrictionId=[$restrictionId]");
        } else {
            return new CommonResponse(ServiceResult.ERR_RESPONSE, "failed to update restriction [$restrictionId]", "1");
        }
    }
    String result = "OK [$profileId] restriction [$restrictionId] successfully updated"
    return AdminHandler.xstream.toXML(result)
}

public void changeBanDuration(final BanItem ban, Long profileId, Integer durationInDays, String note, String admin, ApplicationContext context) {
    TaskService taskService = context.getBean(TaskService.class);
    Runnable updateTask = changeBanDurationReturnTask(ban, profileId, durationInDays, note, admin, context);
    taskService.addTransactionTask({ status -> updateTask.run() } as TransactionCallback);
}

/**
 * метод разбанит игрока, и вернет задачу которую нужно будет выполнить в транзакции
 */
public Runnable changeBanDurationReturnTask(final BanItem ban, Long profileId, Integer durationInDays, String note, String admin, ApplicationContext context) {
    BanService banService = context.getBean(BanService.class);
    DaoService daoService = context.getBean(DaoService.class);

    def banList = banService.getBanList();
    String action = "Бан снят";
    if (durationInDays <= 0) {
        // снимаем бан однозначно
        ban.setEndDate(System.currentTimeMillis());
        banList.remove(profileId);
        restoreInTop(profileId, context);
    } else {
        // пробыл в бане
        long wasBanned = System.currentTimeMillis() - ban.getStartDate();
        // новый срок
        long newBanPeriod = durationInDays * 24l * 60l * 60l * 1000l;
        // осталость просидеть в бане
        long leftBan = newBanPeriod - wasBanned;
        if (leftBan <= 0) {
            // отсидел уже, так сказать
            // снимаем бан однозначно
            ban.setEndDate(System.currentTimeMillis());
            banList.remove(profileId);
            restoreInTop(profileId, context);
        } else {
            // осталось немного доситеть
            ban.setEndDate(System.currentTimeMillis() + leftBan);
            action = "Бан изменен";
        }
    }

    // дописываем комментарий
    String newNote = String.format("%s%s (%s), причина - %s", (ban.getNote() == null ? "" : ban.getNote() + "; "), action, admin, note);
    ban.setNote(newNote);

    { -> daoService.getBanDao().update(new BanEntity(ban)) } as Runnable
}

def restoreInTop(Long profileId, ApplicationContext context) {
    def ratingService = context.getBean(RatingService.class)
    def profile = context.getBean(ProfileService.class).getUserProfile(profileId)
    ratingService.onUpdateRating(profile, true)
}


public void addToBanList(Long profileId, int cheatReason, int durationInDays, String note, String admin, String attachments, ApplicationContext context) {
    BanService banService = context.getBean(BanService.class);
    TaskService taskService = context.getBean(TaskService.class);
    // если игрок уже находится в бане
    BanItem ban = banService.get(profileId);
    if (ban != null) {
//        log.warn("user already banned! {}", ban);
        return;
    }

    Runnable insertTask = addToBanListReturnTask(profileId, cheatReason, durationInDays, note, admin, attachments, context);

    taskService.addTransactionTask({ status -> insertTask.run() } as TransactionCallback);
}

public Runnable addToBanListReturnTask(Long profileId, int cheatReason, Integer durationInDays, String note, String admin, String attachments, ApplicationContext context) {
    def daoService = context.getBean(DaoService.class);
    def banService = context.getBean(BanService.class);
    def ratingService = context.getBean(RatingService.class);
    def dailyRegistry = context.getBean(DailyRegistry.class);

    def banList = banService.getBanList();
    Long startDate = new Date().getTime();
    Long endDate;
    if (durationInDays == null || durationInDays == 0) {
        endDate = null;
    } else {
        endDate = startDate + (durationInDays * 24l * 60l * 60l * 1000l);
    }
    final BanItem banItem = new BanItem(profileId, startDate, cheatReason, endDate, note, admin, attachments);
    banList.put(profileId, banItem);

    ratingService.onBan(profileId)
    BattleWager.values().each { battleWager -> dailyRegistry.setDailyRating(profileId, 0, battleWager) }

    return { ->
        BanEntity banEntity = new BanEntity(banItem);
        daoService.getBanDao().insert(banEntity);
        banItem.setId(banEntity.getId());
    } as Runnable
}

def UserProfile getUserProfile(ApplicationContext context, String profileId) {
    ProfileService profileService = context.getBean(ProfileService.class)
    UserProfile profile = profileService.getUserProfile(profileId)

    if (profile == null) {
        throw new ExecAdminScriptException(ServiceResult.ERR_PROFILE_NOT_FOUND, "UserProfile not found by id ${profileId}")
    }
    return profile
}


