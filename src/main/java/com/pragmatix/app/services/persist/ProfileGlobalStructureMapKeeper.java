package com.pragmatix.app.services.persist;

import com.pragmatix.app.services.UserRegistry;
import com.pragmatix.gameapp.services.persist.DefaultKeeperImpl;
import com.pragmatix.server.Server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.05.11 14:50
 */
public class ProfileGlobalStructureMapKeeper extends DefaultKeeperImpl {

    private UserRegistry userRegistry;

    public ProfileGlobalStructureMapKeeper(UserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    @Override
    public void writeObject(Object o) throws IOException {
        Map<Long, UserRegistry.ProfileGlobalStructure> map = (Map<Long, UserRegistry.ProfileGlobalStructure>) o;
        out.writeInt(map.size());
        for(Map.Entry<Long, UserRegistry.ProfileGlobalStructure> entry : map.entrySet()) {
            out.writeInt(entry.getKey().intValue());
            out.writeByte(entry.getValue().level);
            out.writeBoolean(entry.getValue().isAbandonded);
        }
        Server.sysLog.info("Persisted {} ProfileGlobalStructures", map.size());
    }

    @Override
    public <T> T readObject(T objectClass) throws IOException, ClassNotFoundException {
        Map<Long, UserRegistry.ProfileGlobalStructure> map = new ConcurrentHashMap<>();
        int size = in.readInt();
        for(int i = 0; i < size; i++) {
            long profileId = in.readInt();
            int level = in.readByte();
            boolean abandonded = in.readBoolean();

            UserRegistry.ProfileGlobalStructure profileGlobalStructure = userRegistry.getStructureInstanceForParams(level, abandonded);
            map.put(profileId, profileGlobalStructure);
        }
        Server.sysLog.info("Loaded {} ProfileGlobalStructures", map.size());
        return (T) map;
    }

}
