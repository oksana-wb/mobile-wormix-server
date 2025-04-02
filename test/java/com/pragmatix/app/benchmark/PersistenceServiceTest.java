package com.pragmatix.app.benchmark;

import com.pragmatix.app.services.persist.WhoPumpedReactionByDaysKeeper;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.testcase.AbstractTest;
import org.apache.mina.util.ConcurrentHashSet;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static junit.framework.Assert.assertEquals;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.05.11 16:21
 */
public class PersistenceServiceTest extends AbstractTest {

    PersistenceService persistenceService = new PersistenceService();
    private int mapSize = 1000;
    private int setSize = 100;

    @Before
    public void init() {
        persistenceService.init();
    }

    @Test
    public void testWrite() {
        System.out.println("prepare ...");
        List<Map<Long, Set<Long>>> whoPumpedRateByDays = new CopyOnWriteArrayList<Map<Long, Set<Long>>>();

        Map<Long, Set<Long>> map = new ConcurrentHashMap<Long, Set<Long>>();
        for(int i = 0; i < mapSize; i++) {
            ConcurrentHashSet<Long> set = new ConcurrentHashSet<Long>();
            for(int j = 0; j < setSize; j++) {
                set.add(AppUtils.generateRandomLong(Long.MAX_VALUE));
            }
            map.put(AppUtils.generateRandomLong(Long.MAX_VALUE), new ConcurrentHashSet<Long>(set));
        }
        whoPumpedRateByDays.add(new ConcurrentHashMap<Long, Set<Long>>(map));
        whoPumpedRateByDays.add(new ConcurrentHashMap<Long, Set<Long>>(map));
        whoPumpedRateByDays.add(new ConcurrentHashMap<Long, Set<Long>>(map));

        persistenceService.persistObjectToFile(whoPumpedRateByDays, "whoPumpedRateByDays", new WhoPumpedReactionByDaysKeeper());
//        persistenceService.persistObjectToFile(whoPumpedRateByDays, "whoPumpedRateByDays.default");
    }

    @Test
    public void testRead() {
        List<Map<Long, Set<Long>>> list = persistenceService.restoreObjectFromFile(List.class, "whoPumpedRateByDays", new WhoPumpedReactionByDaysKeeper());
        assertEquals(3, list.size());
        assertEquals(mapSize, list.get(2).size());
        assertEquals(setSize, list.get(2).values().iterator().next().size());
    }

}
