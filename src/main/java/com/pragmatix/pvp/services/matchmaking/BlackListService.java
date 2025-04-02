package com.pragmatix.pvp.services.matchmaking;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.gameapp.services.DailyTaskAvailable;
import com.pragmatix.gameapp.services.IServiceTask;
import com.pragmatix.gameapp.services.persist.DefaultKeeperImpl;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf;
import com.pragmatix.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Класс сопровождает "Индивидуальный черный список" для игрока. В этот список попадают противники, которые проиграли все 3 боя за день.
 * И хранятся в нем expireDays дней
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.10.13 9:30
 */
@Service
public class BlackListService implements DailyTaskAvailable {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String keepFileName = "BlackListService.blackListForUser";

    @Resource
    private PersistenceService persistenceService;

    @Resource
    private LobbyConf lobbyConf;


    /**
     * <userId1, <userId2 который здался все 3 раза за день, время добавления в секундах>>
     */
    private Map<Long, Map<Long, Integer>> blackListsForUsers = new ConcurrentHashMap<Long, Map<Long, Integer>>();

    /**
     * <userId1, <противник, результат 3-х боёв>>
     */
    private final Map<Long, Map<Long, byte[]>> battlesResultsForUsers = new ConcurrentHashMap<Long, Map<Long, byte[]>>();

    /**
     * время нахождения в черном списке в днях
     */
    @Value("${BlackListService.expireDays:22}")
    private int expireDays = 22;

    private boolean initialized = false;

    public void init() {
        try {
            Map<Long, Map<Long, Integer>> map = persistenceService.restoreObjectFromFile(Map.class, keepFileName, new BlackListKeeper());
            if(map != null && map.size() > 0) {
                this.blackListsForUsers = map;
            }
        } catch (Exception e) {
            Server.sysLog.error(e.toString(), e);
        }
        initialized = true;
    }

    @Override
    public IServiceTask getDailyTask() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -expireDays);
        final long maxTrackedTime = cal.getTime().getTime();

        return new IServiceTask() {
            @Override
            public void runServiceTask() {
                // храним игроков в черном списке не более expireDays дней
                int n = 0;
                for(Map.Entry<Long, Map<Long, Integer>> mapEntry : blackListsForUsers.entrySet()) {
                    Long profileId = mapEntry.getKey();
                    Map<Long, Integer> blackList = mapEntry.getValue();
                    for(Map.Entry<Long, Integer> blackListEntry : blackList.entrySet()) {
                        if(blackListEntry.getValue() * 1000L < maxTrackedTime) {
                            blackList.remove(blackListEntry.getKey());
                        }
                    }
                    if(blackList.size() == 0) {
                        blackListsForUsers.remove(profileId);
                        n++;
                    }
                }
                Server.sysLog.info("Remove blackLists for {} profiles", n);

                // пополняем черные списки игроков по итогам дня
                n = 0;
                for(Map.Entry<Long, Map<Long, byte[]>> longMapEntry : battlesResultsForUsers.entrySet()) {
                    Long profileId = longMapEntry.getKey();
                    for(Map.Entry<Long, byte[]> longEntry : longMapEntry.getValue().entrySet()) {
                        Long opponentId = longEntry.getKey();
                        int lostCount = 0;
                        for(int i = 0; i < longEntry.getValue().length; i++) {
                            byte battleResult = longEntry.getValue()[i];
                            if(battleResult == PvpBattleResult.NOT_WINNER.getType()){
                                lostCount++;
                            }
                        }
                        if(lostCount >= lobbyConf.getMaxSurrenderedBattlesToSameUser()){
                            addOpponetToBlackListFor(profileId, opponentId);
                            n++;
                        }
                    }
                }
                Server.sysLog.info("Added {} ignored opponents", n);

                battlesResultsForUsers.clear();

                // сохраняем состояние на диск
                persistToDisk();
            }

            @Override
            public boolean isInitialized() {
                return initialized;
            }
        };
    }

    public boolean isInBlackList(Long userId, Long candidatId) {
        try {
            Map<Long, Integer> blackListForUser = this.blackListsForUsers.get(userId);
            return blackListForUser != null && blackListForUser.get(candidatId) != null;
        } catch (Exception e) {
            log.error(e.toString(), e);

            return false;
        }
    }

    public void registerBattleResult(Long userId, Long candidatId, byte pvpBattleResult) {
        Map<Long, byte[]> battlesResulsWithOpponent = battlesResultsForUsers.get(userId);
        if(battlesResulsWithOpponent == null){
            battlesResulsWithOpponent = new ConcurrentHashMap<>();
            battlesResultsForUsers.put(userId, battlesResulsWithOpponent);
        }
        byte[] battlesResuls = battlesResulsWithOpponent.get(candidatId);
        if(battlesResuls == null){
            battlesResuls = new byte[lobbyConf.getMaxSurrenderedBattlesToSameUser()];
            Arrays.fill(battlesResuls, (byte)-1);
            battlesResulsWithOpponent.put(candidatId, battlesResuls);
        }
        for(int i = 0; i < battlesResuls.length; i++) {
            if(battlesResuls[i] == -1){
                battlesResuls[i] = pvpBattleResult;
                break;
            }
        }
    }

    private void addOpponetToBlackListFor(Long profileId, Long opponentId) {
        Map<Long, Integer> blackList = blackListsForUsers.get(profileId);
        if(blackList == null){
            blackList = new ConcurrentHashMap<>();
            blackListsForUsers.put(profileId, blackList);
        }
        blackList.put(opponentId, (int)(System.currentTimeMillis() / 1000));
    }

    public void persistToDisk() {
        persistenceService.persistObjectToFile(blackListsForUsers, keepFileName, new BlackListKeeper());
    }

    class BlackListKeeper extends DefaultKeeperImpl {
        @Override
        public void writeObject(Object o) throws IOException {
            Map<Long, Map<Long, Integer>> blackListsForUsers = (Map<Long, Map<Long, Integer>>) o;
            Set<Long> inBlackListTotal = new HashSet<>();
            out.writeInt(blackListsForUsers.size());
            for(Map.Entry<Long, Map<Long, Integer>> longMapEntry : blackListsForUsers.entrySet()) {
                out.writeLong(longMapEntry.getKey());
                out.writeInt(longMapEntry.getValue().size());
                for(Map.Entry<Long, Integer> longIntegerEntry : longMapEntry.getValue().entrySet()) {
                    out.writeLong(longIntegerEntry.getKey());
                    out.writeInt(longIntegerEntry.getValue());
                    inBlackListTotal.add(longIntegerEntry.getKey());
                }
            }
            Server.sysLog.info("Persisted blackLists for {} profiles; total denied {} profiles", blackListsForUsers.size(), inBlackListTotal.size());
        }

        @Override
        public <T> T readObject(T objectClass) throws IOException, ClassNotFoundException {
            Set<Long> inBlackListTotal = new HashSet<>();
            int size = in.readInt();
            Map<Long, Map<Long, Integer>> blackListsForUsers = new ConcurrentHashMap<>(size);
            for(int i = 0; i < size; i++) {
                Long profileId = in.readLong();
                int size2 = in.readInt();
                Map<Long, Integer> blackList = new ConcurrentHashMap<>(size2);
                for(int j = 0; j < size2; j++) {
                    long opponentId = in.readLong();
                    blackList.put(opponentId, in.readInt());
                    inBlackListTotal.add(opponentId);
                }
                blackListsForUsers.put(profileId, blackList);
            }

            Server.sysLog.info("Restored blackLists for {} profiles; total denied {} profiles", blackListsForUsers.size(), inBlackListTotal.size());
            return (T) blackListsForUsers;
        }
    }

//====================== Getters and Setters =================================================================================================================================================


    public void setPersistenceService(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public void setLobbyConf(LobbyConf lobbyConf) {
        this.lobbyConf = lobbyConf;
    }

    public Map<Long, Map<Long, Integer>> getBlackListsForUsers() {
        return blackListsForUsers;
    }

    public Map<Long, Map<Long, byte[]>> getBattlesResultsForUsers() {
        return battlesResultsForUsers;
    }

    public int getExpireDays() {
        return expireDays;
    }

    public void setExpireDays(int expireDays) {
        this.expireDays = expireDays;
    }

}
