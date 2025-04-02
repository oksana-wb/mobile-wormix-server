package com.pragmatix.app.messages.server;

import com.pragmatix.app.settings.HeroicMissionDailyProgress;
import com.pragmatix.app.settings.HeroicMissionState;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;
import org.apache.commons.collections.CollectionUtils;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ответ от сервера на команду GetArena
 * вернет список профайлов для арены
 * <p>
 * User: denis
 * Date: 03.12.2009
 * Time: 1:51:46
 */
@Command(10004)
public class ArenaResult {
    /**
     * количество доступных боёв
     */
    public int battlesCount;
    /**
     * id последней пройденной миссии
     */
    public short currentMission;
    /**
     * id последней пройденной новой миссии
     */
    public short currentNewMission;
    /**
     * доступны ли игроку миссии с боссами
     */
    public boolean bossAvailable;
    /**
     * доступны ли игроку миссии с супер боссами
     */
    public boolean superBossAvailable;

    public HeroicMissionState[] currentHeroicMissions;

    @Resize(TypeSize.UINT32)
    public long delay;

    @Resize(TypeSize.UINT32)
    public long restoreBattlesDelay;

    // если в данный момент ставка не доступна
    public List<BattleWager> restrictedWagers;

    // время когда ставка станет доступна (в секундах)
    @Resize(TypeSize.UINT32)
    public List<Long> restrictedWagersLeftTime;

    public HeroicMissionDailyProgress[] heroicMissionDailyProgresses;

    public int defeatContributionMoney;

    /* JSON MAP
         {
            "ROPE_RACE":"2016-04-01,2016-04-30",
            "DUEL_20":"2016-04-01,2016-04-30",
            "MERCENARIES_DUEL":"2016-04-01,2016-04-30"
         }
     */
    public String extraBattlesTimetable;

    public int wagerWinAwardToken;

    public int bossWinAwardToken;

    public ArenaResult() {
    }

    @Override
    public String toString() {
        Map<BattleWager, LocalTime> restrictedWagersMap = new LinkedHashMap<>();
        for(int i = 0; i < restrictedWagers.size(); i++) {
            BattleWager battleWager = restrictedWagers.get(i);
            restrictedWagersMap.put(battleWager, LocalTime.ofSecondOfDay(restrictedWagersLeftTime.get(i)));
        }
        return "ArenaResult{" +
                "battlesCount=" + battlesCount +
                ", currentMission=" + currentMission +
                ", currentNewMission=" + currentNewMission +
                ", bossAvailable=" + bossAvailable +
                ", superBossAvailable=" + superBossAvailable +
                ", wagerWinAwardToken=" + wagerWinAwardToken +
                ", bossWinAwardToken=" + bossWinAwardToken +
                ", delay=" + delay +
                ", restoreBattlesDelay=" + restoreBattlesDelay +
                (!restrictedWagersMap.isEmpty() ? ", restrictedWagersLeftTime=" + restrictedWagersMap  : "") +
                ", currentHeroicMissions=" + Arrays.toString(currentHeroicMissions) +
                ", heroicMissionDailyProgresses=" + Arrays.toString(heroicMissionDailyProgresses) +
                ", defeatContributionMoney=" + defeatContributionMoney +
                ", extraBattlesTimetable=" + extraBattlesTimetable +
                '}';
    }

}
