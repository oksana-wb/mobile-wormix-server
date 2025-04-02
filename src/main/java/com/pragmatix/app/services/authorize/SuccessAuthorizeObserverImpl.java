package com.pragmatix.app.services.authorize;

import com.pragmatix.app.common.LoginType;
import com.pragmatix.app.common.Race;
import com.pragmatix.app.dao.CallbackFriendDao;
import com.pragmatix.app.domain.PaymentStatisticEntity;
import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.server.CallbackersList;
import com.pragmatix.app.messages.server.EnterAccount;
import com.pragmatix.app.messages.server.WithdrawSeasonWeaponsResult;
import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.messages.structures.login_awards.BonusDaysAward;
import com.pragmatix.app.messages.structures.login_awards.DailyBonusAward;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.app.services.rating.SeasonService;
import com.pragmatix.clan.WormixClanService;
import com.pragmatix.common.utils.VarInt;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.social.service.VkontakteService;
import com.pragmatix.gameapp.social.service.vkontakte.UserSubscription;
import io.vavr.Tuple4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;

import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.FIRST_LOGIN_BY_DAY;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 17.05.2016 14:07
 */
public class SuccessAuthorizeObserverImpl implements SuccessAuthorizeObserver {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private StuffService stuffService;

    @Resource
    private GroupService groupService;

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private CallbackFriendService callbackFriendService;

    @Resource
    private CallbackFriendDao callbackFriendDao;

    @Resource
    private ReferralLinkService referralLinkService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private ProfileService profileService;

    @Resource
    private SkinService skinService;

    @Resource
    private SeasonService seasonService;

    @Resource
    private WormixClanService wormixClanService;

    @Resource
    private WeaponService weaponService;

    @Resource
    private CookiesService cookiesService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private Optional<VkontakteService> vkontakteServiceOpt;

    @Resource
    private PaymentService paymentService;

    @Resource
    private CraftService craftService;

    @Resource
    private BattleService battleService;

    final private ThreadLocal<List<Long>> callbackersList = new ThreadLocal<>();

    final private ThreadLocal<Boolean> needUpdate = new ThreadLocal<>();

    final private ThreadLocal<WithdrawSeasonWeaponsResult> withdrawSeasonWeaponsResult = new ThreadLocal<>();

    @Override
    public void beforeSendEnterAccount(UserProfile profile, ILogin login, EnterAccount enterAccount, ArrayList<LoginAwardStructure> loginAwards) {
        needUpdate.set(false);

        // удаляем предметы которые могли размножится по ошибке
        stuffService.removeDublicate(profile);

        // удаляем предметы с истекшим сроком действия
        stuffService.removeExpiredStuff(profile);

        //Выдача бесплатного бота в команду
        groupService.tryAddFreeMerchenary(profile);

        // Оружие, выдаваемое за новые уровни. Если у игрока такого оружия нет, добавляем в арсенал
        LoginAwardStructure awardStructure = profileBonusService.addLevelUpWeaponsAndAwards(profile, needUpdate);
        if(awardStructure != null) {
            loginAwards.add(awardStructure);
        }

        // компенсация на закрытие сезона
        // Старт сезона. Забираем временное оружие до выдачи награды за ТОП клана
        Tuple4<List<BackpackItemStructure>, Integer, Integer, Set<Integer>> result = seasonService.withdrawSeasonWeapon(profile);
        withdrawSeasonWeaponsResult.set(new WithdrawSeasonWeaponsResult(
                result._1,
                result._2,
                result._3,
                result._4.stream().mapToInt(i -> i).toArray(),
                seasonService.getCurrentSeasonWeaponsArr(),
                seasonService.getCurrentSeasonStuffArr()
        ));

        enterAccount.invites = wormixClanService.getInvitesFor(profile.getSocialId(), profile.getId());
        Tuple4<short[], short[], short[], Byte> backpackConfs = weaponService.getBackpackConfs(profile);
        enterAccount.backpackConf1 = backpackConfs._1;
        enterAccount.backpackConf2 = backpackConfs._2;
        enterAccount.backpackConf3 = backpackConfs._3;
        enterAccount.activeBackpackConf = backpackConfs._4;
        enterAccount.hotkeys = weaponService.getHotkeys(profile);
        enterAccount.cookies = cookiesService.cookiesToArray(cookiesService.getCookiesFor(profile));

        try {
            vkontakteServiceOpt.filter(s -> profile.getVipSubscriptionId() > 0).ifPresent(vkontakteService -> {
                Runnable task;
                Optional<UserSubscription> userSubscriptionOpt = vkontakteService.getUserSubscription(profile.id, profile.getVipSubscriptionId());
                if(userSubscriptionOpt.isPresent()) {
                    UserSubscription userSubscription = userSubscriptionOpt.get();
                    if(userSubscription.isSubscriptionActive()) {
                        int vipSubscriptionId = profile.getVipSubscriptionId();
                        task = paymentService.setVipSubscriptionExpireTime(profile, userSubscription.item_id, userSubscription.getSubscriptionExpireTime(), "updateByLogin", () -> {
                            PaymentStatisticEntity entity = paymentService.getPaymentStatisticEntityByTransactionId("sub_" + vipSubscriptionId);
                            if(entity == null) {
                                log.error("[{}] PaymentStatisticEntity not found by vipSubscriptionId [{}]", profile, vipSubscriptionId);
                            }
                            return entity;
                        });
                    } else {
                        task = paymentService.cancelVipSubscription(profile, "cancelByLogin", userSubscription, profile.getVipSubscriptionId());
                        profile.setVipSubscriptionId(0);
                    }
                } else {
                    task = null;
//                    task = paymentService.cancelVipSubscription(profile, "cancelByLogin", null, profile.getVipSubscriptionId());
//                    profile.setVipSubscriptionId(0);
                }
                if(task != null)
                    transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                            task.run();
                        }
                    });
            });
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        enterAccount.vipSubscriptionId = profile.getVipSubscriptionId();

        enterAccount.reconnectResult = battleService.onLogin(profile, LoginService.getParamOpt(login.getParams(), ILogin.RECONNECT_TO_BATTLE).map(Long::valueOf).orElse(0L), login.getVersion(),
                LoginService.getParamOpt(login.getParams(), ILogin.MISSION_ID).map(Short::valueOf).orElse((short) 0),
                LoginService.getParamOpt(login.getParams(), ILogin.LAST_TURN_TUM).map(Short::valueOf).orElse((short) 0)
        );

        if(!profile.inRace(Race.BOAR) && !Race.hasRace(profile.getRaces(), Race.valueOf(profile.getRace())) && !profileService.isVipActive(profile)) {
            int maxRaceOrdinal = Race.toList(profile.getRaces()).stream().mapToInt(Race::ordinal).max().orElse(Race.BOAR.ordinal());
            Race maxRace = Race.values()[maxRaceOrdinal];
            profile.setRace(maxRace);
            skinService.setActiveSkin(profile, maxRace, (byte) 0);
        }
    }

    @Override
    public void fillLoginAwards(UserProfile profile, long referrerId, String[] params, boolean isNewProfile, List<LoginAwardStructure> loginAwards, VarInt firstLoginByDay) {

        LoginType loginType = isNewProfile ? LoginType.registration : LoginType.regular;

        // проверяем нужно ли выдать бонус за возврат в игру
        callbackersList.set(Collections.emptyList());
        LoginAwardStructure comebackAward = profileBonusService.awardForComeback(profile);
        if(comebackAward != null) {
            loginAwards.add(comebackAward);

            needUpdate.set(true);
            // вернулся по приглашению
            if(referrerId > 0) {
                callbackFriendService.rewardCallbacker(profile, referrerId);
            } else {
                List<Long> callers = callbackFriendDao.selectCallers(profile.getId(), 24);
                callbackersList.set(callers);
                profile.setNeedRewardCallbackers(!callers.isEmpty());
            }

            if(loginType == LoginType.regular)
                loginType = LoginType.comeback;
        }

        // вернулся по приглашению
        // зашел повторно уже по ссылке, первый раз не определился кому отдать приз
        if(referrerId > 0 && profile.isNeedRewardCallbackers()) {
            callbackFriendService.rewardCallbacker(profile, referrerId);
        }

        // проверяем нужно ли выдать бонус за призовое место клана
        LoginAwardStructure topClanAward = profileBonusService.awardForTopClan(profile);
        if(topClanAward != null) {
            loginAwards.add(topClanAward);
        }

        // проверяем нужно ли выдать награду по итогам сезона
        seasonService.awardForSeason(profile).ifPresent(loginAwards::add);

        // проверяем нужно ли выдать бонус за возвращенных друзей
        LoginAwardStructure comebackedFriendsAward = profileBonusService.awardForComebackedFriends(profile);
        if(comebackedFriendsAward != null) {
            loginAwards.add(comebackedFriendsAward);
        }

        // если действует бонусный период
        BonusDaysAward bonusDaysAward = profileBonusService.awardForBonusDays(profile);
        if(bonusDaysAward != null) {
            loginAwards.add(bonusDaysAward);
        }

        // если зашли по реферальной ссылке
        String referralLinkToken = LoginService.getParam(params, ILogin.REFERRAL_LINK_TOKEN);
        if(!referralLinkToken.isEmpty()) {
            LoginAwardStructure referralLinkAward = referralLinkService.awardForReferralLink(referralLinkToken, profile);
            if(referralLinkAward != null) {
                loginAwards.add(referralLinkAward);
            }
        }

        // если положен ежедневный бонус за вход
        DailyBonusAward dailyBonusAward = profileBonusService.getDailyBonusAward(profile);
        if(dailyBonusAward != null) {
            loginAwards.add(dailyBonusAward);
            if(dailyBonusAward.firstLogin) {
                firstLoginByDay.value = 1;
                profileEventsService.fireEvent(FIRST_LOGIN_BY_DAY, profile, dailyBonusAward.lastLogin, params);
            }
        }

        // отправляем на клиент награды, выданные пока он был offline
        LoginAwardStructure[] offlineAwards = dailyRegistry.getOfflineAwards(profile.getId());
        if(offlineAwards != null && offlineAwards.length > 0) {
            Collections.addAll(loginAwards, offlineAwards);
            dailyRegistry.cleanOfflineAward(profile.getId());
        }

        // забираем апгрейды ставшие недоступными игроку по уровню. Компенсируем рубинами
        LoginAwardStructure awardStructure = craftService.downgradeWeaponsInaccessibleByLevel(profile);
        if(awardStructure != null) {
            loginAwards.add(awardStructure);
            needUpdate.set(true);
        }

//        profileService.logLoginByReferrer(profile.getProfileId(), LoginService.getParam(params, ILogin.REFERRER_PARAM_NAME), loginType);
    }

    @Override
    public void afterSentEnterAccount(UserProfile profile) {
        List<Long> callbackersList = this.callbackersList.get();
        if(!callbackersList.isEmpty()) {
            // заполняем массив строковых Id
            String[] callbackersListString = new String[callbackersList.size()];
            boolean empty = true;
            for(int i = 0; i < callbackersList.size(); i++) {
                callbackersListString[i] = profileService.getProfileStringId(callbackersList.get(i));
                empty = empty && callbackersListString[i].isEmpty();
            }
            // если все строковые Id пустые - передаем пустой массив
            callbackersListString = empty ? new String[0] : callbackersListString;

            Messages.toUser(new CallbackersList(callbackersList, callbackersListString));
        }

        WithdrawSeasonWeaponsResult withdrawSeasonWeaponsResult = this.withdrawSeasonWeaponsResult.get();
        if(withdrawSeasonWeaponsResult.compensationInMoney > 0 || !withdrawSeasonWeaponsResult.withdrawnWeapons.isEmpty()) {
            needUpdate.set(true);
        }
        Messages.toUser(withdrawSeasonWeaponsResult);

        if(needUpdate.get()) {
            profile.setDirty(true);
            profile.setLastLoginTime(new Date());
            // фиксируем сразу в базе
            profileService.updateSync(profile);
        }
    }

}
