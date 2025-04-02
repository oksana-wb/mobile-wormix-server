package com.pragmatix.app.services.persist;

import com.pragmatix.app.model.ProfileDailyStructure;
import com.pragmatix.gameapp.services.persist.DefaultKeeperImpl;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.server.Server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.05.11 14:50
 */
public class DailyRatingMapKeeper extends DefaultKeeperImpl {

    @Override
    public void writeObject(Object o) throws IOException {
        Object[] values = (Object[]) o;
        Set<Map.Entry<Long, ProfileDailyStructure>> entrySet = (Set<Map.Entry<Long, ProfileDailyStructure>>) values[0];
        BattleWager battleWager = (BattleWager) values[1];

        int count = 0;
        // сначало считаем элементы с ненулевым рейтингом
        for(Map.Entry<Long, ProfileDailyStructure> entry : entrySet) {
            int dailyRating = entry.getValue().getDailyRating(battleWager);
            if(dailyRating != 0) {
                count++;
            }
        }

        // пишем длину
        out.writeInt(count);

        // пишем рейтинги
        for(Map.Entry<Long, ProfileDailyStructure> entry : entrySet) {
            int dailyRating = entry.getValue().getDailyRating(battleWager);
            if(dailyRating != 0) {
                out.writeLong(entry.getKey());
                out.writeInt(dailyRating);
            }
        }
        Server.sysLog.info("{}: Persisted daily rating for {} profiles", battleWager, count);
    }

    @Override
    public <T> T readObject(T objectClass) throws IOException, ClassNotFoundException {
        Map<Long, Integer> map = new ConcurrentHashMap<Long, Integer>();
        int size = in.readInt();
        for(int i = 0; i < size; i++) {
            long key = in.readLong();
            int value = in.readInt();
            map.put(key, value);
        }
        Server.sysLog.info("Loaded daily rating from file for {} profiles", map.size());
        return (T) map;
    }

}
