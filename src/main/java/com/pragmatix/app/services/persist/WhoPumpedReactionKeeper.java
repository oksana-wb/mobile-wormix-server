package com.pragmatix.app.services.persist;

import com.pragmatix.gameapp.services.persist.DefaultKeeperImpl;
import com.pragmatix.server.Server;
import org.apache.mina.util.ConcurrentHashSet;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.05.11 12:25
 */
public class WhoPumpedReactionKeeper extends DefaultKeeperImpl {

    private int forDate;

    public WhoPumpedReactionKeeper(int forDate) {
        this.forDate = forDate;
    }

    @Override
    public void writeObject(Object o) throws IOException, ClassCastException {
        Map<Long, Object> whoPumpedRateByDay = (Map<Long, Object>) o;
        out.writeInt(whoPumpedRateByDay.size());
        for(Map.Entry<Long, Object> entry : whoPumpedRateByDay.entrySet()) {
            out.writeLong(entry.getKey());
            if(entry.getValue() instanceof Set) {
                Set<Long> set = (Set<Long>) entry.getValue();
                out.writeInt(set.size());
                for(Long id : set) {
                    out.writeLong(id);
                }
            } else if(entry.getValue() instanceof int[]) {
                int[] arr = (int[]) entry.getValue();
                out.writeInt(arr.length);
                for(int id : arr) {
                    out.writeLong(id);
                }
            } else {
                Server.sysLog.error("unexpexted map's value class [{}]", entry.getValue());
                out.writeInt(0);
            }
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, forDate);
        String s = String.format("%tF: pumped rate for %s gamers", cal, whoPumpedRateByDay.size());
        Server.sysLog.info(s);
    }

    @Override
    public <T> T readObject(T objectClass) throws IOException, ClassNotFoundException, ClassCastException {
        Map<Long, Object> map;
        if(forDate == 0) {
            // данные за сегодня размещяем в конкурентных структурах
            map = new ConcurrentHashMap<Long, Object>();
        } else {
            map = new HashMap<Long, Object>();
        }
        int mapSize = in.readInt();
        for(int j = 0; j < mapSize; j++) {
            long keyId = in.readLong();
            int setSize = in.readInt();
            if(forDate == 0) {
                // данные за сегодня размещяем в конкурентных структурах
                Set<Long> set = new ConcurrentHashSet<Long>();
                map.put(keyId, set);
                for(int k = 0; k < setSize; k++) {
                    long id = in.readLong();
                    set.add(id);
                }
            } else {
                int[] set = new int[setSize];
                map.put(keyId, set);
                for(int k = 0; k < setSize; k++) {
                    long id = in.readLong();
                    set[k] = (int) id;
                }
            }
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, forDate);
        String s = String.format("%tF: pumped rate for %s gamers", cal, map.size());
        Server.sysLog.info(s);
        return (T) map;
    }

}
