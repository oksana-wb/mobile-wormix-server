package com.pragmatix.app.services.persist;

import com.pragmatix.app.services.UserRegistry;
import com.pragmatix.gameapp.services.persist.DefaultKeeperImpl;
import com.pragmatix.server.Server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.05.11 14:50
 */
public class MapIntegerByteKeeper extends DefaultKeeperImpl {

    public final String name;

    public MapIntegerByteKeeper(String name) {
        this.name = name;
    }

    @Override
    public void writeObject(Object o) throws IOException {
        Map<Integer, Byte> map = (Map<Integer, Byte>) o;
        out.writeInt(map.size());
        for(Map.Entry<Integer, Byte> entry : map.entrySet()) {
            out.writeInt(entry.getKey());
            out.writeByte(entry.getValue());
        }
        Server.sysLog.info("[{}] Persisted {} Entry<Integer, Byte>", name, map.size());
    }

    @Override
    public <T> T readObject(T objectClass) throws IOException, ClassNotFoundException {
        Map<Integer, Byte> map = new HashMap<>();
        int size = in.readInt();
        for(int i = 0; i < size; i++) {
            int key = in.readInt();
            byte value = in.readByte();

            map.put(key, value);
        }
        Server.sysLog.info("[{}] Loaded {} Entry<Integer, Byte>", name, map.size());
        return (T) map;
    }

}
