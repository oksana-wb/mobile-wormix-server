package com.pragmatix.app.services;

import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.model.ProfileDailyStructure;
import com.pragmatix.app.services.rating.RatingService;
import com.pragmatix.gameapp.GameApp;
import com.pragmatix.gameapp.services.DailyTaskAvailable;
import com.pragmatix.gameapp.services.IServiceTask;
import com.pragmatix.gameapp.threads.Execution;
import com.pragmatix.gameapp.threads.ExecutionContext;
import com.pragmatix.pvp.BattleWager;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр информации об игроках, имеющей актуальность в течении суток
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 20.01.12 16:48
 */
@Component
public class DailyRegistry implements DailyTaskAvailable {

    public final Map<Long, ProfileDailyStructure> store = new ConcurrentHashMap<>();

    @Resource
    private RatingService ratingService;

    @Resource
    private BattleService battleService;

    @Resource
    private GameApp gameApp;

    private boolean initialized = false;

    @Override
    public void init() {
        initialized = true;
    }

    // збрасываем информацию об игроках накопленную в течении дня
    private IServiceTask dailyTask = new IServiceTask() {
        @Override
        public void runServiceTask() {
            ExecutionContext context = new ExecutionContext(gameApp);
            Execution.EXECUTION.set(context);

            // выдаем награды за день, чистим последовательно store
            ratingService.dailyTask();

            // обнуляем прохождение боссов на диске
            battleService.persistToDisk();
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    };

    public IServiceTask getDailyTask() {
        return dailyTask;
    }

    public boolean isSuccessedMission(Long profileId) {
        return false;
    }

    public void setSuccessedMission(Long profileId) {
    }

    public boolean isSuccessedSuperBossMission(Long profileId) {
        // возвращаем ноль если не нашли структуру по profileId
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure != null && dailyStructure.isSuccessedSuperBossMission();
    }

    public void setSuccessedSuperBosMission(Long profileId) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setSuccessedSuperBossMission(true);
    }

    public boolean isMakePeyment(Long profileId) {
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure != null && dailyStructure.isMakePayment();
    }

    public void makePayment(Long profileId) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setMakePayment(true);
    }

    public boolean isReceivedSpecialDeal(Long profileId) {
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure != null && dailyStructure.isReceivedSpecialDeal();
    }

    public void receivedSpecialDeal(Long profileId) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setReceivedSpecialDeal(true);
    }

    public void clearMission(Long profileId) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setSuccessedSuperBossMission(false);
    }

    public byte getHowManyPumped(Long profileId) {
        // возвращаем ноль если не нашли структуру по profileId
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure != null ? dailyStructure.getHowManyPumped() : 0;
    }

    public void setHowManyPumped(Long profileId, byte howManyPumped) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setHowManyPumped(howManyPumped);
    }

    public int getDailyRating(Long profileId, BattleWager battleWager) {
        // возвращаем ноль если не нашли структуру по profileId
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure != null ? dailyStructure.getDailyRating(battleWager) : 0;
    }

    public int getMinDailyRating(Long profileId) {
        // возвращаем ноль если не нашли структуру по profileId
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure != null && dailyStructure.getDailyRatings() != null ? min(dailyStructure.getDailyRatings()) : 0;
    }

    public static int min(int[] values) {
        int min = Integer.MAX_VALUE;
        for(int value : values) {
            if(value < min)
                min = value;
        }
        return min;
    }

    public int[] getDailyRatings(Long profileId) {
        // возвращаем ноль если не нашли структуру по profileId
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure != null ? dailyStructure.getDailyRatings() : new int[0];
    }

    public void setDailyRating(Long profileId, int dailyRating, BattleWager battleWager) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setDailyRating(dailyRating, battleWager);
    }

    public void setSearchKeys(Long profileId, byte searchKeys) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setSearchKeys(searchKeys);
    }

    public byte getSearchKeys(Long profileId) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        return dailyStructure.getSearchKeys();
    }

    public void setPrevHat(Long profileId, Short stuffId) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setPrevHat(stuffId);
    }

    public short getPrevHat(Long profileId) {
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure != null ? dailyStructure.getPrevHat() : 0;
    }

    public void setPrevKit(Long profileId, Short stuffId) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setPrevKit(stuffId);
    }

    public short getPrevKit(Long profileId) {
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure != null ? dailyStructure.getPrevKit() : 0;
    }

    public void cleanOfflineAward(Long profileId) {
        ProfileDailyStructure dailyStructure = store.get(profileId);
        if(dailyStructure != null) {
            dailyStructure.setOfflineAwards(null);
        }
    }

    public void addOfflineAward(Long profileId, LoginAwardStructure loginAwardStructure) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        LoginAwardStructure[] offlineAwards = dailyStructure.getOfflineAwards();
        if(offlineAwards == null) {
            dailyStructure.setOfflineAwards(new LoginAwardStructure[]{loginAwardStructure});
        } else {
            dailyStructure.setOfflineAwards(ArrayUtils.add(offlineAwards, loginAwardStructure));
        }
    }

    public LoginAwardStructure[] getOfflineAwards(Long profileId) {
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure != null ? dailyStructure.getOfflineAwards() : null;
    }

    public void setMercenariesBattleSeries(Long profileId, int mercenariesBattleCount) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setMercenariesBattleSeries((byte) mercenariesBattleCount);
    }

    public void incMercenariesBattleSeries(Long profileId) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setMercenariesBattleSeries((byte) (dailyStructure.getMercenariesBattleSeries() + 1));
    }

    public short getMercenariesBattleSeries(Long profileId) {
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure != null ? dailyStructure.getMercenariesBattleSeries() : 0;
    }

    public boolean isDividendPaid(Long profileId) {
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure != null && dailyStructure.isDividendPaid();
    }

    public void setDividendPaid(Long profileId) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setDividendPaid(true);
    }

    public void setFlags(Long profileId, byte flags) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setFlags(flags);
    }

    public int getWagerWinAwardToken(Long profileId) {
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure == null ? ProfileDailyStructure.WagerWinAwardTokenDefault : dailyStructure.getWagerWinAwardToken();
    }

    public void addWagerWinAwardToken(Long profileId, int wagerWinAwardToken) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setWagerWinAwardToken(Math.max(0, dailyStructure.getWagerWinAwardToken() + wagerWinAwardToken));
    }

    public void setWagerWinAwardToken(Long profileId, int wagerWinAwardToken) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setWagerWinAwardToken(wagerWinAwardToken);
    }

    public int getBossWinAwardToken(Long profileId) {
        ProfileDailyStructure dailyStructure = store.get(profileId);
        return dailyStructure == null ? ProfileDailyStructure.BossWinAwardTokenDefault : dailyStructure.getBossWinAwardToken();
    }

    public void addBossWinAwardToken(Long profileId, int bossWinAwardToken) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setBossWinAwardToken(Math.max(0, dailyStructure.getBossWinAwardToken() + bossWinAwardToken));
    }

    public void setBossWinAwardToken(Long profileId, int bossWinAwardToken) {
        ProfileDailyStructure dailyStructure = getDailyStructure(profileId);
        dailyStructure.setBossWinAwardToken(bossWinAwardToken);
    }

    // запросить структуру по игроку, и создать её в случае отсутствия
    private ProfileDailyStructure getDailyStructure(Long profileId) {
        ProfileDailyStructure dailyStructure = store.get(profileId);
        if(dailyStructure == null) {
            dailyStructure = new ProfileDailyStructure();
            store.put(profileId, dailyStructure);
        }
        return dailyStructure;
    }

    public Set<Map.Entry<Long, ProfileDailyStructure>> getStore() {
        return store.entrySet();
    }

    /**
     * удалить информацию по игроку
     *
     * @param profileId id игрока
     */
    public void clearFor(long profileId) {
        store.remove(profileId);
    }
}
