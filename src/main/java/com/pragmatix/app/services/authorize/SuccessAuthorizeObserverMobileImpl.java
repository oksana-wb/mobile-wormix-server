package com.pragmatix.app.services.authorize;

import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.server.EnterAccount;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.messages.structures.login_awards.BonusDaysAward;
import com.pragmatix.app.messages.structures.login_awards.DailyBonusAward;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.clan.WormixClanService;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.steam.SteamPurchaseService;
import com.pragmatix.common.utils.VarInt;

import javax.annotation.Resource;
import java.util.*;

import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.FIRST_LOGIN_BY_DAY;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.05.2016 14:07
 */
public class SuccessAuthorizeObserverMobileImpl implements SuccessAuthorizeObserver {

    @Resource
    private StuffService stuffService;

    @Resource
    private GroupService groupService;

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private BattleService battleService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private DepositService depositService;

    @Resource
    private CookiesService cookiesService;

    @Resource
    private SteamPurchaseService steamPurchaseService;

    @Resource
    private WeaponService weaponService;

    @Resource
    private CraftService craftService;

    @Resource
    private ProfileService profileService;

    @Resource
    private WormixClanService wormixClanService;

    final private ThreadLocal<Boolean> needUpdate = new ThreadLocal<>();

    @Override
    public void beforeSendEnterAccount(UserProfile profile, ILogin login, EnterAccount enterAccount, ArrayList<LoginAwardStructure> loginAwards) {
        // удаляем предметы которые могли размножится по ошибке
        stuffService.removeDublicate(profile);

        // удаляем предметы с истекшим сроком действия
        stuffService.removeExpiredStuff(profile);

        // выдача бесплатного бота в команду
        groupService.tryAddFreeMerchenary(profile);
        // увеличиваем атаку и защиту (в суммме должно быть 60) ранее взятым друзьям в комманду
        groupService.increaseAttackAndArmorToFriendsIfNeed(profile);

        // Оружие, выдаваемое за новые уровни. Если у игрока такого оружия нет, добавляем в арсенал
        LoginAwardStructure awardStructure = profileBonusService.addLevelUpWeaponsAndAwards(profile, needUpdate);
        if(awardStructure != null) {
            loginAwards.add(awardStructure);
        }

        // количество боёв на момент выхода из игры
        battleService.refundOfflineBattles(profile, login.getOfflineBattles());

        enterAccount.renameAct = (byte)(profile.getRenameAct() + profile.getRenameVipAct());
        enterAccount.deposits = depositService.getDepositStructuresFor(profile, new Date());

        enterAccount.invites = wormixClanService.getInvitesFor(profile.getSocialId(), profile.getId());
        enterAccount.hotkeys = weaponService.getHotkeys(profile);
        enterAccount.cookies = cookiesService.cookiesToArray(cookiesService.getCookiesFor(profile));

        steamPurchaseService.onLogin(profile);

        if(battleService.simpleBattleReconnectEnabled){
            enterAccount.reconnectResult = battleService.onLogin(profile, LoginService.getParamOpt(login.getParams(), ILogin.RECONNECT_TO_BATTLE).map(Long::valueOf).orElse(0L), login.getVersion(),
                    LoginService.getParamOpt(login.getParams(), ILogin.MISSION_ID).map(Short::valueOf).orElse((short) 0),
                    LoginService.getParamOpt(login.getParams(), ILogin.LAST_TURN_TUM).map(Short::valueOf).orElse((short) 0)
            );
        }
    }

    @Override
    public void fillLoginAwards(UserProfile profile, long referrerId, String[] params, boolean isNewProfile, List<LoginAwardStructure> loginAwards, VarInt firstLoginByDay) {
        needUpdate.set(false);

        //если действует бонусный период
        BonusDaysAward bonusDaysAward = profileBonusService.awardForBonusDays(profile);
        if(bonusDaysAward != null) {
            loginAwards.add(bonusDaysAward);
        }

        //если положен ежедневный бонус за вход
        DailyBonusAward dailyBonusAward = profileBonusService.getDailyBonusAward(profile);
        if(dailyBonusAward != null) {
            loginAwards.add(dailyBonusAward);
            if(dailyBonusAward.firstLogin) {
                firstLoginByDay.value = 1;
                profileEventsService.fireEvent(FIRST_LOGIN_BY_DAY, profile, dailyBonusAward.lastLogin, params);
            }
        }

        // проверяем нужно ли выдать бонус за призовое место клана
        LoginAwardStructure topClanAward = profileBonusService.awardForTopClan(profile);
        if(topClanAward != null) {
            loginAwards.add(topClanAward);
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
    }

    @Override
    public void afterSentEnterAccount(UserProfile profile) {
        if(needUpdate.get()) {
            profile.setDirty(true);
            profile.setLastLoginTime(new Date());
            // фиксируем сразу в базе
            profileService.updateSync(profile);
        }
    }

}
