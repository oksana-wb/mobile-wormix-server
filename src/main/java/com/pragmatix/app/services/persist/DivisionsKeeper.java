package com.pragmatix.app.services.persist;

import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.app.services.rating.Division;
import com.pragmatix.gameapp.services.persist.DefaultKeeperImpl;
import com.pragmatix.serialization.BinarySerializer;
import com.pragmatix.serialization.interfaces.StructureSerializer;
import com.pragmatix.server.Server;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.05.11 14:50
 */
public class DivisionsKeeper extends DefaultKeeperImpl {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Callable<Map<Integer, Division>> createDivisions;

    StructureSerializer<RatingProfileStructure, RatingProfileStructure> structureSerializer;

    public DivisionsKeeper(Callable<Map<Integer, Division>> createDivisions, BinarySerializer binarySerializer) {
        this.createDivisions = createDivisions;
        structureSerializer = binarySerializer.getStructureSerializer(RatingProfileStructure.class, RatingProfileStructure.class);
    }

    @Override
    public void writeObject(Object o) throws IOException {
        int profiles = 0;
        ByteBuf buffer = Unpooled.buffer();
        Map<Integer, Division> map = (Map<Integer, Division>) o;
        out.writeInt(map.size());
        for(Map.Entry<Integer, Division> entry : map.entrySet()) {
            out.writeInt(entry.getKey());
            Map<Long, RatingProfileStructure> topPlayers = entry.getValue().getTopPlayers();
            out.writeInt(topPlayers.size());
            for(Map.Entry<Long, RatingProfileStructure> topPlayersEntry : topPlayers.entrySet()) {
                out.writeInt(topPlayersEntry.getKey().intValue());
                structureSerializer.serializeStructure(topPlayersEntry.getValue(), buffer);
                out.writeShort(buffer.readableBytes());
                out.write(buffer.array(), 0, buffer.readableBytes());
                buffer.clear();
                profiles++;
            }
        }
        Server.sysLog.info("stored {} RatingProfileStructures", profiles);
    }

    @Override
    public <T> T readObject(T objectClass) throws IOException, ClassNotFoundException {
        Map<Integer, Division> divisions = null;
        int profiles = 0;
        try {
            divisions = createDivisions.call();
            int size = in.readInt();
            for(int i = 0; i < size; i++) {
                int divisionKey = in.readInt();
                Division division = divisions.get(divisionKey);
                int topPlayersSize = in.readInt();
                for(int j = 0; j < topPlayersSize; j++) {
                    long profileId = in.readInt();
                    short structSize = in.readShort();
                    byte[] array = new byte[structSize];
                    int readed = in.read(array);
                    while (readed < structSize) {
                        int readed_ = in.read(array, readed, structSize - readed);
                        if(readed_ == -1) {
                            throw new Exception(String.format("i: %s, divisionKey: %s, topPlayers: %s, j: %s, profileId: %s В потоке отсутствуют данные! Считано только %s из %s байт!", i, divisionKey, topPlayersSize, j, profileId, readed, structSize));
                        }
                        readed += readed_;
                    }
                    RatingProfileStructure ratingProfileStructure = structureSerializer.deserializeStructure(Unpooled.wrappedBuffer(array));
                    division.getTopPlayers().put(profileId, ratingProfileStructure);
                    profiles++;
                }
            }
            Server.sysLog.info("restored {} RatingProfileStructures", profiles);
            return (T) divisions;
        } catch (Exception e) {
            log.error(e.toString(), e);
            return null;
        }
    }

}
