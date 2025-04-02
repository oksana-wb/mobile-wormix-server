package com.pragmatix.app.services;

import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.craft.domain.Reagent;
import com.pragmatix.craft.domain.ReagentsEntity;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.gameapp.services.TaskService;
import com.pragmatix.gameapp.sessions.Connection;
import com.pragmatix.gameapp.threads.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.01.2016 18:10
 */
@Service
public class ProfileEventsService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    ReactionRateService reactionRateService;

    @Resource
    WeaponService weaponService;

    public enum ProfileEventEnum {
        LOGIN,
        LOGOUT,
        RECONNECT,
        FIRST_LOGIN_BY_DAY,
        START_SIMPLE_BATTLE,
        END_SIMPLE_BATTLE,
        SHUTDOWN_SERVER,
        RESTORE_BATTLES,
        START_PVP_BATTLE,
        END_PVP_BATTLE,
        END_FRIEND_BATTLE,
        END_PVE_BATTLE,
        END_GLADIATOR_BATTLE,
        END_QUEST_BATTLE,
        END_MERCENARIES_BATTLE,
        AWARD,
        PAYMENT,
        SUBSCRIPTION,
        PURCHASE,
        WIPE,
        PUMP_REACTION,
        CHEAT,
        EXTRA,
        ACHIEVEMENTS,
    }

    public enum Param {
        profile_reagents("profile#reagents"),
        profile_reaction("profile#reaction"),
        profile_reactionLevel("profile#reactionLevel"),
        profile_backpack("profile#backpack"),
        profile_stuff("profile#stuff"),
        profile_temporalStuff("profile#temporalStuff"),
        profile_recipes("profile#recipes"),
        profile_races("profile#races"),
        profile_skins("profile#skins"),

        eventType,
        eventSource,
        money,
        extraMoney,
        wagerToken,
        bossToken,
        bossBattleResultType,
        battleTime,
        battleTurns,
        realMoney,
        experience,
        healthInPercent,
        battles,
        reagents,
        itemId,
        items,
        itemCount,
        boostFactor,
        weapons,
        note,
        battleId,
        missionId,
        version,
        recipeId,
        reaction,
        friendId,
        race,
        skins,

        extraParams,

        lastLoginTime,
        pickUpSequence,
        vipExpireDate,

        result,

        endSimpleBattleMessage,
        endPvpBattleMessage,;

        private final String name;

        Param() {
            this.name = name();
        }

        Param(String name) {
            this.name = name;
        }

        public static Param of(@NotNull MoneyType moneyType) {
            return moneyType == MoneyType.REAL_MONEY ? realMoney : money;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    @Autowired(required = false)
    private List<ProfileEventsServiceI> listeners = Collections.emptyList();

    @Resource
    private TaskService taskService;

    public void fireProfileEventAsync(final ProfileEventEnum event, final UserProfile profile, final Object... params) {
        Date date = new Date();
        taskService.addSimpleTask(() -> {
            for(ProfileEventsServiceI listener : listeners) {
                try {
                    listener.fireProfileEvent(date, event, profile, params);
                } catch (Exception | LinkageError e) {
                    log.error(e.toString(), e);
                }
            }
        });
    }

    public void fireAchieveEventAsync(final ProfileEventEnum event, final ProfileAchievements profileAchievements, final Object... params) {
        Date date = new Date();
        taskService.addSimpleTask(() -> {
            for(ProfileEventsServiceI listener : listeners) {
                try {
                    listener.fireAchieveEvent(date, event, profileAchievements, params);
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }
            }
        });
    }

    public String[] fillBackpackTrimmed(UserProfile userProfile) {
        return userProfile.getBackpack().stream()
                .filter(BackpackItem::isNotEmpty)
                .map(item -> weaponService.isPresentInfinitely(item) ? "" + item.getWeaponId() : "" + item.getWeaponId() + ":" + item.getCount())
                .toArray(String[]::new);
    }

    /**
     * Передаём полную информацию по профилю
     */
    public void fireEvent(ProfileEventEnum event, UserProfile profile, Date lastLogin, String[] loginParams) {
        String[] backpack = fillBackpackTrimmed(profile);

        ReagentsEntity reagents = profile.getReagents();
        Map<Reagent, Integer> reagentValues = reagents != null ? reagents.getReagentValues() : null;

        fireProfileEventAsync(event, profile,
                Param.version, AppParams.versionToString(profile.version),
                "loginParams", loginParams,
                Param.profile_reaction, profile.getReactionRate(),
                Param.profile_reactionLevel, reactionRateService.getReactionLevel(profile.getReactionRate()),
                Param.profile_reagents, reagentValues,
                Param.profile_backpack, backpack,
                Param.profile_recipes, profile.getRecipes(),
                Param.profile_stuff, profile.getStuff(),
                Param.profile_temporalStuff, profile.getTemporalStuff().length > 0 ? TemporalStuffService.toStringTemporalStuff(profile.getTemporalStuff()) : null,
                Param.lastLoginTime, lastLogin,
                Param.pickUpSequence, profile.getLoginSequence(),
                "remoteAddress", Optional.ofNullable(Execution.EXECUTION.get().getConnection()).map(Connection::getIP).orElse("")
        );
    }

}
