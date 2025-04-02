package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.domain.ClanDailyStructure;
import com.pragmatix.gameapp.services.DailyTaskAvailable;
import com.pragmatix.gameapp.services.IServiceTask;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.07.13 17:55
 */
@Service
public class ClanDailyRegistry implements DailyTaskAvailable {

    private Map<Long, ClanDailyStructure> store = new ConcurrentHashMap<>();

    private boolean initialized = false;

    @Override
    public void init() {
        initialized = true;
    }

    @Override
    public IServiceTask getDailyTask() {
        return new IServiceTask() {
            @Override
            public void runServiceTask() {
                store = new ConcurrentHashMap<>();
            }

            @Override
            public boolean isInitialized() {
                return initialized;
            }
        };
    }

    public int getExpelCount(Long clanMemberId) {
        ClanDailyStructure dailyStructure = getDailyStructureOrCreate(clanMemberId);
        return dailyStructure.getExpelCount();
    }

    public int incExpelCount(Long clanMemberId) {
        ClanDailyStructure dailyStructure = getDailyStructureOrCreate(clanMemberId);
        return dailyStructure.incExpelCount();
    }

    // запросить структуру по игроку, и создать её в случае отсутствия
    private ClanDailyStructure getDailyStructureOrCreate(Long clanMemberId) {
        ClanDailyStructure dailyStructure = store.get(clanMemberId);
        if(dailyStructure == null) {
            dailyStructure = new ClanDailyStructure();
            store.put(clanMemberId, dailyStructure);
        }
        return dailyStructure;
    }

    public void clearFor(long clanMemberId) {
        store.remove(clanMemberId);
    }

    public Map<Long, ClanDailyStructure> getStore() {
        return store;
    }

}
