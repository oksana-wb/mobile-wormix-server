package com.pragmatix.app.services;

import com.pragmatix.app.messages.server.ArenaResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.services.ExtraBattlesTimetableService;
import com.pragmatix.server.Server;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.08.2016 16:26
 */
@Service
public class ArenaService {

    @Resource
    private BattleService battleService;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private HeroicMissionService heroicMissionService;

    @Resource
    private ExtraBattlesTimetableService extraBattlesTimetableService;

    // время работы арены, по ставкам http://jira.pragmatix-corp.com/browse/WORMIX-4433
    private Map<BattleWager, Tuple2<LocalTime, LocalTime>> arenaWorkTime;

    public boolean isArenaLocked(BattleWager battleWager) {
        Tuple2<LocalTime, LocalTime> wagerWorkTime = arenaWorkTime.get(battleWager);
        if(wagerWorkTime == null) {
            return false;
        } else {
            LocalTime now = LocalTime.now();
            return now.isBefore(wagerWorkTime._1) || now.isAfter(wagerWorkTime._2);
        }
    }

    @Value("#{ArenaService_arenaWorkTime}")
    public void setArenaWorkTime(Map<BattleWager, String[]> arenaWorkTime) {
        this.arenaWorkTime = new HashMap<>();
        if(!arenaWorkTime.isEmpty())
            Server.sysLog.info("Ограничения на доступность Арены по ставкам:");
        for(Map.Entry<BattleWager, String[]> battleWagerEntry : arenaWorkTime.entrySet()) {
            BattleWager battleWager = battleWagerEntry.getKey();
            LocalTime from = LocalTime.parse(battleWagerEntry.getValue()[0]);
            LocalTime to = LocalTime.parse(battleWagerEntry.getValue()[1]);
            if(from.isAfter(to))
                throw new IllegalStateException(String.format("Не корректно указано время доступности ставки [%s] [%s - %s]", battleWager, from, to));
            Server.sysLog.info("  {}: {} - {}", battleWager, from, to);
            this.arenaWorkTime.put(battleWager, Tuple.of(from, to));
        }
    }

    public ArenaResult newArenaResult(UserProfile profile){
        // начисляем нужное количество битв если пришло время
        battleService.checkBattleCount(profile);

        ArenaResult arenaResult = new ArenaResult();
        arenaResult.battlesCount = profile.getBattlesCount();
        arenaResult.currentMission = profile.getCurrentMission();
        arenaResult.currentNewMission = profile.getCurrentNewMission();
        arenaResult.bossAvailable = battleService.isDisableValidateMission() || battleService.isBossTodayAvaliableFor(profile);
        arenaResult.superBossAvailable = battleService.isDisableValidateMission() || battleService.isSuperBossTodayAvaliableFor(profile);
        arenaResult.currentHeroicMissions = heroicMissionService.getHeroicMissionStates();

        if(profile.getBattlesCount() < 5) {
            long delay = battleService.getDelay(profile.getLevel());
            long curTime = System.currentTimeMillis();
            long lastTime = profile.getLastBattleTime();
            // говорим клиенту, что нужно подождать столько-то секунд
            arenaResult.delay = lastTime + delay - curTime;
            arenaResult.restoreBattlesDelay = delay;
        }

        arenaResult.restrictedWagers = new ArrayList<>();
        arenaResult.restrictedWagersLeftTime = new ArrayList<>();
        for(Map.Entry<BattleWager, Tuple2<LocalTime, LocalTime>> battleWagerTuple2Entry : arenaWorkTime.entrySet()) {
            BattleWager battleWager = battleWagerTuple2Entry.getKey();
            if(isArenaLocked(battleWager)){
                arenaResult.restrictedWagers.add(battleWager);
                LocalTime fromTime = battleWagerTuple2Entry.getValue()._1;
                if(LocalTime.now().isBefore(fromTime)){
                    // арена для ставки откроется сегодня
                    arenaResult.restrictedWagersLeftTime.add(fromTime.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toEpochSecond() - AppUtils.currentTimeSeconds());
                }else{
                    // арена для ставки откроется только завтра
                    arenaResult.restrictedWagersLeftTime.add(fromTime.atDate(LocalDate.now().plusDays(1)).atZone(ZoneId.systemDefault()).toEpochSecond() - AppUtils.currentTimeSeconds());
                }
            }
        }

        arenaResult.heroicMissionDailyProgresses = heroicMissionService.getHeroicMissionDailyProgresses();
        arenaResult.defeatContributionMoney = heroicMissionService.getDefeatContributionMoney();
        arenaResult.extraBattlesTimetable = extraBattlesTimetableService.printTimetableAsJson();
        arenaResult.wagerWinAwardToken = dailyRegistry.getWagerWinAwardToken(profile.getId());
        arenaResult.bossWinAwardToken = dailyRegistry.getBossWinAwardToken(profile.getId());

        return arenaResult;
    }

}
