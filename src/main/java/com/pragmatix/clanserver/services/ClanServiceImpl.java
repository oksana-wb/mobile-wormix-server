package com.pragmatix.clanserver.services;

import com.pragmatix.clan.ClanInteropServiceImpl;
import com.pragmatix.clanserver.common.ClanActionEnum;
import com.pragmatix.clanserver.dao.DAO;
import com.pragmatix.clanserver.dao.jdbc.JdbcDAO;
import com.pragmatix.clanserver.domain.*;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.ServiceResult;
import com.pragmatix.clanserver.messages.request.*;
import com.pragmatix.clanserver.messages.response.*;
import com.pragmatix.clanserver.messages.structures.*;
import com.pragmatix.common.utils.VarInt;
import com.pragmatix.common.utils.VarObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Author: Vladimir
 * Date: 05.04.13 8:47
 */
@Service
public class ClanServiceImpl implements ClanService {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ClanInteropServiceImpl interopService;

    @Autowired
    ConcurrentService concurrentService;

    @Autowired
    ClanSeasonService seasonService;

    @Autowired
    RatingServiceImpl ratingService;

    @Autowired
    ClanSeasonService clanSeasonService;

    @Autowired
    DAO dao;

    @Autowired
    ClanRepoImpl clanRepo;

    @Autowired
    PriceServiceImpl priceService;

    @Autowired
    ChatServiceImpl chatService;

    @Autowired
    InviteRepoImpl inviteRepo;

    @Autowired
    NameNormalizer nameNormalizer;

    @Resource
    private ClanDailyRegistry clanDailyRegistry;

    @Resource
    private AuditService auditService;

    @Value("${clan.invite.maxLifeTime:86400}")
    int inviteMaxLifeTime;

    @Value("${clan.member.logoutSaveInterval:300}")
    int logoutSaveInterval;

    @Value("${clan.search.phrase.minLength:3}")
    int searchPhraseMinLength;

    @Value("${clan.search.pageSize:20}")
    int searchPageSize;

    @Value("${clan.top.size:100}")
    int topSize = 100;

    @Value("${server.mode:0}")
    int serverMode;

    @Value("${clan.minMedalPrice:1}")
    int minMedalPrice = 1;

    @Value("${clan.maxMedalPrice:10}")
    int maxMedalPrice = 10;

    @Value("${clan.minMedalAmount:20}")
    int minMedalAmount = 20;

    @Value("${clan.debug.donateDailyCheck:true}")
    boolean debugDonateDailyCheck = true;

    @Value("${clan.debug.changeMedalPriceCheck:true}")
    boolean debugChangeMedalPriceCheck = true;

    /**
     * запретить модификацию состава клана
     * иcползуется перед закрытием сезона
     */
    private boolean freezeClanSizeModifications;

    private int RatingPerMedal = 1000;

    private int MedalsToQuitFromClosedClan = 20;

    @Override
    public CommonResponse<LoginBase> onLogin(LoginBase login, final ClanMember user) {
        // говорим, что мы в онлайне
        user.setOnline(true);
        user.lastLoginTime = (int) (System.currentTimeMillis() / 1000L);

        if(logger.isDebugEnabled() || (login.getCommandId() != Messages.LOGIN_REQUEST && logger.isInfoEnabled())) {
            logger.info(login.toString());
        }

        int commandId = login.getCommandId();
        final CommonResponse<LoginBase> response = new LoginErrorResponse(login);
        Clan clan = null;

        if(commandId == Messages.LOGIN_CREATE_REQUEST) {
            LoginCreateRequest request = (LoginCreateRequest) login;
            request.clanName = nameNormalizer.trim(request.clanName);
            if(user.clan != null) {
                response.serviceResult = ServiceResult.ERR_ALREADY_IN_CLAN;
                response.logMessage = "Пользователь " + user + " уже является членом клана " + user.getClanId();
            } else if(!nameNormalizer.isValidName(request.clanName)) {
                response.serviceResult = ServiceResult.ERR_INVALID_ARGUMENT;
                response.logMessage = "Имя клана не соответствует требованиям";
            } else if(getDao().clanExists(request.clanName, 0)) {
                response.serviceResult = ServiceResult.ERR_CLAN_NAME_EXISTS;
                response.logMessage = "Клан с похожим именем уже существует";
            } else if(interopOk(interopService.beforeCreateClan(user, response), response)) {
                Price price = priceService.createClanPrice(user.socialId);
                VarInt reservation = new VarInt();

                if(price.amount > 0 && !interopService.reserveFunds(user.socialId, user.profileId, price, reservation)) {
                    response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_MONEY;
                    response.logMessage = "Недостаточно средств на счете";
                } else {
                    inviteRepo.removeInvites(user.getId());

                    clan = new Clan();
                    clan.createDate = new Date();
                    clan.name = request.clanName;
                    clan.level = ClanLevel.FIRST;
                    clan.emblem = request.clanEmblem;
                    clan.description = request.clanDescription;
                    clan.size = 1;
                    clan.rating = user.rating;

                    // делаем копию объекта пользователя на случай ошибки
                    ClanMember _user = user.clone();

                    clan = getDao().createClan(clan, _user);

                    user.clan = _user.clan;
                    user.rank = _user.rank;
                    user.joinDate = _user.joinDate;
                    user.setNew(false);

                    clan.accept(user);

                    clanRepo.putClan(clan);
                    clanRepo.putMember(user);

                    if(price.amount > 0 && !interopService.withdrawFunds(user.socialId, user.profileId, price, reservation.value, clan.id)) {
                        logger.warn("Не удалось получить оплату за создание клана с пользователя " + user);
                    }

                    auditService.logClanAction(clan, ClanActionEnum.CREATE, user.profileId, 0);
                }
            }
        } else if(commandId == Messages.LOGIN_JOIN_REQUEST) {
            if(user.clan == null) {
                final LoginJoinRequest request = (LoginJoinRequest) login;

                clan = clanRepo.getClan(request.clanId);
                final VarObject<ClanMember> host = new VarObject<>();
                if(clan == null) {
                    response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                    response.logMessage = "Клан не нейден";
                } else if(clan.reviewState == ReviewState.LOCKED) {
                    response.serviceResult = ServiceResult.ERR_CLAN_LOCKED;
                    response.logMessage = "Клан заблокирован модератором";
                } else if(interopOk(interopService.beforeJoinClan(user, response), response)) {
                    ClanTask task = new ClanTask(clan) {
                        @Override
                        protected void exec() {
                            if(request.hostProfileId > 0) {
                                host.value = clan.getMember(request.hostSocialId, request.hostProfileId);

                                if(host.value == null) {
                                    response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                                    response.logMessage = "Не найден автор приглашения " + request.hostSocialId + " " + request.hostProfileId;
                                } else if(host.value.getClanId() == null || host.value.getClanId() != request.clanId) {
                                    response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                                    response.logMessage = "Автор приглашения не принадлежит клану " + request.clanId;
                                } else if(!host.value.rank.canInvite()) {
                                    response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                                    response.logMessage = "Пользователь " + user + " не в праве приглашать в клан";
                                } else {
                                    host.value.trimInvites(inviteMaxLifeTime);

                                    Invite invite = host.value.removeInvite(user.socialId, user.profileId);

                                    if(invite == null) {
                                        response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                                        response.logMessage = "Приглашение не найдено для " + user.socialId + " " + user.profileId;
                                    }
                                }
                            } else if(clan.joinRating < 0 || clan.joinRating > user.rating) {
                                response.serviceResult = ServiceResult.ERR_REQUIREMENTS_FAILURE;
                                response.logMessage = "Вступительный рейтинг " + clan.joinRating + ", рейтинг игрока " + user.rating;
                            }

                            if(response.isOk()) {
                                inviteRepo.removeInvites(user.getId());

                                if(clan.size < clan.capacity()) {
                                    user.clan = clan;
                                    user.rank = Rank.SOLDIER;
                                    user.joinDate = new Date();
                                    user.hostProfileId = request.hostProfileId;

                                    getDao().createClanMember(user);

                                    clan.accept(user);

                                    getDao().updateClanAggregates(clan);

                                    clanRepo.putMember(user);
                                } else {
                                    response.serviceResult = ServiceResult.ERR_CLAN_SIZE_LIMIT;
                                    response.logMessage = "Достигнуто предельное количество " + clan.capacity() + " членов клана " + clan.id + ":" + clan.name;
                                }
                            }
                        }
                    };
                    concurrentService.execWrite(task, response);

                    if(response.isOk()) {
                        chatService.broadcastClanAction(clan, user, null, request, response);
                        auditService.logClanAction(clan, ClanActionEnum.JOIN, user.profileId, host.value, user.seasonRating);
                    }
                }
            } else {
                clan = user.clan;

                refreshAggregates(clan, response);

                chatService.broadcastClanAction(clan, user, null, login, response);
            }
        } else {
            clan = user.clan;

            refreshAggregates(clan, response);

            chatService.broadcastClanAction(clan, user, null, login, response);
        }

        if(response.isOk()) {
            final EnterAccount enterAccount = new EnterAccount(login);

            ClanTask task = new ClanTask(clan) {
                @Override
                protected void exec() {
                    enterAccount.clan = new ClanTO(clan, ratingService);
                    int i;

                    int chatSize = clan.chat.size();
                    if(chatSize > 0) {
                        enterAccount.chat = new ChatMessageTO[clan.chat.size()];
                        i = 0;
                        for(ChatMessage message : clan.chat) {
                            enterAccount.chat[i++] = new ChatMessageTO(message);
                        }
                    }

                    int newsSize = clan.newsBoard.size();
                    if(newsSize > 0) {
                        enterAccount.newsBoard = new NewsTO[newsSize];
                        i = 0;
                        for(News news : clan.newsBoard) {
                            enterAccount.newsBoard[i++] = new NewsTO(news);
                        }
                    }

                    Season currentSeason = seasonService.getCurrentSeason();
                    enterAccount.startSeasonDate = currentSeason.start;
                    enterAccount.finishSeasonDate = currentSeason.finish;
                    ratingService.updateRatings(clan);
                }
            };
            concurrentService.execRead(task, response);

            if(response.isOk()) {
                interopService.refreshClan(user);

                if(login.getCommandId() != Messages.LOGIN_REQUEST && logger.isInfoEnabled()) {
                    logger.info(enterAccount.toString());
                }

                return enterAccount;
            }
        }

        // даже если возникла ошибка, синхронизируем клановую информацию с main сервером
        interopService.refreshClan(user);

        if(logger.isDebugEnabled() || (login.getCommandId() != Messages.LOGIN_REQUEST && logger.isInfoEnabled())) {
            logger.info(response.toString());
        }

        return response;
    }

    @Override
    public void onLogout(ClanMember clanMember, boolean broadcast) {
        // если игрок покинул клан, он нас больше не интересует
        if(clanMember == null || clanMember.clan == null) {
            return;
        }
        //todo прокоментировать дальнейшие действия. Есть подозрение, что происходит не совсем то, что должно
        if(!clanMember.isNew()) {
            long interval = System.currentTimeMillis();

            if(clanMember.logoutDate != null) {
                interval -= clanMember.logoutDate.getTime();
            }

            if(clanMember.isDirty() || interval >= 1000L * logoutSaveInterval) {
                clanMember.logoutDate = new Date();

                getDao().updateClanMember(clanMember);
            }

            ClanTask task = new ClanTask(clanMember.clan) {
                @Override
                protected void exec() {
                    if(clan.isDirty()) {
                        getDao().updateClanAggregates(clan);
                    }
                }
            };
            concurrentService.execWrite(task, null);

            if(broadcast) {
                chatService.broadcastClanAction(clanMember.clan, clanMember, null, Messages.LOGOUT_REQUEST, "");
            }

        }
    }

    @Override
    public ClanSummaryResponse getClanSummary(final ClanSummaryRequest request, ClanMember user) {

        if(logger.isDebugEnabled()) {
            logger.debug(request.toString());
        }

        final ClanSummaryResponse response = new ClanSummaryResponse(request);

        Clan clan = clanRepo.getClan(request.clanId);

        if(clan == null) {
            response.serviceResult = ServiceResult.ERR_NOT_FOUND;
            response.logMessage = "Клан " + request.clanId + " не найден";
            response.clan = ClanTO.NULL_CLAN;
        } else {
            ClanTask task = new ClanTask(clan) {
                @Override
                protected void exec() {
                    response.clan = new ClanTO(clan, request.scope, ratingService);
                }
            };
            concurrentService.execRead(task, response);
        }

        if(logger.isDebugEnabled()) {
            logger.debug(response.toString());
        }

        return response;
    }

    @Override
    public ListClansResponse listClans(ListClansRequest request) {

        if(logger.isDebugEnabled()) {
            logger.debug(request.toString());
        }

        final ListClansResponse response = new ListClansResponse(request);

        String searchPhrase = request.searchPhrase.trim();

        if(searchPhrase.length() < searchPhraseMinLength) {
            response.serviceResult = ServiceResult.ERR_INVALID_ARGUMENT;
            response.logMessage = "Недостаточная длина строки поиска " + searchPhrase.length();
        } else {
            List<Clan> clans = getDao().listClansByName(searchPhrase, searchPageSize);
            response.clans = new ClanTO[clans.size()];
            int i = 0;

            for(Clan clan : clans) {
                response.clans[i++] = new ClanTO(clan, ClanTO.SCOPE_HEADER, ratingService);
            }
        }

        if(logger.isDebugEnabled()) {
            logger.debug(response.toString());
        }

        return response;
    }

    @Override
    public ListClansResponse listClansOrderByName(ListClansRequest request) {

        if(logger.isDebugEnabled()) {
            logger.debug(request.toString());
        }

        final ListClansResponse response = new ListClansResponse(request);

        List<Clan> clans = getDao().listClansOrderByName(request.searchPhrase, request.reviewStates, request.offset, request.limit);
        response.clans = new ClanTO[clans.size()];
        int i = 0;

        for(Clan clan : clans) {
            response.clans[i++] = new ClanTO(clan, ClanTO.SCOPE_HEADER, ratingService);
        }

        if(logger.isDebugEnabled()) {
            logger.debug(response.toString());
        }

        return response;
    }

    @Override
    public TopClansResponse topClans(TopClansRequest request, final ClanMember user) {

        if(logger.isDebugEnabled()) {
            logger.debug(request.toString());
        }

        final TopClansResponse response = new TopClansResponse(request);

        RatingItem[] topItems = ratingService.topN(topSize, request.season);
        List<ClanTO> top = new ArrayList<>();
        List<Integer> oldPlaces = new ArrayList<>();

        for(RatingItem item : topItems) {
            Clan clan = getClan(item.clanId);
            if(clan != null) {
                top.add(new ClanTO(clan, ClanTO.SCOPE_HEADER, ratingService));
                if(request.season) {
                    oldPlaces.add(item.oldPlace);
                }
            }
        }

        response.clans = top.toArray(new ClanTO[top.size()]);
        response.oldPlaces = oldPlaces.toArray(new Integer[oldPlaces.size()]);
        if(user != null && user.clan != null) {
            Integer clanId = user.clan.id;
            response.position = Math.max(0, ratingService.position(clanId, request.season) + 1);
            response.oldPlace = request.season ? ratingService.getOldPlace(clanId) : 0;
            response.rating = ratingService.getSeasonRating(clanId);
        }

        Season currentSeason = seasonService.getCurrentSeason();
        response.startSeasonDate = currentSeason.start;
        response.finishSeasonDate = currentSeason.finish;
        response.seasonId = currentSeason.id;

        if(logger.isDebugEnabled()) {
            logger.debug(response.toString());
        }

        return response;
    }

    @Override
    public DeleteClanResponse deleteClan(final DeleteClanRequest request, final ClanMember user) {

        if(logger.isInfoEnabled()) {
            logger.info(request.toString());
        }

        DeleteClanResponse response = new DeleteClanResponse(request);

        if(user.rank != Rank.LEADER) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Пользователь " + user + " не в праве удалить клан";
        } else if(user.clan.size > 1) {
            response.serviceResult = ServiceResult.ERR_REQUIREMENTS_FAILURE;
            response.logMessage = "В клане " + user.clan.size + " участников";
        } else {
            Clan clan = user.clan;

            ClanTask task = new ClanTask(clan) {
                @Override
                protected void exec() {
                    clanRepo.removeClan(clan);

                    ratingService.removeRating(clan.id);
                }
            };
            concurrentService.execWrite(task, response);

            if(response.isOk()) {
                response.comeback = interopService.deleteClan(clan, user);
            }
        }

        if(logger.isInfoEnabled()) {
            logger.info(response.toString());
        }

        return response;
    }

    @Override
    public InviteToClanResponse inviteToClan(final InviteToClanRequest request, final ClanMember user) {

        if(logger.isInfoEnabled()) {
            logger.info(request.toString());
        }

        final InviteToClanResponse response = new InviteToClanResponse(request);

        if(!user.rank.canInvite()) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Пользователь " + user + " не в праве приглашать в клан";
        } else if(user.clan.reviewState == ReviewState.LOCKED) {
            response.serviceResult = ServiceResult.ERR_CLAN_LOCKED;
            response.logMessage = "Клан заблокирован модератором";
        } else {
            ClanTask task = new ClanTask(user.clan) {
                @Override
                protected void exec() {
                    if(user.trimInvites(inviteMaxLifeTime) >= user.rank.inviteLimit) {
                        response.serviceResult = ServiceResult.ERR_MEMBER_INVITE_LIMIT;
                        response.logMessage = "Пользователь " + user + " исчерпал лимит приглашений " + user.rank.inviteLimit;
                    } else if(clan.size >= clan.capacity()) {
                        response.serviceResult = ServiceResult.ERR_CLAN_SIZE_LIMIT;
                        response.logMessage = "Достигнуто предельное количество " + clan.capacity() + " членов клана " + clan.id + ":" + clan.name;
                    } else {
                        ClanMember member = clan.getMember(request.socialId, request.profileId);

                        if(member != null && !member.isNew()) {
                            response.logMessage = "Пользователь " + member + " уже является членом клана " + member.getClanId();
                            response.serviceResult = ServiceResult.ERR_ALREADY_IN_CLAN;
                        } else if(interopOk(interopService.beforeInviteToClan(user, request.socialId, request.profileId, response), response)) {
                            user.trimInvites(inviteMaxLifeTime);

                            for(Invite invite : user.invites) {
                                if(invite.socialId == request.socialId && invite.profileId == request.profileId) {
                                    response.serviceResult = ServiceResult.ERR_REPEATED_INVITE;
                                    response.logMessage = "Повторное приглашение [" + request.socialId + " " +
                                            request.profileId + "] от [" + user.socialId + " " + user.profileId + "]";
                                    break;
                                }
                            }

                            if(response.isOk()) {
                                ServiceResult repoRes = inviteRepo.addInvite(ClanMember.getId(request.socialId, request.profileId), clan.id, user.socialId, user.profileId);

                                if(!repoRes.isOk()) {
                                    response.serviceResult = repoRes;
                                    response.logMessage = "Ошибка добавления в базу инвайтов";
                                } else if(interopOk(interopService.sendInvite(request.socialId, request.profileId, user, clan, response), response)) {
                                    user.addInvite(new Invite(clan.id, request.socialId, request.profileId));
                                }
                            }
                        }
                    }
                    response.invites = InviteTO.convert(user.invites);
                }
            };
            concurrentService.execWrite(task, response);
        }

        if(logger.isInfoEnabled()) {
            logger.info(response.toString());
        }

        return response;
    }

    @Override
    public ExpelFromClanResponse expelFromClan(final ExpelFromClanRequest request, final ClanMember user) {
        if(logger.isInfoEnabled()) {
            logger.info(request.toString());
        }

        final ExpelFromClanResponse response = new ExpelFromClanResponse(request);

        if(freezeClanSizeModifications && request.adminUserId == 0) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Модификация размера клана в данный момент запрещена";
        } else if(clanDailyRegistry.getExpelCount(user.getClanMemberId()) >= user.rank.getMaxExpelByDay() && request.adminUserId == 0) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Превышен ежедневный лимит на удаление из клана";
        } else {
            Clan clan = user.clan;
            final VarObject<ClanMember> member = new VarObject<>();
            final VarInt clanTreasLoss = new VarInt();
            final VarInt compensationInRuby = new VarInt();

            ClanTask task = new ClanTask(clan) {
                @Override
                protected void exec() {
                    member.value = clan.getMember(request.socialId, request.profileId);
                    if(member.value == null) {
                        response.serviceResult = ServiceResult.ERR_NOT_FOUND;
                        response.logMessage = "Не в клане";
                    } else {
                        int rubyByRating = Math.max(0, member.value.seasonRating - member.value.cashedMedals * RatingPerMedal) / RatingPerMedal * clan.medalPrice;
                        clanTreasLoss.value = member.value.donationCurrSeason + member.value.donationPrevSeason + rubyByRating;
                        compensationInRuby.value = member.value.donationCurrSeasonComeback + member.value.donationPrevSeasonComeback + rubyByRating;

                        if(!user.rank.canExpel() || !user.expelPermit || (clanTreasLoss.value > 0 && user.rank != Rank.LEADER)) {
                            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                            response.logMessage = "Не разрешено";
                        } else if(!user.getClanId().equals(member.value.getClanId())) {
                            response.serviceResult = ServiceResult.ERR_NOT_IN_CLAN;
                            response.logMessage = "Нельзя удалить члена другого клана";
                        } else if(!user.rank.isHigherThan(member.value.rank)) {
                            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                            response.logMessage = "Нельзя удалить старшего или равного члена клана";
                        } else if(clanTreasLoss.value > clan.treas) {
                            response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_TREAS;
                            response.logMessage = String.format("В казне не достаточно рубинов для отступных игроку! profileId=%s, medalPrice=%s, treas=%s, compensation=%s", user.profileId, clan.medalPrice, clan.treas, compensationInRuby);
                        } else {
                            expel(member.value, false);

                            clan.treas -= clanTreasLoss.value;
                            // Если лидер выгоняет игрока с медалями и оплачивает их, рейтинг за эти медали должен оставаться в клане
                            if(rubyByRating > 0)
                                clan.cashedMedals += Math.max(0, member.value.seasonRating - member.value.cashedMedals * RatingPerMedal) / RatingPerMedal;
                            clan.setDirty(true);

                            clanDailyRegistry.incExpelCount(user.getClanMemberId());
                        }
                    }
                }
            };
            concurrentService.execWrite(task, response);

            if(response.isOk()) {
                interopService.expelFromClan(clan, member.value, compensationInRuby.value);
                chatService.broadcastClanAction(clan, user, member.value, request.getCommandId(), "" + compensationInRuby, member.value);
                auditService.logClanAction(clan,
                        request.adminUserId > 0 ? ClanActionEnum.ADMIN_EXPELL : ClanActionEnum.EXPELL,
                        request.adminUserId > 0 ? request.adminUserId : user.profileId,
                        member.value, member.value.donationCurrSeason + member.value.donationPrevSeason);
            }
        }

        if(logger.isInfoEnabled()) {
            logger.info(response.toString());
        }

        return response;
    }

    @Override
    public QuitClanResponse quitClan(QuitClanRequest request, final ClanMember user) {

        if(logger.isInfoEnabled()) {
            logger.info(request.toString());
        }

        QuitClanResponse response = new QuitClanResponse(request);

        if(freezeClanSizeModifications) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Модификация размера клана в данный момент запрещена";
        } else if(user.rank == Rank.LEADER) {
            response.serviceResult = ServiceResult.ERR_INVALID_ARGUMENT;
            response.logMessage = "Нельзя удалить лидера клана";
        } else if(user.clan == null) {
            response.serviceResult = ServiceResult.ERR_NOT_IN_CLAN;
            response.logMessage = "Не в клане";
        } else {
            Clan clan = user.clan;

            // заработанные но не обналиченные медали
            int medals = Math.max(0, user.seasonRating - user.cashedMedals * RatingPerMedal) / RatingPerMedal;
            if(clan.closed && !clanSeasonService.canQuitClan()
                    //Игрок должен иметь возможность выйти из закрытого клана, если у него есть 20 и более медалей
                    && medals < MedalsToQuitFromClosedClan) {
                response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                response.logMessage = "Выход из закрытого клана клана в данный момент запрещен. Медалей: " + medals;
            } else {
                ClanTask task = new ClanTask(clan) {
                    @Override
                    protected void exec() {
                        expel(user, true);
                    }
                };
                concurrentService.execWrite(task, response);

                if(response.isOk()) {
                    interopService.refreshClan(user);
                    chatService.broadcastClanAction(clan, user, null, request, response);
                    // клиент сам разорвет коннект
                    // Connections.closeConnectionDeferred(user, Consts.MAIN_CONNECTION);
                    auditService.logClanAction(clan, ClanActionEnum.QUIT, user.profileId, user.seasonRating);
                }
            }
        }

        if(logger.isInfoEnabled()) {
            logger.info(response.toString());
        }

        return response;
    }

    public void expel(final ClanMember member, final boolean backupMember) {
        final Clan clan = member.clan;

        getDao().runInTransaction(new Runnable() {
            @Override
            public void run() {
                // чтобы сбросить накопленный рейтинг в базу
                getDao().updateClanMember(member);

                clan.expel(member);

                clanRepo.onMemberRemove(member);

                getDao().deleteClanMember(member.socialId, member.profileId);

                getDao().updateClanAggregates(clan);

                if(backupMember)
                    getDao().backupMember(clan.id, member);
            }
        });

        ratingService.updateRatings(clan);
    }

    public DAO getDao() {
        if(clanSeasonService.isDiscardDAO()) {
            throw new IllegalStateException("сезон в процессе закрытия!");
        } else {
            return dao;
        }
    }

    @Override
    public PromoteInRankResponse promoteInRank(final PromoteInRankRequest request, final ClanMember user) {

        if(logger.isInfoEnabled()) {
            logger.info(request.toString());
        }

        final PromoteInRankResponse response = new PromoteInRankResponse(request);
        response.rank = Rank.SOLDIER;

        if(user.isSelf(request.socialId, request.profileId)) {
            response.serviceResult = ServiceResult.ERR_INVALID_ARGUMENT;
            response.logMessage = "Нельзя повысить в звании самого себя";
            response.rank = user.rank;
        } else {
            Clan clan = user.clan;
            final VarObject<ClanMember> member = new VarObject<>();

            ClanTask task = new ClanTask(user.clan) {
                @Override
                protected void exec() {
                    member.value = clan.getMember(request.socialId, request.profileId);

                    if(member.value == null) {
                        response.serviceResult = ServiceResult.ERR_NOT_FOUND;
                        response.logMessage = "Не в клане";
                    } else if(!user.getClanId().equals(member.value.getClanId())) {
                        response.serviceResult = ServiceResult.ERR_NOT_IN_CLAN;
                        response.logMessage = "Нельзя повысить в звании члена другого клана";
                    } else if(!user.rank.canPromoteInRank()) {
                        response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                        response.logMessage = "Не разрешено";
                    } else if(!user.rank.isHigherThan(member.value.rank)) {
                        response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                        response.logMessage = "Нельзя повысить старшего или равного по званию";
                    } else {
                        Clan clan = member.value.clan;

                        Rank memberRank = member.value.rank;
                        Rank upperRank = memberRank.upper();

                        response.rank = memberRank;

                        if(upperRank != request.targetRank || !upperRank.isHigherThan(memberRank)) {
                            response.serviceResult = ServiceResult.ERR_INVALID_ARGUMENT;
                            response.logMessage = "Нельзя повысить " + memberRank + " до " + request.targetRank;
                        } else if(upperRank == Rank.LEADER) {
                            if(member.value.muteMode || !member.value.expelPermit) {
                                response.serviceResult = ServiceResult.ERR_INVALID_STATE;
                                response.logMessage = "На офицера наложены ограничения! muteMode=" + member.value.muteMode + ", expelPermit=" + member.value.expelPermit;
                            } else {
                                user.rank = member.value.rank;
                                user.setDirty(true);

                                member.value.rank = upperRank;
                                member.value.setDirty(true);

                                getDao().updateClanMember(user);
                                getDao().updateClanMember(member.value);

                                response.rank = upperRank;
                            }
                        } else if(!user.rank.isHigherThan(upperRank)) {
                            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                            response.logMessage = "Нельзя повысить до своего звания";
                        } else if(clan.vacancies(upperRank) <= 0) {
                            response.serviceResult = ServiceResult.ERR_CLAN_SIZE_LIMIT;
                            response.logMessage = "Нет свободных вакансий";
                        } else {
                            member.value.rank = upperRank;
                            member.value.setDirty(true);

                            getDao().updateClanMember(member.value);

                            response.rank = upperRank;
                        }
                    }
                }
            };
            concurrentService.execWrite(task, response);

            if(response.isOk()) {
                chatService.broadcastClanAction(clan, user, member.value, request, response);
                auditService.logClanAction(clan,
                        request.adminUserId > 0 ? ClanActionEnum.ADMIN_PROMOTE : ClanActionEnum.PROMOTE,
                        request.adminUserId > 0 ? request.adminUserId : user.profileId,
                        member.value, response.rank.code);
            }
        }

        if(logger.isInfoEnabled()) {
            logger.info(response.toString());
        }

        return response;
    }

    @Override
    public LowerInRankResponse lowerInRank(final LowerInRankRequest request, final ClanMember user) {

        if(logger.isInfoEnabled()) {
            logger.info(request.toString());
        }

        final LowerInRankResponse response = new LowerInRankResponse(request);
        response.rank = Rank.SOLDIER;

        if(user.isSelf(request.socialId, request.profileId)) {
            response.serviceResult = ServiceResult.ERR_INVALID_ARGUMENT;
            response.logMessage = "Нельзя понизить в звании самого себя";
        } else {
            Clan clan = user.clan;
            final VarObject<ClanMember> member = new VarObject<>();

            ClanTask task = new ClanTask(clan) {
                @Override
                protected void exec() {
                    member.value = clan.getMember(request.socialId, request.profileId);

                    if(member.value == null) {
                        response.serviceResult = ServiceResult.ERR_NOT_FOUND;
                        response.logMessage = "Не в клане";
                    } else if(!user.getClanId().equals(member.value.getClanId())) {
                        response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                        response.logMessage = "Нельзя понизить в звании члена другого клана";
                    } else if(!user.rank.canLowerInRank()) {
                        response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                        response.logMessage = "Не разрешено";
                    } else if(!user.rank.isHigherThan(member.value.rank)) {
                        response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                        response.logMessage = "Нельзя понизить старшего или равного по званию";
                    } else {
                        Rank memberRank = member.value.rank;
                        Rank lowerRank = memberRank.lower();

                        response.rank = memberRank;

                        if(lowerRank != request.targetRank || !memberRank.isHigherThan(lowerRank)) {
                            response.serviceResult = ServiceResult.ERR_INVALID_ARGUMENT;
                            response.logMessage = "Нельзя понизить " + memberRank + " до " + request.targetRank;
                        } else {
                            member.value.rank = lowerRank;
                            member.value.setDirty(true);

                            getDao().updateClanMember(member.value);

                            response.rank = lowerRank;
                        }
                    }
                }
            };
            concurrentService.execWrite(task, response);

            if(response.isOk()) {
                chatService.broadcastClanAction(clan, user, member.value, request, response);
                auditService.logClanAction(clan,
                        request.adminUserId > 0 ? ClanActionEnum.ADMIN_LOWER : ClanActionEnum.LOWER,
                        request.adminUserId > 0 ? request.adminUserId : user.profileId,
                        member.value, response.rank.code);
            }
        }

        if(logger.isInfoEnabled()) {
            logger.info(response.toString());
        }

        return response;
    }

    @Override
    public ExpandClanResponse expandClan(final ExpandClanRequest request, final ClanMember user) {

        if(logger.isInfoEnabled()) {
            logger.info(request.toString());
        }

        final ExpandClanResponse response = new ExpandClanResponse(request);

        if(!user.rank.canExpand() || (request.fromTreas && user.rank != Rank.LEADER)) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Не разрешено";
        } else if(user.clan.reviewState == ReviewState.LOCKED) {
            response.serviceResult = ServiceResult.ERR_CLAN_LOCKED;
            response.logMessage = "Клан заблокирован модератором";
        } else {
            Clan clan = user.clan;

            ClanTask task = new ClanTask(clan) {
                @Override
                protected void exec() {
                    if(request.level <= clan.level || request.level > clan.level + 1 || request.level > ClanLevel.LAST) {
                        response.serviceResult = ServiceResult.ERR_INVALID_ARGUMENT;
                        response.logMessage = "Ошибочный номер расширения " + request.level;
                    } else {
                        Price price = priceService.expandClanPrice(user.socialId, request.level);
                        if(request.fromTreas) {
                            if(price.amount > clan.treas) {
                                response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_MONEY;
                                response.logMessage = "Недостаточно средств в казне";
                            } else {
                                getDao().expandClan(clan.id, request.level);
                                response.level = ++clan.level;
                                clan.treas -= price.amount;
                            }
                        } else {
                            VarInt reservation = new VarInt();

                            if(price.amount > 0 && !interopService.reserveFunds(user.socialId, user.profileId, price, reservation)) {
                                response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_MONEY;
                                response.logMessage = "Недостаточно средств на счете";
                            } else {
                                getDao().expandClan(clan.id, request.level);
                                response.level = ++clan.level;

                                if(price.amount > 0 && !interopService.withdrawFunds(user.socialId, user.profileId, price, reservation.value, clan.id)) {
                                    logger.warn("Не удалось получить оплату за расширение клана с пользователя " + user);
                                }
                            }
                        }
                    }
                }
            };
            concurrentService.execWrite(task, response);

            if(response.isOk()) {
                chatService.broadcastClanAction(clan, user, null, request, response);
                auditService.logClanAction(clan, ClanActionEnum.EXPAND, user.profileId, response.level);
            }
        }

        if(logger.isInfoEnabled()) {
            logger.info(response.toString());
        }

        return response;
    }

    @Override
    public RenameClanResponse renameClan(final RenameClanRequest request, final ClanMember user) {

        if(logger.isInfoEnabled()) {
            logger.info(request.toString());
        }

        final RenameClanResponse response = new RenameClanResponse(request);
        request.name = nameNormalizer.trim(request.name);
        if(!user.rank.canEdit()) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Не разрешено";
        } else if(!nameNormalizer.isValidName(request.name)) {
            response.serviceResult = ServiceResult.ERR_INVALID_CLAN_NAME;
            response.logMessage = "Имя клана не соответствует требованиям";
        } else {
            Clan clan = user.clan;

            ClanTask task = new ClanTask(clan) {
                @Override
                protected void exec() {
                    Price price = priceService.renameClanPrice(user.socialId);
                    if(request.fromTreas) {
                        if(price.amount > clan.treas) {
                            response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_MONEY;
                            response.logMessage = "Недостаточно средств в казне";
                        } else {
                            try {
                                getDao().updateClanName(clan.id, request.name, ReviewState.NONE);

                                clan.name = request.name;
                                clan.reviewState = ReviewState.NONE;

                                ratingService.updateRatings(clan);

                                clan.treas -= price.amount;
                            } catch (DataIntegrityViolationException e) {
                                logger.warn(e.toString());
                                response.serviceResult = ServiceResult.ERR_CLAN_NAME_EXISTS;
                                response.logMessage = "Клан с похожим именем уже существует";
                            }
                        }
                    } else {
                        VarInt reservation = new VarInt();

                        if(price.amount > 0 && !interopService.reserveFunds(user.socialId, user.profileId, price, reservation)) {
                            response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_MONEY;
                            response.logMessage = "Недостаточно средств на счете";
                        } else {
                            try {
                                getDao().updateClanName(clan.id, request.name, ReviewState.NONE);

                                clan.name = request.name;
                                clan.reviewState = ReviewState.NONE;

                                ratingService.updateRatings(clan);

                                if(price.amount > 0 && !interopService.withdrawFunds(user.socialId, user.profileId, price, reservation.value, clan.id)) {
                                    logger.warn("Не удалось получить оплату за переименование клана с пользователя " + user);
                                }
                            } catch (DataIntegrityViolationException e) {
                                logger.warn(e.toString());
                                response.serviceResult = ServiceResult.ERR_CLAN_NAME_EXISTS;
                                response.logMessage = "Клан с похожим именем уже существует";
                            }
                        }
                    }
                }
            };
            concurrentService.execWrite(task, response);

            if(response.isOk()) {
                chatService.broadcastClanAction(clan, user, null, request, response);
                auditService.logClanAction(clan, ClanActionEnum.RENAME, user.profileId, 0);
            }
        }

        if(logger.isInfoEnabled()) {
            logger.info(response.toString());
        }

        return response;
    }

    @Override
    public ChangeClanEmblemResponse changeClanEmblem(final ChangeClanEmblemRequest request, final ClanMember user) {

        if(logger.isInfoEnabled()) {
            logger.info(request.toString());
        }

        final ChangeClanEmblemResponse response = new ChangeClanEmblemResponse(request);

        if(!user.rank.canEdit()) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Не разрешено";
        } else {
            Clan clan = user.clan;

            ClanTask task = new ClanTask(clan) {
                @Override
                protected void exec() {
                    Price price = priceService.changeClanEmblemPrice(user.socialId);
                    if(request.fromTreas) {
                        if(price.amount > clan.treas) {
                            response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_MONEY;
                            response.logMessage = "Недостаточно средств в казне";
                        } else {
                            getDao().updateClanEmblem(clan.id, request.emblem);

                            clan.emblem = request.emblem;
                            clan.treas -= price.amount;
                        }
                    } else {
                        VarInt reservation = new VarInt();

                        if(price.amount > 0 && !interopService.reserveFunds(user.socialId, user.profileId, price, reservation)) {
                            response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_MONEY;
                            response.logMessage = "Недостаточно средств на счете";
                        } else {
                            getDao().updateClanEmblem(clan.id, request.emblem);

                            clan.emblem = request.emblem;

                            if(price.amount > 0 && !interopService.withdrawFunds(user.socialId, user.profileId, price, reservation.value, clan.id)) {
                                logger.warn("Не удалось получить оплату за эмблему клана с пользователя " + user);
                            }
                        }
                    }
                }
            };
            concurrentService.execWrite(task, response);

            if(response.isOk()) {
                chatService.broadcastClanAction(clan, user, null, request, response);
                auditService.logClanAction(clan, ClanActionEnum.CHANGE_EMBLEM, user.profileId, 0);
            }
        }

        if(logger.isInfoEnabled()) {
            logger.info(response.toString());
        }

        return response;
    }

    @Override
    public ChangeClanDescriptionResponse changeClanDescription(final ChangeClanDescriptionRequest request, final ClanMember user) {

        if(logger.isInfoEnabled()) {
            logger.info(request.toString());
        }

        final ChangeClanDescriptionResponse response = new ChangeClanDescriptionResponse(request);

        if(!user.rank.canEdit()) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Не разрешено";
        } else {
            Clan clan = user.clan;

            ClanTask task = new ClanTask(clan) {
                @Override
                protected void exec() {
                    Price price = priceService.changeClanDescriptionPrice(user.socialId);
                    if(request.fromTreas) {
                        if(price.amount > clan.treas) {
                            response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_MONEY;
                            response.logMessage = "Недостаточно средств в казне";
                        } else {
                            getDao().updateClanDescription(clan.id, request.description, ReviewState.NONE);

                            clan.description = request.description;
                            clan.reviewState = ReviewState.NONE;

                            ratingService.updateRatings(clan);
                            clan.treas -= price.amount;
                        }
                    } else {

                        VarInt reservation = new VarInt();

                        if(price.amount > 0 && !interopService.reserveFunds(user.socialId, user.profileId, price, reservation)) {
                            response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_MONEY;
                            response.logMessage = "Недостаточно средств на счете";
                        } else {
                            getDao().updateClanDescription(clan.id, request.description, ReviewState.NONE);

                            clan.description = request.description;
                            clan.reviewState = ReviewState.NONE;

                            ratingService.updateRatings(clan);

                            if(price.amount > 0 && !interopService.withdrawFunds(user.socialId, user.profileId, price, reservation.value, clan.id)) {
                                logger.warn("Не удалось получить оплату за описание клана с пользователя " + user);
                            }
                        }
                    }
                }
            };
            concurrentService.execWrite(task, response);

            if(response.isOk()) {
                chatService.broadcastClanAction(clan, user, null, request, response);
                auditService.logClanAction(clan, ClanActionEnum.CHANGE_DESCRIPTION, user.profileId, 0);
            }
        }

        if(logger.isInfoEnabled()) {
            logger.info(response.toString());
        }

        return response;
    }

    @Override
    public ChangeClanReviewStateResponse changeClanReviewState(final ChangeClanReviewStateRequest request) {

        if(logger.isInfoEnabled()) {
            logger.info(request.toString());
        }

        final ChangeClanReviewStateResponse response = new ChangeClanReviewStateResponse(request);

        Clan clan = getClan(request.clanId);

        if(clan == null) {
            response.serviceResult = ServiceResult.ERR_NOT_FOUND;
            response.logMessage = "Клан не нейден";
        } else {
            ClanTask task = new ClanTask(clan) {
                @Override
                protected void exec() {
                    getDao().updateClanReviewState(clan.id, request.reviewState);
                    clan.reviewState = request.reviewState;
                }
            };
            concurrentService.execWrite(task, response);

            if(response.isOk()) {
                if(request.reviewState == ReviewState.LOCKED) {
                    chatService.broadcastClanAction(clan, null, null, request, response);
                }
                ratingService.updateRatings(clan);
            }
        }

        if(logger.isInfoEnabled()) {
            logger.info(response.toString());
        }

        return response;
    }

    public ChangeClanClosedStateResponse changeClanClosedState(final ChangeClanClosedStateRequest request, ClanMember user) {

        if(logger.isInfoEnabled()) {
            logger.info(request.toString());
        }

        final ChangeClanClosedStateResponse response = new ChangeClanClosedStateResponse(request);

        if(!user.rank.canEdit()) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Не разрешено";
        } else {
            Clan clan = user.clan;

            if(clan == null) {
                response.serviceResult = ServiceResult.ERR_NOT_FOUND;
                response.logMessage = "Клан не нейден";
            } else {
                ClanTask task = new ClanTask(clan) {
                    @Override
                    protected void exec() {
                        getDao().updateClanClosedState(clan.id, request.closed);
                        clan.closed = request.closed;
                    }
                };
                concurrentService.execWrite(task, response);

                if(response.isOk()) {
                    chatService.broadcastClanAction(clan, null, null, request, response);
                    auditService.logClanAction(clan, ClanActionEnum.CHANGE_CLOSE_STATE, user.profileId, clan.closed ? 1 : 0);
                }
            }

            if(logger.isInfoEnabled()) {
                logger.info(response.toString());
            }
        }
        return response;
    }

    @Override
    public ChangeClanJoinRatingResponse changeClanJoinRating(final ChangeClanJoinRatingRequest request, final ClanMember user) {

        if(logger.isInfoEnabled()) {
            logger.info(request.toString());
        }

        final ChangeClanJoinRatingResponse response = new ChangeClanJoinRatingResponse(request);

        if(!user.rank.canEdit()) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Не разрешено";
        } else {
            Clan clan = user.clan;

            ClanTask task = new ClanTask(clan) {
                @Override
                protected void exec() {
                    getDao().updateClanJoinRating(clan.id, request.joinRating);

                    clan.joinRating = request.joinRating;

                    ratingService.updateRatings(clan);
                }
            };
            concurrentService.execWrite(task, response);
        }

        if(logger.isInfoEnabled()) {
            logger.info(response.toString());
        }

        return response;
    }

    @Override
    public ClanTO[] joinClans(int rating, int limit) {
        ClanTO[] res;

        RatingItem[] items = ratingService.joinsN(rating, limit);

        if(items.length > 0) {
            int[] clansId = RatingItem.toClansId(items);
            res = ClanTO.convert(clanRepo.loadClans(clansId), ClanTO.SCOPE_HEADER, ratingService);
        } else {
            res = ClanTO.EMPTY_ARRAY;
        }

        return res;
    }

    @Override
    public UpdateRatingResponse updateRating(final UpdateRatingRequest request) {
        if(logger.isDebugEnabled()) {
            logger.debug(request.toString());
        }

        UpdateRatingResponse response = new UpdateRatingResponse(request);

        if(clanSeasonService.isRejectUpdateRating()) {
            return response;
        }

        final ClanMember member = getClanMember(request.socialId, request.profileId);

        if(member == null) {
            response.serviceResult = ServiceResult.ERR_NOT_FOUND;
            response.logMessage = "Не в клане";
        } else {
            ClanTask task = new ClanTask(member.clan) {
                @Override
                protected void exec() {
                    if(request.wipeRating) {
                        member.seasonRating = 0;
                        ratingService.wipeDailyRatings(member);
                    } else {
                        if(member.cashedMedals == 0) {
                            member.seasonRating = member.seasonRating + request.ratingPoints;
                        } else if(member.cashedMedals > 0) {
                            // если игрок обналичил медали, то его личный рейтинг в клане не опустится ниже оплаченного порога
                            member.seasonRating = Math.max(member.cashedMedals * RatingPerMedal, member.seasonRating + request.ratingPoints);
                        }
                        ratingService.updateDailyRating(member, request.ratingPoints);
                    }
                    member.setDirty(true);

                    clan.refreshAggregates();

                    ratingService.updateRatings(clan);
                }
            };
            concurrentService.execWrite(task, response);
        }

        if(logger.isDebugEnabled()) {
            logger.debug(response.toString());
        }

        return response;
    }

    @Override
    public CommonResponse updateClan(final ClanTO source) {
        CommonResponse response = new CommonResponse();

        Clan clan = getClan(source.id);

        source.name = nameNormalizer.trim(source.name);
        if(clan == null) {
            response.serviceResult = ServiceResult.ERR_NOT_FOUND;
            response.logMessage = "Клан не нейден";
        } else if(!nameNormalizer.isValidName(source.name)) {
            response.serviceResult = ServiceResult.ERR_INVALID_ARGUMENT;
            response.logMessage = "Имя клана не соответствует требованиям";
        } else if(getDao().clanExists(source.name, clan.id)) {
            response.serviceResult = ServiceResult.ERR_CLAN_NAME_EXISTS;
            response.logMessage = "Клан с похожим именем уже существует";
        } else {
            ClanTask task = new ClanTask(clan) {
                @Override
                protected void exec() {
                    getDao().updateClan(source.update(clan.copy()));
                    source.update(clan);
                }
            };
            concurrentService.execWrite(task, response);

            if(response.isOk()) {
                chatService.broadcastClanAction(clan, null, null, Messages.UPDATE_CLAN_REQUEST, response);
            }
        }

        return response;
    }

    @Override
    public ClanMember createClanMember(short socialId, int profileId, String socialProfileId, String name) {
        ClanMember user = new ClanMember();
        user.profileId = profileId;
        user.socialProfileId = socialProfileId;
        user.name = name;
        user.lastLoginTime = (int) (System.currentTimeMillis() / 1000L);

        user.setNew(true);

        clanRepo.putMember(user);

        return user;
    }

    @Override
    public Clan getClan(Integer clanId) {
        return clanRepo.getClan(clanId);
    }

    @Override
    public Clan getClanByMember(short socialId, int profileId) {
        return clanRepo.getClanByMember(socialId, profileId);
    }

    @Override
    public ClanMember[] getMembers(short socialId, int[] profilesId) {
        return clanRepo.getMembers(socialId, profilesId);
    }

    @Override
    public ClanMember getClanMember(short socialId, int profileId) {
        return clanRepo.getMember(socialId, profileId);
    }

    public ClanMember getClanMember(int profileId) {
        return clanRepo.getMember((short) 0, profileId);
    }

    private void refreshAggregates(Clan clan, CommonResponse response) {
        ClanTask task = new ClanTask(clan) {
            @Override
            protected void exec() {
                clan.refreshAggregates();
            }
        };
        concurrentService.execWrite(task, response);
    }

    private boolean interopOk(int interopCode, CommonResponse response) {
        if(interopCode != 0) {
            if(StringUtils.isEmpty(response.logMessage)) {
                response.logMessage = "INTEROP_CODE_" + interopCode;
            }
            if(response.isOk()) {
                response.serviceResult = ServiceResult.ERR_INTEROP;
            }
            return false;
        } else {
            return true;
        }
    }

    public boolean isFreezeClanSizeModifications() {
        return freezeClanSizeModifications;
    }

    public void setFreezeClanSizeModifications(boolean freezeClanSizeModifications) {
        this.freezeClanSizeModifications = freezeClanSizeModifications;
    }

    public ServiceResult donate(int profileId, short socialNetId, final int donation, final int donationComeback) {
        final ClanMember user = clanRepo.getMember(socialNetId, profileId);

        if(user == null) {
            return ServiceResult.ERR_NOT_FOUND;
        }
        Clan clan = user.clan;
        if(clan == null) {
            return ServiceResult.ERR_NOT_FOUND;
        } else {
            ClanTask task = new ClanTask(clan) {
                @Override
                protected void exec() {
                    user.donation += donation;
                    user.donationCurrSeason += donation;
                    user.donationCurrSeasonComeback += donationComeback;
                    user.setDirty(true);

                    clan.treas += donation;
                    clan.setDirty(true);
                }
            };
            concurrentService.execWrite(task, null);
            chatService.broadcastClanAction(clan, user, null, Messages.DONATE_REQUEST, String.valueOf(donation));
            auditService.logClanAction(clan, ClanActionEnum.DONATE, user.profileId, donation);
        }

        getDao().updateClanMember(user);

        ClanTask task = new ClanTask(user.clan) {
            @Override
            protected void exec() {
                getDao().updateClanAggregates(clan);
            }
        };
        concurrentService.execWrite(task, null);

        return ServiceResult.OK;
    }

    public ServiceResult changeClanMedalPrice(final byte medalPrice, ClanMember member) {
        if(!member.rank.canEdit()) {
            return ServiceResult.ERR_ACCESS_DENIED;
        }

        Clan clan = member.clan;
        if(clan == null) {
            return ServiceResult.ERR_NOT_FOUND;
        }
        if(medalPrice <= clan.medalPrice && debugChangeMedalPriceCheck) {
            logger.error("цену медали можно только увеличивать на протяжении сезона! clan.medalPrice={}, msg.medalPrice={}", clan.medalPrice, medalPrice);
            return ServiceResult.ERR_INVALID_STATE;
        }
        if(medalPrice < minMedalPrice || medalPrice > maxMedalPrice) {
            logger.error("цена медали не корректна! msg.medalPrice={}", medalPrice);
            return ServiceResult.ERR_INVALID_ARGUMENT;
        }
        if(medalPrice * minMedalAmount > clan.treas) {
            logger.error("в казне не достаточно рубинов на 20 медалей! treas={}, msg.medalPrice={}", clan.treas, medalPrice);
            return ServiceResult.ERR_NOT_ENOUGH_TREAS;
        }

        ClanTask task = new ClanTask(clan) {
            @Override
            protected void exec() {
                clan.medalPrice = medalPrice;
                getDao().updateClanMedalPrice(clan.id, medalPrice);
            }
        };
        concurrentService.execWrite(task, null);
        chatService.broadcastClanAction(clan, member, null, Messages.CHANGE_CLAN_MEDAL_PRICE_REQUEST, String.valueOf(medalPrice));
        auditService.logClanAction(clan, ClanActionEnum.SET_MEDAL_PRICE, member.profileId, medalPrice);

        return ServiceResult.OK;
    }

    public ServiceResult cashMedals(final int medals, final ClanMember member) {
        Clan clan = member.clan;
        if(clan == null) {
            return ServiceResult.ERR_NOT_FOUND;
        }
        if(clan.medalPrice == 0) {
            logger.error("clan.medalPrice == 0");
            return ServiceResult.ERR_INVALID_STATE;
        }
        if(member.seasonRating - (member.cashedMedals + medals) * RatingPerMedal < 0) {
            logger.error("не достаточно рейтинга! rating={}, cashedMedals={}", member.seasonRating, member.cashedMedals);
            return ServiceResult.ERR_NOT_ENOUGH_RATING;
        }
        if(medals * clan.medalPrice > clan.treas) {
            logger.error("не достаточно рубинов в казне! medalPrice={}, treas={}", clan.medalPrice, clan.treas);
            return ServiceResult.ERR_NOT_ENOUGH_TREAS;
        }
        ClanTask task = new ClanTask(clan) {
            @Override
            protected void exec() {
                member.cashedMedals += medals;
                member.setDirty(true);

                clan.treas -= medals * clan.medalPrice;
                clan.cashedMedals += medals;
                clan.setDirty(true);

                interopService.cashMedals(clan, member, medals * clan.medalPrice);
            }
        };
        concurrentService.execWrite(task, null);
        chatService.broadcastClanAction(clan, member, null, Messages.CASH_MEDALS_REQUEST, String.valueOf(medals));
        auditService.logClanAction(clan, ClanActionEnum.CASH_MEDALS, member.profileId, medals);

        return ServiceResult.OK;
    }

    public List<ClanAuditActionTO> auditActions(final ClanMember member) {
        Clan clan = member.clan;
        if(clan == null) {
            return Collections.emptyList();
        }
        return dao.selectClanActions(member.clan.id);
    }

    public void postToChat(short socialId, int profileId, int actionId, String text) {
        ClanMember member = clanRepo.getMember(socialId, profileId);
        if(member != null) {
            Clan clan = member.clan;
            if(clan != null) {
                chatService.broadcastClanAction(clan, member, null, actionId, text);
            }
        }
    }

    public ServiceResult setExpelPermit(final int profileId, final boolean value, final ClanMember leader) {
        if(leader.rank != Rank.LEADER) {
            return ServiceResult.ERR_ACCESS_DENIED;
        }
        Clan clan = leader.clan;
        if(clan == null) {
            return ServiceResult.ERR_NOT_FOUND;
        }
        final VarObject<ClanMember> member = new VarObject<>();
        final VarObject<ServiceResult> result = new VarObject<>(ServiceResult.OK);
        final ClanTask task = new ClanTask(clan) {
            @Override
            protected void exec() {
                member.value = clan.getMember(leader.socialId, profileId);
                if(member.value == null) {
                    result.value = ServiceResult.ERR_NOT_FOUND;
                    logger.warn("{} не в клане!", profileId);
                } else if(!leader.getClanId().equals(member.value.getClanId())) {
                    result.value = ServiceResult.ERR_ACCESS_DENIED;
                    logger.warn("Нельзя изменить свойство [expelPermit] члену другого клана! clanId={}", member.value.getClanId());
                } else if(member.value.rank != Rank.OFFICER) {
                    result.value = ServiceResult.ERR_INVALID_ARGUMENT;
                    logger.warn("Свойство [expelPermit] можно изменить только офицеру! member.rank={}", member.value.rank);
                } else if(member.value.expelPermit == value) {
                    result.value = ServiceResult.ERR_INVALID_ARGUMENT;
                    logger.warn("Значение не отличается от текущего! member.expelPermit={}", member.value.expelPermit);
                } else {
                    member.value.expelPermit = value;
                    member.value.setDirty(true);
                    getDao().updateClanMember(member.value);
                }
            }
        };
        concurrentService.execWrite(task, null);

        if(result.value.isOk()) {
            chatService.broadcastClanAction(clan, leader, member.value, Messages.SET_EXPEL_PERMIT_REQUEST, "" + value);
            auditService.logClanAction(clan, ClanActionEnum.SET_EXPEL_PERMIT, leader.profileId, member.value, value ? 1 : 0);
        }
        return result.value;
    }

    public ServiceResult setMuteMode(final int profileId, final boolean value, final ClanMember leader) {
        if(leader.rank != Rank.LEADER) {
            return ServiceResult.ERR_ACCESS_DENIED;
        }
        Clan clan = leader.clan;
        if(clan == null) {
            return ServiceResult.ERR_NOT_FOUND;
        }
        final VarObject<ClanMember> member = new VarObject<>();
        final VarObject<ServiceResult> result = new VarObject<>(ServiceResult.OK);
        final ClanTask task = new ClanTask(clan) {
            @Override
            protected void exec() {
                member.value = clan.getMember(leader.socialId, profileId);
                if(member.value == null) {
                    result.value = ServiceResult.ERR_NOT_FOUND;
                    logger.warn("{} не в клане!", profileId);
                } else if(!leader.getClanId().equals(member.value.getClanId())) {
                    result.value = ServiceResult.ERR_ACCESS_DENIED;
                    logger.warn("Нельзя изменить свойство [muteMode] члену другого клана! clanId={}", member.value.getClanId());
                } else if(member.value.muteMode == value) {
                    result.value = ServiceResult.ERR_INVALID_ARGUMENT;
                    logger.warn("Значение не отличается от текущего! member.expelPermit={}", member.value.expelPermit);
                } else if(leader.profileId == profileId) {
                    result.value = ServiceResult.ERR_INVALID_ARGUMENT;
                    logger.warn("Нельзя изменить свойство [muteMode] самому себе");
                } else {
                    member.value.muteMode = value;
                    member.value.setDirty(true);
                    getDao().updateClanMember(member.value);
                }
            }
        };
        concurrentService.execWrite(task, null);

        if(result.value.isOk()) {
            chatService.broadcastClanAction(clan, leader, member.value, Messages.SET_MUTE_MODE_REQUEST, "" + value);
            auditService.logClanAction(clan, ClanActionEnum.SET_MUTE_MODE, leader.profileId, member.value, value ? 1 : 0);
        }
        return result.value;
    }

}
