package com.pragmatix.arena.mercenaries;

import com.pragmatix.app.common.*;
import com.pragmatix.app.init.LevelCreator;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.app.settings.GenericAwardProducer;
import com.pragmatix.arena.mercenaries.messages.BackpackItemShortStruct;
import com.pragmatix.arena.mercenaries.messages.MercenariesTeamMember;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.craft.domain.ReagentsEntity;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.END_MERCENARIES_BATTLE;
import static com.pragmatix.arena.mercenaries.MercenariesEntity.TEAM_SIZE;

public class MercenariesService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    MercenariesDao dao;

    @Resource
    StatisticService statisticService;

    @Resource
    ProfileEventsService profileEventsService;

    @Resource
    ProfileBonusService profileBonusService;

    @Resource
    DailyRegistry dailyRegistry;

    @Resource
    BattleService battleService;

    @Value("${Mercenaries.attemptsByDay:10}")
    public int attemptsByDay = 10;

    @Value("${MercenariesService.debugMode:false}")
    boolean debugMode = false;

    @Value("${Mercenaries.ticketBattlesPrice:3}")
    public int ticketBattlesPrice = 3;

    @Value("${Mercenaries.completeSeriesWinCondition:3}")
    public int completeSeriesWinCondition = 3;

    @Value("${Mercenaries.completeSeriesDefeatCondition:5}")
    public int completeSeriesDefeatCondition = 5;

    @Resource(name = "Mercenaries.completeSeriesAwardByWin")
    public Map<Integer, List<GenericAwardProducer>> completeSeriesAwardByWin;

    private Map<Byte, MercenariesTeamMember> mercenariesDefs = Collections.emptyMap();

    @Resource(name = "Mercenaries.mercenariesDefs")
    public void setMercenariesDefs(List<MercenariesTeamMember> mercenariesDefs) {
        Map<Byte, MercenariesTeamMember> mercenariesMap = new ConcurrentHashMap<>();
        for(MercenariesTeamMember mercenariesDef : mercenariesDefs) {
            mercenariesMap.put(mercenariesDef.id, mercenariesDef);
        }
        this.mercenariesDefs = mercenariesMap;
    }

    public void updateMercenaryDef(MercenariesTeamMember mercenaryDef){
        mercenariesDefs.put(mercenaryDef.id, mercenaryDef);
    }

    public MercenariesEntity mercenariesEntity(final UserProfile profile) {
        if(profile.getMercenariesEntity() == null) {
            synchronized (profile) {
                if(profile.getMercenariesEntity() == null) {
                    int profileId = profile.getId().intValue();
                    MercenariesEntity entity = dao.find(profileId);
                    if(entity == null)
                        entity = newMercenariesEntity(profileId);
                    profile.setMercenariesEntity(entity);
                }
            }
        }
        return profile.getMercenariesEntity();
    }

    public Collection<MercenariesTeamMember> mercenariesDefs() {
        return mercenariesDefs.values();
    }

    public int attemptsRemainToday(UserProfile profile) {
        return Math.max(0, attemptsByDay - dailyRegistry.getMercenariesBattleSeries(profile.getId()));
    }

    public void wipeMercenariesState(UserProfile profile) {
        if(profile.getMercenariesEntity() != null) {
            dao.delete(profile.getId().intValue(), true);
            profile.setMercenariesEntity(null);
        }
    }

    public boolean isSeriesCompleted(MercenariesEntity entity) {
        return entity.win >= completeSeriesWinCondition || entity.defeat >= completeSeriesDefeatCondition;
    }

    public boolean isSeriesInProgress(UserProfile profile, MercenariesEntity entity) {
        return entity.win < completeSeriesWinCondition && entity.defeat < completeSeriesDefeatCondition && entity.open && entity.isTeamFull() && attemptsRemainToday(profile) > 0;
    }

    public void consumeBattleResult(UserProfile profile, PvpBattleResult battleResult, byte[] collectedReagents, String battleTime) {
        MercenariesEntity entity = mercenariesEntity(profile);
        if(isSeriesInProgress(profile, entity)) {
            applyBattleResult(entity, battleResult, profile);

            profileEventsService.fireProfileEventAsync(END_MERCENARIES_BATTLE, profile,
                    "num", entity.num,
                    "team", entity.team,
                    Param.result, battleResult,
                    Param.battleTime, battleTime,
                    Param.reagents, ReagentsEntity.getReagentValues(collectedReagents)

            );
        } else {
            log.error("[{}] получен результат битвы наёмников но состоянии серии не корректно! {} battleResult={}", profile, entity, battleResult);
        }
    }

    private void applyBattleResult(MercenariesEntity entity, PvpBattleResult battleResult, UserProfile profile) {
        if(battleResult == PvpBattleResult.WINNER) {
            entity.win = !debugMode ? (byte) (entity.win + 1) : (byte) completeSeriesWinCondition;
            entity.total_win++;
        } else if(battleResult == PvpBattleResult.NOT_WINNER) {
            entity.defeat = (byte) (entity.defeat + 1);
            entity.total_defeat++;
        } else {
            entity.draw = (byte) (entity.draw + 1);
            entity.total_draw++;
        }
        if(entity.win + entity.defeat + entity.draw == 1) entity.startSeries = AppUtils.currentTimeSeconds();
        entity.touch();
    }

    public List<GenericAwardStructure> getReward(UserProfile profile) {
        MercenariesEntity entity = mercenariesEntity(profile);
        if(isSeriesCompleted(entity)) {
            List<GenericAwardProducer> completeSeriesAward = completeSeriesAwardByWin.get((int) entity.win);
            if(completeSeriesAward == null) {
                log.error("не определены награды для количества побед {}", entity.win);
                return Collections.emptyList();
            }
            List<GenericAwardStructure> result;
            try {
                result = profileBonusService.awardProfile(completeSeriesAward, profile, AwardTypeEnum.MERCENARIES_BATTLE,
                        "num", entity.num,
                        "win", entity.win,
                        "defeat", entity.defeat,
                        "draw", entity.draw,
                        "started", new Date(entity.startSeries * 1000L)
                );
            } finally {
                GenericAwardProducer.removeRandomAwardSeed();
            }
            closeSeries(entity);
            dailyRegistry.incMercenariesBattleSeries(profile.getId());
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    public void persistEntity(MercenariesEntity entity) {
        if(entity != null)
            dao.persist(entity, true);
    }

    public Pair<MercenariesEntity, MercenariesErrorEnum> setTeamMembers(UserProfile profile, byte[] team) {
        try {
            MercenariesEntity entity = mercenariesEntity(profile);
            if(!validateTeam(team)) {
                return new ImmutablePair<>(null, MercenariesErrorEnum.INVALID_TEAM);
            } else {
                entity.team = team;
                entity.touch();
                return new ImmutablePair<>(entity, null);
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
            return new ImmutablePair<>(null, MercenariesErrorEnum.ERROR);
        }
    }

    private boolean validateTeam(byte[] team) {
        if(team.length != TEAM_SIZE)
            return false;
        Set<Byte> set = new HashSet<>(TEAM_SIZE);
        for(byte mercId : team) {
            if(mercId == 0 || !set.add(mercId) || mercenariesDefs.get(mercId) == null)
                return false;
        }
        return true;
    }

    public Pair<MercenariesEntity, MercenariesErrorEnum> buyTicket(UserProfile profile) {
        MercenariesEntity entity = mercenariesEntity(profile);
        if(entity.open) {
            return new ImmutablePair<>(null, MercenariesErrorEnum.ALREADY_OPEN);
        } else if(profile.getBattlesCount() < ticketBattlesPrice) {
            return new ImmutablePair<>(null, MercenariesErrorEnum.NO_ENOUGH_BATTLES);
        } else {
            battleService.decBattleCount(profile, ticketBattlesPrice);

            profileEventsService.fireProfileEventAsync(ProfileEventEnum.PURCHASE, profile,
                    Param.eventType, ItemType.MERCENARIES_TICKET,
                    Param.battles, -ticketBattlesPrice
            );
            statisticService.buyItemStatistic(profile.getId(), 2, ticketBattlesPrice, ItemType.MERCENARIES_TICKET, 1, 0, profile.getLevel());

            startSeries(entity);
            persistEntity(entity);
            return new ImmutablePair<>(entity, null);
        }
    }

    public WormStructure[] wormsGroup(UserProfile profile) {
        MercenariesEntity coliseumEntity = mercenariesEntity(profile);
        WormStructure[] result = new WormStructure[coliseumEntity.team.length];
        for(int i = 0; i < coliseumEntity.team.length; i++) {
            byte mercenaryId = coliseumEntity.team[i];
            result[i] = new WormStructure(mercenariesDefs.get(mercenaryId));
        }
        return result;
    }

    public int[] backpack(UserProfile profile) {
        MercenariesEntity coliseumEntity = mercenariesEntity(profile);
        List<Integer> resultList = new ArrayList<>();
        for(byte mercId : coliseumEntity.team) {
            MercenariesTeamMember teamMember = mercenariesDefs.get(mercId);
            for(BackpackItemShortStruct backpackItem : teamMember.backpack) {
                resultList.add(BackpackUtils.toItem(backpackItem.weaponId, backpackItem.count));
            }
        }
        int[] result = new int[resultList.size()];
        for(int i = 0; i < resultList.size(); i++) {
            result[i] = resultList.get(i);
        }
        return result;
    }


    private MercenariesEntity startSeries(MercenariesEntity entity) {
        entity.open = true;
        entity.win = 0;
        entity.defeat = 0;
        entity.draw = 0;
        entity.num += 1;

        entity.touch();

        return entity;
    }

    private MercenariesEntity closeSeries(MercenariesEntity entity) {
        entity.open = false;
        entity.win = 0;
        entity.defeat = 0;
        entity.draw = 0;
        entity.touch();
        return entity;
    }

    private MercenariesEntity newMercenariesEntity(int profileId) {
        MercenariesEntity result = new MercenariesEntity(
                profileId,
                false,
                0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                0,
                0,
                0,
                new byte[TEAM_SIZE],
                false,
                true,
                0
        );
        dao.persist(result);
        return result;
    }

    public void setDao(MercenariesDao dao) {
        this.dao = dao;
    }
}

