package com.pragmatix.app.services.persist;

import com.pragmatix.gameapp.services.persist.DefaultKeeperImpl;
import com.pragmatix.server.Server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.05.11 14:50
 */
public class RatingMapKeeper extends DefaultKeeperImpl {

    private String ratingType;

    public RatingMapKeeper(String ratingType) {
        this.ratingType = ratingType;
    }

    @Override
    public void writeObject(Object o) throws IOException {
        Map<Long, Integer> map = (Map<Long, Integer>) o;
        out.writeInt(map.size());
        for(Map.Entry<Long, Integer> entry : map.entrySet()) {
            out.writeLong(entry.getKey());
            out.writeInt(entry.getValue());
        }
        Server.sysLog.info("Persisted " + ratingType + " rating for {} profiles", map.size());
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
        Server.sysLog.info("Loaded " + ratingType + " rating from file for {} profiles", map.size());
        return (T) map;
    }

}
