package com.pragmatix.app.services;

import com.google.gson.Gson;
import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.services.AchieveCommandService;
import com.pragmatix.app.messages.client.EndBattle;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.WagerDef;
import com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.craft.domain.ReagentsEntity;
import com.pragmatix.intercom.messages.EndPvpBattleRequest;
import com.pragmatix.pvp.services.PvpService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.*;
import static com.pragmatix.app.services.ProfileEventsService.Param.*;
import static java.util.stream.Collectors.toList;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.01.2016 16:43
 */
@Service
public class ProfileEventsStatService implements ProfileEventsServiceI {

    @Resource
    private BattleService battleService;

    @Resource
    private AchieveCommandService achieveService;

    @Resource
    private DailyRegistry dailyRegistry;

    @Value("${ProfileEventsStatService.enabled:true}")
    private boolean enabled = true;

    private final Logger log = LoggerFactory.getLogger("EVENTS_CDR_LOGGER");

    private final Gson gson = new Gson();

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    @Override
    public void fireProfileEvent(Date date, ProfileEventEnum event, UserProfile profile, Object... params) {
        if(!enabled) return;

        Map<String, Object> statRowAll = new LinkedHashMap<>();
        statRowAll.put("date", sdf.format(date));
        statRowAll.put("profileId", profile.getId());
        putIfNonEmpty(statRowAll, "socialId", profile.getProfileStringId());
        statRowAll.put("event", event);

        consumeParams(event, params, statRowAll);

        Map<String, Object> statRow = new LinkedHashMap<>();
        Map<String, Object> statRowProfile = new LinkedHashMap<>();
        
        statRowAll.forEach((param, value) -> {
            if(param.startsWith("profile#")) {
                statRowProfile.put(param.substring(8), value);
            } else {
                statRow.put(param, value);
            }
        });
        
        statRowProfile.put("level", profile.getLevel());
        putIfNonEmpty(statRowProfile, "money", profile.getMoney());
        putIfNonEmpty(statRowProfile, "realMoney", profile.getRealMoney());
        putIfNonEmpty(statRowProfile, "rankPoints", profile.getRankPoints());
        statRowProfile.put("bestRank", profile.getBestRank());
        putIfNonEmpty(statRowProfile, "rating", profile.getRating());
        putIfNonEmpty(statRowProfile, "experience", profile.getExperience());
        putIfNonEmpty(statRowProfile, "battles", profile.getBattlesCount());
        putIfNonEmpty(statRowProfile, "bossToken", dailyRegistry.getBossWinAwardToken(profile.getId()));
        putIfNonEmpty(statRowProfile, "wagerToken", dailyRegistry.getWagerWinAwardToken(profile.getId()));

        statRow.put("profile", statRowProfile);
        
        log.info(gson.toJson(statRow));
    }

    @Override
    public void fireAchieveEvent(Date date, ProfileEventEnum event, ProfileAchievements profileAchievements, Object... params) {
        if(!enabled) return;

        Map<String, Object> statRow = new LinkedHashMap<>();
        statRow.put("date", sdf.format(date));
        putIfNonEmpty(statRow, "socialId", profileAchievements.getProfileId());
        statRow.put("event", event);

        consumeParams(event, params, statRow);

        Map<String, Object> statRowProfile = new LinkedHashMap<>();
        putIfNonEmpty(statRowProfile, "achievePoints", achieveService.getAchievePoints(profileAchievements));
        putIfNonEmpty(statRowProfile, "investedAwardPoints", profileAchievements.getInvestedAwardPoints());

        statRow.put("profile", statRowProfile);

        log.info(gson.toJson(statRow));
    }

    private void consumeParams(ProfileEventEnum event, Object[] params, Map<String, Object> statRow) {
        if(event == END_SIMPLE_BATTLE) {
            EndBattle msg = (EndBattle) params[1];

            statRow.put(battleId.toString(), msg.battleId);
            putIfNonEmpty(statRow, missionId.toString(), msg.missionId);
            if(msg.missionId == 0)
                statRow.put("battleType", msg.type);
            statRow.put(result.toString(), msg.result.name());
            putIfNonEmpty(statRow, money, msg.battleAward.money);
            putIfNonEmpty(statRow, realMoney, msg.battleAward.realMoney);
            putIfNonEmpty(statRow, "rareItemId", msg.battleAward.rareItem);
            if(msg.battleAward.boostFactor > 1)
                statRow.put(boostFactor.toString(), msg.battleAward.boostFactor);
            if(msg.battleAward.collectedReagents.size() > 0)
                statRow.put("" + reagents, ReagentsEntity.getReagentValues(msg.battleAward.collectedReagents));
            if(ArrayUtils.isNotEmpty(msg.items))
                statRow.put(weapons.toString(), Arrays.stream(msg.items).map(item -> "" + item.weaponId + ":-" + item.count).collect(toList()));
            putIfNonEmpty(statRow, experience, msg.battleAward.experience);
            putIfNonEmpty(statRow, bossToken, msg.battleAward.bossWinAwardToken);
            putIfNonEmpty(statRow, bossBattleResultType, msg.battleAward.bossBattleResultTypeName());

            consumeParamPairs(params, 2, statRow);
        } else if(event == END_PVP_BATTLE || event == END_PVE_BATTLE) {
            EndPvpBattleRequest msg = (EndPvpBattleRequest) params[1];

            statRow.put(battleId.toString(), msg.battleId);
            statRow.put("battleType", msg.battleType);
            statRow.put(result.toString(), msg.result);
            statRow.put("participantState", msg.participantState);
            if(msg.offlineTime > 0) statRow.put("offlineTime", PvpService.formatTime(msg.offlineTime));
            if(event == END_PVP_BATTLE) {
                WagerDef wagerDef = battleService.getBattleWagerDef(msg.wager, msg.teamSize);
                statRow.put("wager", wagerDef.value);
                statRow.put("teamSize", msg.teamSize);
                putIfNonEmpty(statRow, "droppedUnits", msg.droppedUnits);
                putIfNonEmpty(statRow, "ratingPoints", msg.ratingPoints);
                putIfNonEmpty(statRow, "rankPoints", msg.rankPoints);
            } else {
                statRow.put(missionId.toString(), msg.missionIds);
                putIfNonEmpty(statRow, realMoney, msg.battleAward.realMoney);
                putIfNonEmpty(statRow, "rareItemId", msg.battleAward.rareItem);
                if(msg.battleAward.boostFactor > 1)
                    statRow.put(boostFactor.toString(), msg.battleAward.boostFactor);
                statRow.put(battles.toString(), -1);
            }
            putIfNonEmpty(statRow, money, msg.battleAward.money);
            putIfNonEmpty(statRow, extraMoney, msg.battleAward.extraMoney);
            putIfNonEmpty(statRow, wagerToken, msg.battleAward.wagerWinAwardToken);
            putIfNonEmpty(statRow, bossToken, msg.battleAward.bossWinAwardToken);
            putIfNonEmpty(statRow, bossBattleResultType, msg.battleAward.bossBattleWinTypeName());
            putIfNonEmpty(statRow, experience, msg.battleAward.experience);
            putIfNonEmpty(statRow, healthInPercent, msg.battleAward.healthInPercent);
            if(ArrayUtils.isNotEmpty(msg.battleAward.collectedReagents))
                statRow.put("" + reagents, ReagentsEntity.getReagentValues(msg.battleAward.collectedReagents));
            if(ArrayUtils.isNotEmpty(msg.items))
                statRow.put(weapons.toString(), Arrays.stream(msg.items).map(item -> "" + item.weaponId + ":-" + item.count).collect(toList()));

            consumeParamPairs(params, 2, statRow);
        } else {
            consumeParamPairs(params, statRow);
        }
    }

    public static void consumeParamPairs(Object[] params, Map<String, Object> statRow) {
        consumeParamPairs(params, 0, statRow);
    }

    public static void consumeParamPairs(Object[] params, int from, Map<String, Object> statRow) {
        for(int i = from; i < params.length; i = i + 2) {
            Object paramName = params[i];
            Object value = params[i + 1];
            if(paramName == null || value == null) {
                continue;
            }
            if(paramName == Param.extraParams){
                 consumeParamPairs((Object[]) value, statRow);
            }else if(value instanceof Date) {
                Date date = (Date) value;
                if(date.getTime() > 0) {
                    value = AppUtils.formatDate(date);
                    statRow.put("" + paramName, value);
                }
            } else if(value instanceof Number) {
                putIfNonEmpty(statRow, "" + paramName, ((Number) value).intValue());
            } else if(value instanceof String) {
                putIfNonEmpty(statRow, "" + paramName, (String) value);
            } else if(value instanceof Map) {
                if(((Map) value).size() > 0)
                    statRow.put("" + paramName, value);
            } else if(value instanceof Collection) {
                if(((Collection) value).size() > 0)
                    statRow.put("" + paramName, value);
            } else if(value instanceof byte[]) {
                if(((byte[]) value).length > 0)
                    statRow.put("" + paramName, value);
            } else if(value instanceof int[]) {
                if(((int[]) value).length > 0)
                    statRow.put("" + paramName, value);
            } else if(value instanceof short[]) {
                if(((short[]) value).length > 0)
                    statRow.put("" + paramName, value);
            } else if(value instanceof long[]) {
                if(((long[]) value).length > 0)
                    statRow.put("" + paramName, value);
            } else if(value instanceof Object[]) {
                if(((Object[]) value).length > 0)
                    statRow.put("" + paramName, value);
            } else {
                statRow.put("" + paramName, value);
            }
        }
    }

    private void putIfNonEmpty(Map<String, Object> statRow, Param param, int value) {
        putIfNonEmpty(statRow, param.toString(), value);
    }

    private void putIfNonEmpty(Map<String, Object> statRow, Param param, String value) {
        putIfNonEmpty(statRow, param.toString(), value);
    }

    private static void putIfNonEmpty(Map<String, Object> statRow, String param, int value) {
        if(value != 0) statRow.put(param, value);
    }

    private static void putIfNonEmpty(Map<String, Object> statRow, String param, String value) {
        if(StringUtils.isNotEmpty(StringUtils.trim(value))) statRow.put(param, value);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
