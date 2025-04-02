package com.pragmatix.pvp.services;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.services.serialize.AppMergeBeanFactory;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.messages.battle.client.DropReasonEnum;
import com.pragmatix.pvp.messages.battle.client.PvpActionEx;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.server.PvpStartTurn;
import com.pragmatix.pvp.messages.handshake.server.BattleCreated;
import com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.BattleStateTrackerI;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import com.pragmatix.pvp.services.battletracking.PvpBattleStateEnum;
import com.pragmatix.serialization.BinarySerializer;
import com.pragmatix.serialization.interfaces.StructureSerializer;
import com.pragmatix.sessions.AppServerAddress;
import com.pragmatix.testcase.AbstractTest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.SetUtils;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.09.2016 12:48
 */
public class ReplayServiceTest extends AbstractTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private BinarySerializer serializer;

    @Before
    public void init() throws Exception {
        serializer = new BinarySerializer();
        List<StructureSerializer<?, ?>> serializers = Arrays.asList(
                newStructureSerializer(Class.forName("generated.com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructureBinarySerializer")),
                newStructureSerializer(Class.forName("generated.com.pragmatix.pvp.messages.PvpProfileStructureBinarySerializer.class")),
                newStructureSerializer(Class.forName("generated.com.pragmatix.app.messages.structures.WormStructureBinarySerializer.class")),
                newStructureSerializer(Class.forName("generated.com.pragmatix.clan.structures.ClanMemberStructureBinarySerializer.class"))
        );
        serializer.setStructureSerializers(new HashSet<>(serializers));
    }

    private StructureSerializer newStructureSerializer(Class clazz) throws Exception {
        StructureSerializer structureSerializer = (StructureSerializer) clazz.newInstance();
        Field field = clazz.getDeclaredField("serializer");
        field.setAccessible(true);
        field.set(structureSerializer, serializer);

        Arrays.stream(clazz.getDeclaredFields()).filter(f -> f.getName().equals("mergeBeanFactory")).findFirst().ifPresent(f -> {
            f.setAccessible(true);
            try {
                f.set(structureSerializer, new AppMergeBeanFactory());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });

        return structureSerializer;
    }

    @Test
    public void replayParser() throws Exception {
//        String commandBufKey = "2016-09-19/17/VNIOQGTETXBLZPDT";
        String commandBufKey = "2016-09-20/14/FWZDK7QXIF0Z81ZA";
        ReplayService service = new ReplayService(serializer);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(new File(service.getReplayStoreBaseDir(), commandBufKey)));
        GzipCompressorInputStream inputStream = new GzipCompressorInputStream(bufferedInputStream);
        ByteBuf buf = Unpooled.wrappedBuffer(IOUtils.toByteArray(inputStream));
        StructureSerializer<BattleCreatedStructure, BattleCreatedStructure> battleCreatedStructureSerializer = serializer.getStructureSerializer(BattleCreatedStructure.class, BattleCreatedStructure.class);

        log.info("version <- {}", buf.readInt());
        BattleCreatedStructure battleCreatedStructure = battleCreatedStructureSerializer.deserializeStructure(buf);
        log.info("{}", battleCreatedStructure);

        int turnNum = 1;
        int commandNum = 0;

        while (buf.readableBytes() > 0) {
            byte action = buf.readByte();
            switch (action) {
                case 0:
                    readPvpActionEx(buf);
                    commandNum++;
                    break;
                case 1:
                    byte playerNum = buf.readByte();
                    DropReasonEnum reason = DropReasonEnum.values()[buf.readShort()];
                    log.info("PvpDropPlayer [{}] -> {}", playerNum, reason);
                    commandNum++;
                    break;
                case 2:
                    commandNum++;
                    log.info("turn {} -> {}", turnNum, commandNum);
                    turnNum++;
                    commandNum = 0;
                    break;
                case 3:
                    Tuple2<Byte, byte[]> turningPlayerNum_droppedPlayers = readPvpStartTurn(buf);
                    log.info("turning {} droppedPlayers {}", turningPlayerNum_droppedPlayers._1, Arrays.toString(turningPlayerNum_droppedPlayers._2));
                    commandNum++;
                    break;
                case 4:
                    log.info("end battle. turns {}", turnNum);
                    if(buf.readableBytes() > 0)
                        log.info("result: {}", readEndBattle(buf));
            }
        }
    }

    private long[] readPvpActionEx(ByteBuf stream) {
        long firstFrame = stream.readInt() & 0x00000000FFFFFFFFL;
        long lastFrame = stream.readInt() & 0x00000000FFFFFFFFL;
        int idsSize = stream.readUnsignedShort();
        long[] seq = new long[idsSize + 2];
        seq[0] = firstFrame;
        seq[1] = lastFrame;
        for(int i = 0; i < idsSize; i++) {
            seq[i + 2] = stream.readInt() & 0x00000000FFFFFFFFL;
        }
        return seq;
    }

    private Tuple2<Byte, byte[]> readPvpStartTurn(ByteBuf stream) {
        byte turningPlayerNum = stream.readByte();
        int droppedPlayersSize = stream.readUnsignedShort();
        byte[] seq = new byte[droppedPlayersSize];
        for(int i = 0; i < droppedPlayersSize; i++) {
            seq[i] = stream.readByte();
        }
        return Tuple.of(turningPlayerNum, seq);
    }

    private Map<String, Tuple2<PvpBattleResult, BattleParticipant.State>> readEndBattle(ByteBuf stream) {
        Map<String, Tuple2<PvpBattleResult, BattleParticipant.State>> result = new LinkedHashMap<>();
        int size = stream.readUnsignedShort();
        for(int i = 0; i < size; i++) {
            String key = "" + stream.readByte() + ":" + stream.readInt();
            PvpBattleResult pvpBattleResult = PvpBattleResult.valueOf(stream.readShort());
            BattleParticipant.State state = BattleParticipant.State.valueOf(stream.readShort());
            result.put(key, Tuple.of(pvpBattleResult, state));
        }
        return result;
    }

    @Test
    public void test() throws Exception {
        ReplayService service = new ReplayService(serializer);
        BattleBuffer battleBuffer = new BattleBuffer(1L, PvpBattleType.FRIEND_PvP, 1, new BattleStateTrackerI() {
            @Override
            public void handleEvent(PvpUser user, Object event, BattleBuffer battleBuffer) {
            }

            @Override
            public void handleAction(PvpBattleActionEnum action, BattleBuffer battleBuffer) {
            }

            @Override
            public PvpBattleStateEnum getInitState() {
                return PvpBattleStateEnum.WaitProfiles;
            }
        });

        BattleParticipant participant = new BattleParticipant(1L, (byte) 1, BattleParticipant.State.needProfile, 1, 1, new AppServerAddress(""));
        int version = new Random().nextInt(10000);
        log.info("version -> {}", version);
        participant.setVersion(version);
        battleBuffer.addParticipant(participant);

        service.onBattleCreated(battleBuffer, new BattleCreated());

        PvpActionEx pvpActionEx = new PvpActionEx();
        service.onPvpActionEx(battleBuffer, pvpActionEx);
        service.onPvpActionEx(battleBuffer, pvpActionEx);

        service.onPvpEndTurn(battleBuffer, new PvpEndTurn());
        service.onPvpStartTurn(battleBuffer, new PvpStartTurn());

        service.onFinishBattle(battleBuffer);

        log.info(battleBuffer.commandBufKey);

        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(new File(service.getReplayStoreBaseDir(), battleBuffer.commandBufKey)));
        GzipCompressorInputStream inputStream = new GzipCompressorInputStream(bufferedInputStream);
//        ByteBuf buf = Unpooled.wrappedBuffer(IOUtils.toByteArray(bufferedInputStream));
        ByteBuf buf = Unpooled.wrappedBuffer(IOUtils.toByteArray(inputStream));
        log.info("version <- {}", buf.readInt());

        StructureSerializer<BattleCreatedStructure, BattleCreatedStructure> structureSerializer = serializer.getStructureSerializer(BattleCreatedStructure.class, BattleCreatedStructure.class);
        structureSerializer.deserializeStructure(buf);

        log.info("{}", buf.readByte());
        buf.readInt();
        buf.readInt();
        buf.readShort();

        log.info("{}", buf.readByte());
        buf.readInt();
        buf.readInt();
        buf.readShort();

        log.info("{}", buf.readByte());

        log.info("{}", buf.readByte());
        buf.readByte();
        buf.readShort();

        log.info("{}", buf.readByte());
    }

    @Test
    public void test2() throws InterruptedException {
        try {
            int i = 0;
            while (true) {
                i++;
                ByteBuf byteBuf = Unpooled.directBuffer();
                byteBuf.writeBytes(new byte[1_000_000]);
                //            byteBuf.release();
                if(i % 1000 == 0)
                    log.info("{}", i);
            }
        } catch (Throwable e) {
            log.error(e.toString(), e);
        } finally {
            Thread.sleep(Integer.MAX_VALUE);
        }
    }
}