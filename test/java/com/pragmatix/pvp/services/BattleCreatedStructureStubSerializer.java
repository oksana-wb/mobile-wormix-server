package com.pragmatix.pvp.services;

import com.pragmatix.serialization.BinarySerializer;
import com.pragmatix.serialization.SerializeContext;
import com.pragmatix.serialization.annotations.BinaryProtocol;
import com.pragmatix.serialization.annotations.DeserializerFor;
import com.pragmatix.serialization.annotations.SerializerFor;
import com.pragmatix.serialization.interfaces.StructureSerializer;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Random;

/**
 * Generated serializer source for class : com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure
 */
@BinaryProtocol
@SerializerFor(com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure.class)
@DeserializerFor(com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure.class)
public class BattleCreatedStructureStubSerializer implements StructureSerializer<com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure, com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public void serializeStructure(com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure self, ByteBuf stream, SerializeContext context) throws IOException {
        int value = new Random().nextInt(10000);
        log.info("write: {}", value);
        stream.writeInt(value);
    }

    public com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure deserializeStructure(ByteBuf stream, SerializeContext context) throws IOException {
        com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure self = new com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure();
        int value = stream.readInt();
        log.info("read: {}", value);
        return self;
    }

    public com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure deserializeStructureAndConvert(ByteBuf stream, SerializeContext context) throws IOException {
        return convert(deserializeStructure(stream, context), null, context);
    }

    public com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure convert(com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure source, com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure targetCurrentValue, SerializeContext context) {
        return source;
    }

}
