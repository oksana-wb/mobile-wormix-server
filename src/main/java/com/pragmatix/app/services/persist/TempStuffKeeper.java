package com.pragmatix.app.services.persist;

import com.pragmatix.gameapp.services.persist.DefaultKeeperImpl;
import com.pragmatix.server.Server;
import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Сохраняет и восстанавоивает выданные временные шапки
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.05.11 12:25
 */
public class TempStuffKeeper extends DefaultKeeperImpl {

    @Override
    public void writeObject(Object o) throws IOException, ClassCastException {
        Map<Long, Set<Short>> profileIdTempStuffMap = (Map<Long, Set<Short>>) o;
        out.writeInt(profileIdTempStuffMap.size());
        for(Map.Entry<Long, Set<Short>> entry : profileIdTempStuffMap.entrySet()) {
            out.writeLong(entry.getKey());
            out.writeInt(entry.getValue().size());
            for(Short stuffId : entry.getValue()) {
                out.writeShort(stuffId);
            }
        }
        Server.sysLog.info("store the issuance of a temporary hat for {} profiles", profileIdTempStuffMap.size());
    }

    @Override
    public <T> T readObject(T objectClass) throws IOException, ClassNotFoundException, ClassCastException {
        Map<Long, Set<Short>> profileIdTempStuffMap = new HashMap<Long, Set<Short>>();
        int mapSize = in.readInt();
        for(int j = 0; j < mapSize; j++) {
            long keyId = in.readLong();
            Set<Short> set = new HashSet<Short>();
            profileIdTempStuffMap.put(keyId, set);
            int setSize = in.readInt();
            for(int k = 0; k < setSize; k++) {
                short id = in.readShort();
                set.add(id);
            }
        }
        Server.sysLog.info("restore the issuance of a temporary hat for {} profiles", profileIdTempStuffMap.size());

        return (T) profileIdTempStuffMap;
    }

}
