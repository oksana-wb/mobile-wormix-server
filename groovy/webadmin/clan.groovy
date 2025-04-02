import com.pragmatix.app.services.BanService
import com.pragmatix.app.services.ProfileService
import com.pragmatix.clan.ClanInteropServiceImpl
import com.pragmatix.clanserver.common.ClanActionEnum
import com.pragmatix.clanserver.dao.DAO
import com.pragmatix.clanserver.domain.Clan
import com.pragmatix.clanserver.domain.ClanMember
import com.pragmatix.clanserver.domain.Rank
import com.pragmatix.clanserver.domain.ReviewState
import com.pragmatix.clanserver.messages.request.*
import com.pragmatix.clanserver.messages.response.ChangeClanReviewStateResponse
import com.pragmatix.clanserver.messages.response.ListClansResponse
import com.pragmatix.clanserver.messages.structures.ClanTO
import com.pragmatix.clanserver.services.*
import com.pragmatix.common.utils.VarObject
import com.pragmatix.webadmin.AdminHandler
import com.pragmatix.webadmin.ExecAdminScriptException
import com.pragmatix.wormix.webadmin.interop.InteropSerializer
import com.pragmatix.wormix.webadmin.interop.ServiceResult
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler

import java.sql.ResultSet

def getClanList(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ClanServiceImpl clanService = context.getBean(ClanServiceImpl.class)

    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> params = serializer.fromString(request.scriptParams, Map.class)

    ListClansRequest req = new ListClansRequest()
    req.searchPhrase = params.searchPhrase ?: ""
    req.limit = (params.limit ?: 25) as Integer
    req.offset = (params.offset ?: 0) as Integer
    String reviewState = params.reviewState ?: "" as String
    req.reviewStates = (!reviewState.isEmpty() ? [ReviewState.valueOf(reviewState)] : []) as ReviewState[];
    ListClansResponse resp = clanService.listClansOrderByName(req)

    List<Map> clanList = new ArrayList<>()
    for (ClanTO clanTO : resp.clans) {
        clanList.add(newClanStructure(clanTO))
    }

    Map resultMap = new LinkedHashMap<String, Object>();
    resultMap.put("clans", clanList.toArray(new Map[clanList.size()]))

    AdminHandler.xstream.toXML(resultMap)
}

def getClan(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ClanServiceImpl clanService = context.getBean(ClanServiceImpl.class)
    BanService banService = context.getBean(BanService.class)

    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> params = serializer.fromString(request.scriptParams, Map.class)

    int clanId = params.clanId as Integer;
    Clan clanModel = clanService.getClan(clanId)
    if (clanModel == null) {
        throw new ExecAdminScriptException(ServiceResult.ERR_CLAN_NOT_FOUND, "Clan not found by id ${clanId}")
    }

    Map<String, Object> clan = newClanStructure(clanModel)
    int i = 0;
    for (ClanMember clanMember : clanModel.members()) {
        (clan.members as Map[])[i] = newMember(clanMember, banService.isBanned(clanMember.profileId))
        i++
    }

    Map resultMap = new LinkedHashMap<String, Object>();
    resultMap.put("clan", clan)

    AdminHandler.xstream.toXML(resultMap)
}

def getActions(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def _ = new RequestContext(context, request)
    def result = []
    def rowCallbackHandler = { ResultSet res ->
        result += [
                date        : res.getTimestamp("date"),
                action      : res.getInt("action"),
                publisher_id: res.getInt("publisher_id"),
                member_id   : res.getInt("member_id"),
                param       : res.getInt("param"),
                treas       : res.getInt("treas"),
        ]
    } as RowCallbackHandler
    if (_.params.clanAction != null && (_.params.clanAction as Integer) != 0) {
        _.jdbcTemplate.query("select * from clan.audit where clan_id = ${_.params.clanId} and action = ${_.params.clanAction} order by date desc", rowCallbackHandler)
    } else {
        _.jdbcTemplate.query("select * from clan.audit where clan_id = ${_.params.clanId} and date > now() - interval '60 DAYS' order by date desc", rowCallbackHandler)
    }
    AdminHandler.xstream.toXML(result)
}

def updateClan(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def _ = new RequestContext(context, request)

    def clanId = _.params.id as Integer
    def clan = _.clanService.getClan(clanId)
    if (clan == null) {
        throw new ExecAdminScriptException(ServiceResult.ERR_CLAN_NOT_FOUND, "Clan not found by id ${clanId}")
    }

    def clanTO = new ClanTO(clan, _.ratingService);

    def name = _.params.name as String
    def description = _.params.description as String
    def level = _.params.level as Integer
    def joinRating = _.params.joinRating as Integer
    def reviewState = _.params.reviewState as com.pragmatix.wormix.webadmin.interop.response.structure.clan.ReviewState

    def closed = _.params.closed as Boolean
    def treas = _.params.treas as Integer
    def medalPrice = _.params.medalPrice as Integer
    def cashedMedals = _.params.cashedMedals as Integer

    def dirty = false;

    if (name != null) {
        clanTO.name = name; dirty = true
    }
    if (description != null) {
        clanTO.description = description; dirty = true
    }
    if (level != null) {
        clanTO.level = level; dirty = true
    }
    if (joinRating != null) {
        clanTO.joinRating = joinRating; dirty = true
    }
    if (reviewState != null && reviewState) {
        clanTO.reviewState = ReviewState.getByCode(reviewState.code); dirty = true
    }
    if (level != null) {
        clanTO.level = level; dirty = true
    }

    if (closed != null) {
        clanTO.closed = closed; dirty = true
    }
    if (treas != null) {
        clanTO.treas = treas; dirty = true
    }
    if (medalPrice != null) {
        clanTO.medalPrice = medalPrice; dirty = true
    }
    if (cashedMedals != null) {
        clanTO.cashedMedals = cashedMedals; dirty = true
    }

    if (dirty) {
        def resp = _.clanService.updateClan(clanTO)
        if (resp.ok) {
            _.dao.logClanAction(clan.id, ClanActionEnum.ADMIN_UPDATE.type, _.adminUserId, 0, 0, clan.treas);
            "clan [${clanId}] successfully updated"
        } else {
            throw new ExecAdminScriptException(ServiceResult.ERR_INVALID_ARGUMENT, "[${clanId}] ${resp.logMessage}")
        }
    } else {
        "clan [${clanId}] not modified"
    }
}

def lockClan(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ClanServiceImpl clanService = context.getBean(ClanServiceImpl.class)

    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> params = serializer.fromString(request.scriptParams, Map.class)

    int clanId = params.clanId as Integer
    Clan clan = clanService.getClan(clanId)
    if (clan == null) {
        throw new ExecAdminScriptException(ServiceResult.ERR_CLAN_NOT_FOUND, "Clan not found by id ${clanId}")
    }

    if (clan.reviewState == ReviewState.LOCKED) {
        return "clan [${clanId}] alredy locked"
    }

    ChangeClanReviewStateResponse resp = clanService.changeClanReviewState(new ChangeClanReviewStateRequest(clanId, ReviewState.LOCKED))
    if (resp.ok) {
        "clan [${clanId}] successfully locked"
    } else {
        throw new ExecAdminScriptException(ServiceResult.ERR_INVALID_ARGUMENT, "[${clanId}] ${resp.logMessage}")
    }
}

def approveAll(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    ClanServiceImpl clanService = context.getBean(ClanServiceImpl.class)

    InteropSerializer serializer = new InteropSerializer()
    Map<String, Object> params = serializer.fromString(request.scriptParams, Map.class)

    String[] clanIds = (params.ids as String).split(",")

    String result = ""
    for (String clanId_s : clanIds) {
        if (clanId_s.isEmpty()) {
            continue
        }
        int clanId = clanId_s as Integer
        Clan clan = clanService.getClan(clanId)
        if (clan == null) {
            result += clanId + ":Absent, "
            continue
        }
        if (clan.reviewState == ReviewState.APPROVED) {
            result += clanId + ":AlreadyApproved, "
            continue
        }
        if (clan.reviewState == ReviewState.LOCKED) {
            result += clanId + ":StillLocked, "
            continue
        }

        clanService.changeClanReviewState(new ChangeClanReviewStateRequest(clanId, ReviewState.APPROVED))
        result += clanId + ":SuccessApproved, "
    }
    result
}

def add(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def _ = new RequestContext(context, request)

    def clanId = _.params.clanId as int
    def profileId = _.params.profileId as int

    def clan = _.clanRepo.getClan(clanId)
    def profile = _.profileService.getUserProfile(profileId)

    def user = _.clanService.getClanMember(clan.leader.socialId, profileId)
    if (user == null) {
        user = _.clanService.createClanMember(clan.leader.socialId, profileId, profile.getProfileStringId() ?: "" + profileId, "" + profileId)
        user.rating = profile.rating
    }

    if (user.clan != null) throw new IllegalStateException("${profileId} уже в клане '${user.clan.name}'")
    if (clan.size >= clan.capacity()) throw new IllegalStateException("Достигнуто предельное количество ${clan.capacity()} членов клана")

    _.inviteRepo.removeInvites(user.getId())

    user.clan = clan
    user.rank = com.pragmatix.clanserver.domain.Rank.SOLDIER
    user.joinDate = new Date()

    _.dao.createClanMember(user)
    clan.accept(user)
    _.dao.updateClanAggregates(clan)
    _.clanRepo.putMember(user)

    _.chatService.broadcastClanAction(clan, user, null, new LoginJoinRequest(), null)
    _.auditService.logClanAction(clan, ClanActionEnum.ADMIN_ADD, _.adminUserId, user, user.seasonRating)
    _.interopService.refreshClan(user)

    "user [${profileId}] successfully added to clan"
}

def expelPermit(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def _ = new RequestContext(context, request)

    int clanId = _.params.clanId as Integer
    int memberId = _.params.memberId as Integer
    short socialId = _.params.socialId as short
    boolean value = "on" == _.params.value as String

    Clan clan = _.clanRepo.getClan(clanId)

    def member = new VarObject<ClanMember>()
    def result = new VarObject<com.pragmatix.clanserver.messages.ServiceResult>(com.pragmatix.clanserver.messages.ServiceResult.OK)
    def task = new ClanTask(clan) {
        @Override
        protected void exec() {
            member.value = clan.getMember(socialId, memberId)
            if (member.value == null) {
                result.value = com.pragmatix.clanserver.messages.ServiceResult.ERR_NOT_FOUND
            } else if (member.value.rank != Rank.OFFICER) {
                result.value = com.pragmatix.clanserver.messages.ServiceResult.ERR_INVALID_ARGUMENT
            } else {
                member.value.expelPermit = value
                member.value.setDirty(true)
                _.clanService.getDao().updateClanMember(member.value)
            }
        }
    }

    _.clanService.concurrentService.execWrite(task, null);

    if (result.value.ok) {
        "member [${memberId}] expelPermit => ${value}"
    } else {
        throw new ExecAdminScriptException(ServiceResult.ERR_INVALID_ARGUMENT, "[${memberId}] ${result.value}")
    }
}

def expel(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def _ = new RequestContext(context, request)

    int clanId = _.params.clanId as Integer
    int memberId = _.params.memberId as Integer

    Clan clan = _.clanRepo.getClan(clanId)

    def resp = _.clanService.expelFromClan(new ExpelFromClanRequest(clan.leader.socialId, memberId, _.adminUserId), clan.leader)

    if (resp.ok) {
        "member [${memberId}] successfully expeled from clan"
    } else {
        throw new ExecAdminScriptException(ServiceResult.ERR_INVALID_ARGUMENT, "[${memberId}] ${resp.logMessage}")
    }
}

def setRank(ApplicationContext context, ExecScriptRequest request, PrintWriter console) {
    def _ = new RequestContext(context, request)

    def clanId = _.params.clanId as Integer
    def memberId = _.params.memberId as Integer
    def targetRank = Rank.valueOf(_.params.rank as int)

    def clan = _.clanRepo.getClan(clanId)
    def member = clan.getMember(clan.leader.socialId, memberId)

    def promote = targetRank.code < member.rank.code
    def resp
    if (promote) {
        if (targetRank == Rank.LEADER && member.rank == Rank.OFFICER) {
            member.muteMode = false
            member.expelPermit = true
        }
        resp = _.clanService.promoteInRank(new PromoteInRankRequest(clan.leader.socialId, memberId, targetRank, _.adminUserId), clan.leader)
    } else {
        resp = _.clanService.lowerInRank(new LowerInRankRequest(clan.leader.socialId, memberId, targetRank, _.adminUserId), clan.leader)
    }
    if (resp.ok) {
        "member [${memberId}] successfully ${promote ? 'promoted' : 'lowered'}  in clan"
    } else {
        throw new ExecAdminScriptException(ServiceResult.ERR_INVALID_ARGUMENT, "[${memberId}] ${resp.logMessage}")
    }
}

class RequestContext {
    Map<String, Object> params
    String adminUser
    Integer adminUserId

    ClanServiceImpl clanService
    ClanRepoImpl clanRepo
    ProfileService profileService
    AuditService auditService
    ChatServiceImpl chatService
    InviteRepoImpl inviteRepo
    RatingServiceImpl ratingService
    ClanInteropServiceImpl interopService
    JdbcTemplate jdbcTemplate

    RequestContext(ApplicationContext context, ExecScriptRequest request) {
        this.params = new InteropSerializer().fromString(request.scriptParams, Map.class)
        adminUser = request.adminUser
        adminUserId = params.adminUserId as Integer

        clanService = context.getBean(ClanServiceImpl.class)
        clanRepo = context.getBean(ClanRepoImpl.class)
        profileService = context.getBean(ProfileService.class)
        auditService = context.getBean(AuditService.class)
        chatService = context.getBean(ChatServiceImpl.class)
        inviteRepo = context.getBean(InviteRepoImpl.class)
        ratingService = context.getBean(RatingServiceImpl.class)
        interopService = context.getBean(ClanInteropServiceImpl.class)
        jdbcTemplate = context.getBean(JdbcTemplate.class)
    }

    DAO getDao() {
        clanService.getDao()
    }
}

def Map<String, Object> newClanStructure(ClanTO clanTO) {
    Map<String, Object> clan = new LinkedHashMap<>()
    clan.id = clanTO.id
    clan.name = clanTO.name
    clan.description = clanTO.description
    clan.level = clanTO.level
    clan.size = clanTO.size
    clan.createDate = new Date((long) clanTO.createDate * 1000l)
//    clan.rating = clanTO.rating
    clan.seasonRating = clanTO.seasonRating
    clan.reviewState = com.pragmatix.wormix.webadmin.interop.response.structure.clan.ReviewState.valueOf(clanTO.reviewState.code)
    clan.joinRating = clanTO.joinRating

    clan.closed = clanTO.closed
    clan.treas = clanTO.treas
    clan.medalPrice = clanTO.medalPrice
    clan.cashedMedals = clanTO.cashedMedals

    clan
}

def Map<String, Object> newClanStructure(Clan clanModel) {
    Map<String, Object> clan = new LinkedHashMap<>()
    clan.id = clanModel.id
    clan.name = clanModel.name
    clan.description = clanModel.description
    clan.level = clanModel.level
    clan.size = clanModel.size
    clan.createDate = clanModel.createDate
    clan.rating = clanModel.rating
    clan.seasonRating = clanModel.seasonRating
    clan.reviewState = com.pragmatix.wormix.webadmin.interop.response.structure.clan.ReviewState.valueOf(clanModel.reviewState.code)
    clan.members = new LinkedHashMap<String, Object>[clanModel.size]
    clan.joinRating = clanModel.joinRating
    clan.news = clanModel.newsBoard.inject("") { acc, news -> acc + news.text }

    clan.closed = clanModel.closed
    clan.treas = clanModel.treas
    clan.medalPrice = clanModel.medalPrice
    clan.cashedMedals = clanModel.cashedMedals

    clan
}

def Map<String, Object> newMember(ClanMember clanMember, boolean banned) {
    Map<String, Object> member = new LinkedHashMap<>()
    member.socialId = clanMember.socialId
    member.profileId = clanMember.profileId
    member.socialProfileId = clanMember.socialProfileId
    member.name = clanMember.name
    member.online = clanMember.online
    member.rating = clanMember.rating
    member.seasonRating = clanMember.seasonRating
    member.rank = com.pragmatix.wormix.webadmin.interop.response.structure.clan.Rank.valueOf(clanMember.rank.name())
    member.joinDate = clanMember.joinDate
    member.banned = banned

    member.lastLoginTime = new Date(clanMember.lastLoginTime * 1000L)
    member.donation = clanMember.donation
    member.donationCurrSeason = clanMember.donationCurrSeason
    member.donationPrevSeason = clanMember.donationPrevSeason
    member.donationCurrSeasonComeback = clanMember.donationCurrSeasonComeback
    member.donationPrevSeasonComeback = clanMember.donationPrevSeasonComeback
    member.cashedMedals = clanMember.cashedMedals
    member.expelPermit = clanMember.expelPermit
    member.muteMode = clanMember.muteMode

    member;
}