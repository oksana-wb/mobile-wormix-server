package com.pragmatix.app.services.persist;

import com.pragmatix.gameapp.services.persist.DefaultKeeperImpl;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.server.Server;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.05.11 14:50
 */
public class ProfilesByStringIdMapKeeper extends DefaultKeeperImpl {

    @Override
    public void writeObject(Object o) throws IOException {
        Map<SocialServiceEnum, Map<Long, String>> map = (Map<SocialServiceEnum, Map<Long, String>>) o;
        int size = 0;
        for(Map<Long, String> longStringMap : map.values()) {
            size += longStringMap.size();
        }
        out.writeInt(size);
        for(Map.Entry<SocialServiceEnum, Map<Long, String>> socialServiceEnumMapEntry : map.entrySet()) {
            short socialNetId = socialServiceEnumMapEntry.getKey().getShortType();
            for(Map.Entry<Long, String> entry : socialServiceEnumMapEntry.getValue().entrySet()) {
                out.writeUTF(entry.getValue());
                out.writeShort(socialNetId);
                out.writeInt(entry.getKey().intValue());
            }
        }


        Server.sysLog.info("Persisted {} items", map.size());
    }

    @Override
    public <T> T readObject(T objectClass) throws IOException, ClassNotFoundException {
        Map<String, Pair<Short, Long>> map = new HashMap<>();
        int size = in.readInt();
        for(int i = 0; i < size; i++) {
            map.put(in.readUTF(), new ImmutablePair<>(in.readShort(), (long) in.readInt()));
        }
        Server.sysLog.info("Loaded {} items", map.size());
        return (T) map;
    }

}
