package com.pragmatix.arena.coliseum;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.common.ItemType;
import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.common.Race;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileBonusService;
import com.pragmatix.app.services.ProfileEventsService;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum;
import com.pragmatix.app.services.StatisticService;
import com.pragmatix.app.services.rating.SeasonService;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.services.ClanService;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.END_GLADIATOR_BATTLE;

public class ColiseumService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    ColiseumDao dao;

    @Resource
    StatisticService statisticService;

    @Resource
    ProfileEventsService profileEventsService;

    @Resource
    ProfileBonusService profileBonusService;

    @Resource
    ClanService clanService;

    @Resource
    Optional<SeasonService> seasonService;

    @Resource(name = "ColiseumService.hats")
    private List<Short> hats;

    @Resource(name = "ColiseumService.kits")
    private List<Short> kits;

    @Resource(name = "ColiseumService.rewardMap")
    private Map<String, List<ColiseumRewardItem>> rewardMap;

    @Value("${Coliseum.openFrom:9}")
    private int openFrom = 9;

    @Value("${Coliseum.openTo:24}")
    private int openTo = 24;

    @Value("${ColiseumService.debugMode:false}")
    private boolean debugMode = false;

    @Value("${ColiseumService.gladiatorTeamMemberLevel:20}")
    private byte gladiatorTeamMemberLevel = 20;

    private List<Integer> races = Arrays.stream(Race.values()).map(Race::getType).collect(Collectors.toList());

    private Set<Integer> topRaces = Stream.of(Race.ALIEN, Race.DRAGON, Race.CAT, Race.RHINO).map(Race::getType).collect(Collectors.toSet());

    private List<Integer> racesMinusTop = races.stream().filter(race -> !topRaces.contains(race)).collect(Collectors.toList());

    private int buyColiseumTicketRubyPrice = 10;

    private int completeSeriesWinCondition = 10;

    private int completeSeriesDefeatCondition = 3;

    public ColiseumEntity coliseumEntity(final UserProfile profile) {
        if(profile.getColiseumEntity() == null) {
            synchronized (profile) {
                if(profile.getColiseumEntity() == null) {
                    int profileId = profile.getId().intValue();
                    ColiseumEntity coliseumEntity = dao.find(profileId);
                    if(coliseumEntity == null)
                        coliseumEntity = newColiseumEntity(profileId);
                    profile.setColiseumEntity(coliseumEntity);
                }
            }
        }
        return profile.getColiseumEntity();
    }

    public void wipeColiseumState(UserProfile profile) {
        if(profile.getColiseumEntity() != null) {
            dao.delete(profile.getId().intValue());
            profile.setColiseumEntity(null);
        }
    }

    public boolean isColiseumOpen() {
        int hourOfDay_ = hourOfDay();
        return hourOfDay_ >= openFrom && hourOfDay_ < openTo;
    }

    private int hourOfDay() {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    }

    public Pair<Integer, Integer> openTime() {
        int cuttedTime = (int) ((System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hourOfDay())) / 1000L / 60L / 60L);
        if(hourOfDay() >= openTo) // если игрок пришел после закрытия арены, время открытия арены будет уже на след. день
            return new ImmutablePair<>((int) (cuttedTime * 60L * 60L + TimeUnit.HOURS.toSeconds(openFrom + 24)), (int) (cuttedTime * 60L * 60L + TimeUnit.HOURS.toSeconds(openTo)));
        else
            return new ImmutablePair<>((int) (cuttedTime * 60L * 60L + TimeUnit.HOURS.toSeconds(openFrom)), (int) (cuttedTime * 60L * 60L + TimeUnit.HOURS.toSeconds(openTo)));
    }

    public boolean isSeriesCompleted(ColiseumEntity entity) {
        return entity.win == completeSeriesWinCondition || entity.defeat == completeSeriesDefeatCondition;
    }

    public boolean isSeriesInProgress(ColiseumEntity entity) {
        return entity.win < completeSeriesWinCondition && entity.defeat < completeSeriesDefeatCondition && entity.open && entity.isTeamFull();
    }

    public void consumeBattleResult(UserProfile profile, PvpBattleResult battleResult, byte[] collectedReagents, String battleTime) {
        ColiseumEntity entity = coliseumEntity(profile);
        if(isSeriesInProgress(entity)) {
            applyBattleResult(entity, battleResult, profile);
            profileEventsService.fireProfileEventAsync(END_GLADIATOR_BATTLE, profile,
                    Param.result, battleResult,
                    Param.battleTime, battleTime,
                    Param.reagents, ReagentsEntity.getReagentValues(collectedReagents)
            );
        } else {
            log.error("[{}] получен результат гладиаторского боя но серия уже завершена! {} battleResult={}", profile, entity, battleResult);
        }
    }

    public void postToClanChat(ColiseumEntity entity, UserProfile profile) {
        clanService.postToChat(profile.getSocialId(), profile.getId().intValue(), Messages.FINISH_GLADIATOR_SERIES_CHAT_ACTION, "" + entity.win + "/" + entity.defeat);
    }

    private void applyBattleResult(ColiseumEntity entity, PvpBattleResult battleResult, UserProfile profile) {
        if(battleResult == PvpBattleResult.WINNER) {
            entity.win = !debugMode ? (byte) (entity.win + 1) : (byte) completeSeriesWinCondition;
        } else if(battleResult == PvpBattleResult.NOT_WINNER) {
            entity.defeat = !debugMode ? (byte) (entity.defeat + 1) : (byte) completeSeriesDefeatCondition;
        } else {
            entity.draw = (byte) (entity.draw + 1);
        }
        if(entity.win + entity.defeat + entity.draw == 1) entity.startSeries = AppUtils.currentTimeSeconds();
        if(isSeriesCompleted(entity)) postToClanChat(entity, profile);
        entity.touch();
    }

    public GenericAwardStructure[] figureOutReward(UserProfile profile, ColiseumEntity entity) {
        List<ColiseumRewardItem> items;
        if(entity.win == completeSeriesWinCondition && entity.defeat == 0 && entity.draw == 0)
            items = rewardMap.get("10_0"); // чистая победа
        else
            items = rewardMap.get("" + entity.win);
        GenericAward.Builder builder = GenericAward.builder();
        for(ColiseumRewardItem item : items) {
            mapItemToGenericAward(builder, item);
        }
        builder.addExperience(10 * (entity.win + entity.defeat + entity.draw));
        GenericAward reward = builder.build();
        List<GenericAwardStructure> resultList = profileBonusService.awardProfile(reward, profile, AwardTypeEnum.GLADIATOR_BATTLE,
                String.format("%s/%s/%s", entity.win, entity.defeat, entity.draw));
        return resultList.toArray(new GenericAwardStructure[resultList.size()]);
    }

    public GenericAward.Builder mapItemToGenericAward(GenericAward.Builder builder, ColiseumRewardItem item) {
        if(item.weaponId > 0) {
            builder.addWeapon(item.weaponId, getValue(item, item.weaponMin, item.weaponMax));
        } else if(item.weaponMin > 0) {
            seasonService.map(SeasonService::getCurrentSeasonWeaponsArr)
                    .ifPresent(weapons -> builder.addWeapon(weapons[getValue(item, 0, weapons.length - 1)], getValue(item, item.weaponMin, item.weaponMax)));
        }
        if(item.seasonStuff) {
            seasonService.map(SeasonService::getCurrentSeasonStuffArr)
                    .ifPresent(stuff -> builder.addSeasonStuff(stuff[getValue(item, 0, stuff.length - 1)]));
        }
        if(item.reactionMin > 0) builder.addReactionRate(getValue(item, item.reactionMin, item.reactionMax));
        if(item.fuzeMin > 0) builder.addMoney(getValue(item, item.fuzeMin, item.fuzeMax));
        if(item.rubyMin > 0) builder.addRealMoney(getValue(item, item.rubyMin, item.rubyMax));
        if(item.medalsMin > 0) builder.addReagent(ColiseumRewardItem.MEDAL_REAGENT_ID, getValue(item, item.medalsMin, item.medalsMax));
        if(item.randomRewardCount > 0)
            for(int i = 1; i <= item.randomRewardCount; i++) {
                int index = getValue(item, 0, item.randomReward.length - 1);
                mapItemToGenericAward(builder, item.randomReward[index]);
            }
        return builder;
    }

    private int getValue(ColiseumRewardItem item, int from, int to) {
        if(from == to)
            if(from > 0)
                return from;
            else
                throw new IllegalArgumentException("'from' and 'to' both is zero in " + item);
        else if(from > to)
            throw new IllegalArgumentException("from > to in " + item);
        else
            return from + new Random().nextInt(to - from + 1);
    }

    public GenericAwardStructure[] getReward(UserProfile profile) {
        ColiseumEntity entity = coliseumEntity(profile);
        if(isSeriesCompleted(entity)) {
            GenericAwardStructure[] result = figureOutReward(profile, entity);
            closeSeries(entity);
            return result;
        } else {
            return new GenericAwardStructure[0];
        }
    }

    public ColiseumEntity interruptSeries(UserProfile profile) {
        ColiseumEntity entity = coliseumEntity(profile);
        if(isSeriesInProgress(entity)) {
            entity.defeat = (byte) completeSeriesDefeatCondition;
            postToClanChat(entity, profile);
            entity.touch();
        }
        return entity;
    }

    public void persistEntity(ColiseumEntity entity) {
        if(entity != null)
            dao.persist(entity, true);
    }

    public Pair<ColiseumEntity, ColiseumErrorEnum> addTeamMember(UserProfile profile, int teamMemberIndex) {
        try {
            ColiseumEntity entity = coliseumEntity(profile);
            if(!entity.open) {
                return new ImmutablePair<>(null, ColiseumErrorEnum.HAS_NO_TICKET);
            } else if(entity.isTeamFull()) {
                return new ImmutablePair<>(null, ColiseumErrorEnum.TEAM_IS_FULL);
            } else if(teamMemberIndex < 0 || teamMemberIndex > 2) {
                return new ImmutablePair<>(null, ColiseumErrorEnum.INVALID_TEAM_MEMBER_INDEX);
            } else {
                GladiatorTeamMemberStructure memberByIndex = entity.candidats[teamMemberIndex];
                for(int i = 0; i < entity.team.length; i++) {
                    GladiatorTeamMemberStructure member = entity.team[i];
                    if(member == null) {
                        entity.team[i] = memberByIndex;
                        break;
                    }
                }
                entity.candidats = entity.isTeamFull() ? null : initCandidats(entity.team);
                entity.touch();
                return new ImmutablePair<>(entity, null);
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
            return new ImmutablePair<>(null, ColiseumErrorEnum.ERROR);
        }
    }

    public Pair<ColiseumEntity, ColiseumErrorEnum> buyColiseumTicket(UserProfile profile) {
        ColiseumEntity entity = coliseumEntity(profile);
        if(entity.open) {
            return new ImmutablePair<>(null, ColiseumErrorEnum.ALREDY_OPEN);
        } else if(profile.getRealMoney() < buyColiseumTicketRubyPrice) {
            return new ImmutablePair<>(null, ColiseumErrorEnum.NO_ENOUGH_MONEY);
        } else {
            profile.setRealMoney(profile.getRealMoney() - buyColiseumTicketRubyPrice);

            profileEventsService.fireProfileEventAsync(ProfileEventEnum.PURCHASE, profile,
                    Param.eventType, ItemType.COLISEUM_TICKET,
                    Param.realMoney, -buyColiseumTicketRubyPrice
            );
            statisticService.buyItemStatistic(profile.getId(), 0, buyColiseumTicketRubyPrice, ItemType.COLISEUM_TICKET, 1, 0, profile.getLevel());

            startSeries(entity);
            persistEntity(entity);
            return new ImmutablePair<>(entity, null);
        }
    }

    public WormStructure[] wormsGroup(UserProfile profile) {
        List<WormStructure> result = new ArrayList<>();
        ColiseumEntity coliseumEntity = coliseumEntity(profile);
        for(GladiatorTeamMemberStructure member : coliseumEntity.team) {
            if(member != null)
                result.add(new WormStructure(member, gladiatorTeamMemberLevel));
        }
        return result.toArray(new WormStructure[0]);
    }

    private ColiseumEntity startSeries(ColiseumEntity entity) {
        entity.open = true;
        entity.win = 0;
        entity.defeat = 0;
        entity.draw = 0;
        entity.num += 1;
        entity.team = initTeam();
        entity.candidats = initCandidats(entity.team);

        entity.touch();

        return entity;
    }

    private ColiseumEntity closeSeries(ColiseumEntity entity) {
        entity.open = false;
        entity.win = 0;
        entity.defeat = 0;
        entity.draw = 0;
        entity.candidats = null;
        entity.team = initTeam();
        entity.touch();
        return entity;
    }

    public GladiatorTeamMemberStructure[] removeNulls(GladiatorTeamMemberStructure[] team) {
        List<GladiatorTeamMemberStructure> result = new ArrayList<>();
        for(GladiatorTeamMemberStructure member : team) {
            if(member != null)
                result.add(member);
        }
        return result.toArray(new GladiatorTeamMemberStructure[result.size()]);
    }

    private ColiseumEntity newColiseumEntity(int profileId) {
        ColiseumEntity result = new ColiseumEntity(
                profileId,
                true,
                1,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                initCandidats(initTeam()),
                initTeam(),
                false,
                true,
                0
        );
        dao.persist(result);
        return result;
    }

    private GladiatorTeamMemberStructure[] initCandidats(GladiatorTeamMemberStructure[] team) {
        boolean hasTopRace = false;
        for(GladiatorTeamMemberStructure member : team) {
            if(member != null && isTopRace(member.race)) {
                hasTopRace = true;
                break;
            }
        }
        GladiatorTeamMemberStructure first = newTeamMemberCandidate(hasTopRace);
        hasTopRace |= isTopRace(first.race);
        GladiatorTeamMemberStructure second = newTeamMemberCandidate(hasTopRace);
        hasTopRace |= isTopRace(second.race);
        GladiatorTeamMemberStructure third = newTeamMemberCandidate(hasTopRace);
        hasTopRace |= isTopRace(third.race);
        if(team[2] != null && !hasTopRace)
            third.race = new Random().nextBoolean() ? Race.DRAGON.getByteType() : Race.CAT.getByteType();
        return new GladiatorTeamMemberStructure[]{first, second, third};
    }

    private boolean isTopRace(byte race) {
        return topRaces.contains((int) race);
    }

    private GladiatorTeamMemberStructure[] initTeam() {
        return new GladiatorTeamMemberStructure[]{null, null, null, null};
    }

    private GladiatorTeamMemberStructure newTeamMemberCandidate(boolean hasDragonOrCat) {
        GladiatorTeamMemberStructure result = new GladiatorTeamMemberStructure();
        List<Integer> validRaces = !hasDragonOrCat ? races : racesMinusTop;
        Random random = new Random();
        result.race = validRaces.get(random.nextInt(validRaces.size())).byteValue();
        result.attack = (byte) (random.nextInt(gladiatorTeamMemberLevel * 2 + 1));
        result.armor = (byte) (gladiatorTeamMemberLevel * 2 - result.attack);
        result.hat = hats.get(random.nextInt(hats.size()));
        result.kit = kits.get(random.nextInt(kits.size()));
        return result;
    }

    public void setHats(List<Short> hats) {
        this.hats = hats;
    }

    public void setKits(List<Short> kits) {
        this.kits = kits;
    }

    public ColiseumDao getDao() {
        return dao;
    }

    public void setDao(ColiseumDao dao) {
        this.dao = dao;
    }

    public int getGladiatorTeamMemberLevel() {
        return gladiatorTeamMemberLevel;
    }

}

