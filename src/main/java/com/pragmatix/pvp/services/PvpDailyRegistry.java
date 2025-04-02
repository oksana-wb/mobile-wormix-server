package com.pragmatix.pvp.services;

import com.pragmatix.app.services.TrueSkillService;
import com.pragmatix.gameapp.services.DailyTaskAvailable;
import com.pragmatix.gameapp.services.IServiceTask;
import com.pragmatix.pvp.model.PvpUserDailyStructure;
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf;
import com.pragmatix.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр информации об игроках, имеющей актуальность в течении суток
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.01.12 16:48
 */
@Service
public class PvpDailyRegistry implements DailyTaskAvailable {

    private Map<Long, PvpUserDailyStructure> store = new ConcurrentHashMap<>();

    private boolean initialized = false;

    @Override
    public void init() {
        initialized = true;
    }

    // збрасываем информацию об игроках накопленную в течении дня
    private IServiceTask dailyTask = new IServiceTask() {
        @Override
        public void runServiceTask() {
            Server.sysLog.info("unique pvp users was: " + store.size());
            store = new ConcurrentHashMap<>();
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    };

    public IServiceTask getDailyTask() {
        return dailyTask;
    }

    public int getBattlesCount(Long userId, Long opponentId) {
        PvpUserDailyStructure dailyStructure = store.get(userId);
        return dailyStructure != null ? dailyStructure.getBattlesCountWith(opponentId) : 0;
    }

    public int getTrueSkillRating(Long userId, double mean, double standardDeviation) {
        PvpUserDailyStructure dailyStructure = getDailyStructure(userId);
        if(dailyStructure.trueSkillRating == 0) {
            dailyStructure.trueSkillRating = TrueSkillService.trueSkillRating(mean, standardDeviation);
        }
        return dailyStructure.trueSkillRating;
    }

    public void incBattlesCount(Long userId, Long opponentId) {
        PvpUserDailyStructure dailyStructure = getDailyStructure(userId);
        int resultBattleCount = dailyStructure.incBattlesCountWith(opponentId);
//        if(resultBattleCount > lobbyConf.getMaxBattlesWithSameUser()){
//            log.error(String.format("не сработало ограничение на количество боёв! %s <-(%s)-> %s", PvpService.formatPvpUserId(userId), resultBattleCount, PvpService.formatPvpUserId(opponentId)));
//        }
    }

    // запросить структуру по игроку, и создать её в случае отсутствия
    private PvpUserDailyStructure getDailyStructure(Long pvpUserId) {
        PvpUserDailyStructure dailyStructure = store.get(pvpUserId);
        if(dailyStructure == null) {
            dailyStructure = new PvpUserDailyStructure();
            store.put(pvpUserId, dailyStructure);
        }
        return dailyStructure;
    }

    /**
     * удалить информацию по игроку
     *
     * @param pvpUserId id игрока
     */
    public void clearFor(Long pvpUserId) {
        store.remove(pvpUserId);
    }

//====================== Getters and Setters =================================================================================================================================================


    public Map<Long, PvpUserDailyStructure> getStore() {
        return store;
    }

}
