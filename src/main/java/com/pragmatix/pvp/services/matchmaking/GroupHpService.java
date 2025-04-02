package com.pragmatix.pvp.services.matchmaking;

import com.pragmatix.app.common.TeamMemberType;
import com.pragmatix.app.init.LevelCreator;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.app.model.Level;
import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.services.StuffService;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.messages.PvpProfileStructure;
import com.pragmatix.pvp.model.BattleParticipant;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для подсчета общих жизней группы игрока
 */
@Service
public class GroupHpService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private LevelCreator levelCreator;

    @Resource
    private StuffService stuffService;

    // используется для снижения влияния HP шапки с ростом уровня
    @Value("${GroupHpService.levelOffset:25}")
    private int levelOffset = 25;

    @Value("${GroupHpService.logBase:1.4}")
    private double logBase = 1.4;

    private enum HpType {
        /**
         * считаем реальное HP
         */
        REAL,
        /**
         * считаем "подкрученное" HP, используемое для подбора в PVP
         */
        PVP_ADJUSTED,
    }

    /**
     * считает общее количество жизней группы
     *
     * @param profileStructure структура профайла игрока для которого будем считать
     * @param wormsGroup       его команда (обрезанная при необходимости)
     * @param hpType           считаем реальное хп (REAL) или "подкрученное", используемое для подбора (PVP_ADJUSTED)
     * @param debugInfo        (optional, out) структура, в которую добавить информацию о слагаемых этого хп (может быть null)
     * @return суммарное здоровье для всех членов команды
     */
    private int calculateGroupHp(UserProfileStructure profileStructure, WormStructure[] wormsGroup, HpType hpType, @Null List<Map<String, Object>> debugInfo) {
        int groupHp = 0;
        WormStructure masterWorm = getMasterWorm(profileStructure);
        Level masterLevel = getLevel(masterWorm.level);
        for(int i = 0; i < wormsGroup.length; i++) {
            WormStructure wormStructure = wormsGroup[i];
            putDebugInfo(debugInfo, i, "id", wormStructure.ownerId);
            putDebugInfo(debugInfo, i, "type", wormStructure.teamMemberType.getType());
            // учитываем жизни от шапки и артефакта
            int wormHp = 0;

            int hatHp = 0;
            int kitHp = 0;
            switch (hpType) {
                case REAL:
                    hatHp = getRealStuffHp(wormStructure.hat);
                    kitHp = getRealStuffHp(wormStructure.kit);
                    break;
                case PVP_ADJUSTED:
                    hatHp = getModStuffHp(wormStructure.hat, wormStructure, masterWorm);
                    kitHp = getModStuffHp(wormStructure.kit, wormStructure, masterWorm);
                    break;
            }
            wormHp += hatHp;
            wormHp += kitHp;

            putDebugInfo(debugInfo, i, "hat", wormStructure.hat);
            putDebugInfo(debugInfo, i, "hatHp", hatHp);
            putDebugInfo(debugInfo, i, "kit", wormStructure.kit);
            putDebugInfo(debugInfo, i, "kitHp", kitHp);

            // накапливаем жизни команды
            Level level = getLevel(wormStructure.teamMemberType.isSameLevel() ? masterWorm.level : Math.min(masterWorm.level, wormStructure.level));
            if(hpType == HpType.PVP_ADJUSTED && wormStructure.ownerId != masterWorm.ownerId) {
                // боремся с разбалансированными командами
                // т.е. если команда 30+1, то жизни для этой единички будут подсчитаны как для 30 уровня
                wormHp += Math.max(masterLevel.getLevelHp(), level.getLevelHp());
                putDebugInfo(debugInfo, i, "maxHpLimit", wormHp);
            } else {
                wormHp += level.getLevelHp();
                wormHp += getBaseRaceHp(wormStructure, level);
            }

            putDebugInfo(debugInfo, i, "level", level.getLevel());
            putDebugInfo(debugInfo, i, "levelHp", level.getLevelHp());
            putDebugInfo(debugInfo, i, "race", wormStructure.race);
            putDebugInfo(debugInfo, i, "raceHp", getBaseRaceHp(wormStructure, level));

            groupHp += wormHp;
            putDebugInfo(debugInfo, i, "hp", wormHp);

            putDebugInfo(debugInfo, i, "attack", wormStructure.attack);
            putDebugInfo(debugInfo, i, "armor", wormStructure.armor);
        }
        return groupHp;
    }

    private void putDebugInfo(@Null List<Map<String, Object>> debugInfo, int index, String key, Object value) {
        if(debugInfo != null && index >= 0 && index < debugInfo.size()) {
            debugInfo.get(index).put(key, String.valueOf(value));
        }
    }

    private Level getLevel(int lvl) {
        Level level = levelCreator.getLevel(lvl);
        if(level == null) {
            throw new IllegalArgumentException("Can't load level [" + lvl + "]");
        }
        return level;
    }

    private int getModStuffHp(short stuffId, WormStructure wormStructure, WormStructure masterWorm) {
        int stuffHp = 0;
        if(stuffId > 0) {
            Stuff stuff = stuffService.getStuff(stuffId);
            if(stuff != null) {
                //для себя учитываем только половину от жизней предмета
                if(wormStructure.ownerId == masterWorm.ownerId) {
                    stuffHp += getStuffHp(masterWorm.level, stuff.getHp()) / 2;
                } else {
                    stuffHp += getStuffHp(masterWorm.level, stuff.getHp());
                }
            } else {
                log.error("[{}] предмет не найден по id [{}]", wormStructure.ownerId, stuffId);
            }
        }
        return stuffHp;
    }

    // с ростом уровня влияние HP от шапки должно снижаться
    public int getStuffHp(int ownerLevel, int stuffHp) {
        return (int) Math.round(stuffHp / (Math.log((ownerLevel + levelOffset - 2)) / Math.log(logBase) - Math.log((levelOffset + 2)) / Math.log(logBase) + 1));
    }

    private int getRealStuffHp(short stuffId) {
        if(stuffId > 0) {
            Stuff stuff = stuffService.getStuff(stuffId);
            if(stuff != null) {
                return stuff.getHp();
            }
        }
        return 0;
    }

    public static WormStructure getMasterWorm(UserProfileStructure profileStructure) {
        WormStructure[] wormsGroup = profileStructure.wormsGroup();
        for(WormStructure wormStructure : wormsGroup) {
            if(wormStructure.ownerId == profileStructure.id) {
                return wormStructure;
            }
        }
        // в гладиаторском бою игрок не играет своим персонажем. Возвращаем любого (первого)
        return wormsGroup[0];
    }

    /**
     * Находит суммарный HP команды игрока (для случая PVP-боя)
     */
    public int calculateGroupHp(BattleParticipant participant, PvpBattleType battleType) {
        PvpProfileStructure profileStructure = participant.getPvpProfileStructure();
        return calculateGroupHp(profileStructure, profileStructure.wormsGroup(), HpType.PVP_ADJUSTED, null);
    }

    /**
     * Находит суммарный HP команды игрока (для случая боя с боссом)
     */
    public int calculateGroupHp(UserProfileStructure userProfileStructure) {
        WormStructure[] wormsGroup = trimInactiveUnits(userProfileStructure.wormsGroup());
        return calculateGroupHp(userProfileStructure, wormsGroup, HpType.REAL, null);
    }

    /**
     * Находит суммарный HP команды игрока (для случая боя с боссом), при этом сохраняя в {@code debugInfo}
     * отдельную информацию по каждому члену команды
     */
    public int getDetailedGroupHp(UserProfileStructure userProfileStructure, List<Map<String, Object>> debugInfo) {
        WormStructure[] wormsGroup = trimInactiveUnits(userProfileStructure.wormsGroup());
        if(debugInfo != null) {
            debugInfo.clear();
            for(WormStructure ignored : wormsGroup) {
                debugInfo.add(new HashMap<String, Object>());
            }
        }
        return calculateGroupHp(userProfileStructure, wormsGroup, HpType.REAL, debugInfo);
    }

    public static int calculateGroupLevel(PvpBattleType battleType, UserProfileStructure profileStructure) {
        int groupLevel = 0;
        WormStructure masterWorm = getMasterWorm(profileStructure);
        for(WormStructure wormStructure : profileStructure.wormsGroup()) {
            groupLevel += Math.min(masterWorm.level, wormStructure.level);
        }
        return groupLevel;
    }

    public static WormStructure[] trimUnitsCountIfNeed(PvpBattleType battleType, BattleWager wager, UserProfileStructure profileStructure) {
        WormStructure[] wormsGroup = trimInactiveUnits(profileStructure.wormsGroup());

        int wormGroupSize = 4;
        if(battleType != PvpBattleType.FRIEND_PvP && battleType != PvpBattleType.WAGER_PvP_DUEL) {
            // оставляем не болле 2-х червей в команде
            wormGroupSize = 2;
        } else if(battleType == PvpBattleType.WAGER_PvP_DUEL && wager == BattleWager.WAGER_20_DUEL){
            wormGroupSize = 1;
        }
        return battleType == PvpBattleType.WAGER_PvP_2x2 ? trimUnitsDropSoclan(profileStructure.id, wormsGroup) : trimUnitsCount(profileStructure.id, wormsGroup, wormGroupSize);
    }

    /**
     * усекает команду игрока
     */
    private static WormStructure[] trimUnitsCount(long profileId, WormStructure[] wormsGroup, int newGroupCount) {
        if(newGroupCount <= 0) {
            throw new IllegalArgumentException("newGroupCount должен быть > 0 [" + newGroupCount + "]");
        }
        if(wormsGroup.length <= newGroupCount) {
            return wormsGroup;
        }
        WormStructure[] newWormsGroup = new WormStructure[newGroupCount];
        Set<Long> droppedWorms = new HashSet<>(2);
        for(int i = wormsGroup.length - 1; i >= 0 && droppedWorms.size() < (wormsGroup.length - newGroupCount); i--) {
            WormStructure wormGroup = wormsGroup[i];
            if(wormGroup.ownerId != profileId) {
                droppedWorms.add(wormGroup.ownerId);
            }
        }
        int j = 0;
        for(WormStructure wormStructure : wormsGroup) {
            if(!droppedWorms.contains(wormStructure.ownerId)) {
                newWormsGroup[j] = wormStructure;
                j++;
            }
        }
        return newWormsGroup;
    }

    /**
     * усекает команду игрока:  персонаж игрока + 1 из оставшихся членов команды кроме соклана
     */
    public static WormStructure[] trimUnitsDropSoclan(long profileId, WormStructure[] wormsGroup) {
        WormStructure masterWorm = Arrays.stream(wormsGroup).filter(ws -> ws.ownerId == profileId).findFirst().get();
        List<WormStructure> team = Arrays.stream(wormsGroup).filter(ws -> ws.ownerId != profileId && ws.teamMemberType != TeamMemberType.SoclanMember).collect(Collectors.toList());
        if(team.isEmpty()) {
            return new WormStructure[]{masterWorm};
        } else{
            if(wormsGroup[0].ownerId == profileId) {
                return new WormStructure[]{masterWorm, team.get(0)};
            } else {
                return new WormStructure[]{team.get(0), masterWorm};
            }
        }
    }

    private static WormStructure[] trimInactiveUnits(WormStructure[] wormsGroup) {
        // не учитываем члена команды, бывшего соклановца
        int indexOfOtherClanTeamMember = indexOfDisabledTeamMember(wormsGroup);
        while (indexOfOtherClanTeamMember >= 0) {
            wormsGroup = ArrayUtils.remove(wormsGroup, indexOfOtherClanTeamMember);
            indexOfOtherClanTeamMember = indexOfDisabledTeamMember(wormsGroup);
        }
        return wormsGroup;
    }

    private static int indexOfDisabledTeamMember(WormStructure[] wormsGroup) {
        for(int i = 0; i < wormsGroup.length; i++) {
            if(!WormStructure.isActive(wormsGroup[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Вернет базовый hp для текущей расы игрока
     *
     * @param wormStructure профайл игрока
     * @param curLavel      текущий уровень игрока
     * @return базовый hp
     */
    public int getBaseRaceHp(WormStructure wormStructure, Level curLavel) {
        switch (wormStructure.race) {
            // BOXER
            case 2: {
                return (int) (curLavel.getLevelHp() * 0.1);
            }
            default: {
                return 0;
            }
        }
    }

}
