package com.pragmatix.pvp.services;

import com.pragmatix.app.settings.AppParams;
import com.pragmatix.pvp.messages.battle.client.PvpActionEx;
import com.pragmatix.pvp.messages.battle.client.PvpDropPlayer;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.server.PvpStartTurn;
import com.pragmatix.pvp.messages.handshake.server.BattleCreated;
import com.pragmatix.pvp.messages.handshake.server.BattleCreatedStructure;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.serialization.BinarySerializer;
import com.pragmatix.serialization.interfaces.StructureSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.09.2016 10:45
 */
@Service
public class ReplayService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private File replayStoreBaseDir = new File("data/replay/");
    private int replayDataFileNameLength = 16;

    private BinarySerializer serializer;

    @Value("${ReplayService.enabled:false}")
    private boolean enabled = false;

    public ReplayService(BinarySerializer serializer) {
        this.serializer = serializer;
    }

    private boolean isEnabled(BattleBuffer battleBuffer){
       return enabled;
    }

    public void onBattleCreated(BattleBuffer battleBuffer, BattleCreated battleCreated) {
        if(!isEnabled(battleBuffer))
            return;
        try {
            ByteBuf commandBuf = Unpooled.directBuffer();

            commandBuf.writeInt(battleBuffer.getParticipantByNum(0).getVersion());

            StructureSerializer<BattleCreatedStructure, BattleCreatedStructure> structureSerializer = serializer.getStructureSerializer(BattleCreatedStructure.class, BattleCreatedStructure.class);
            structureSerializer.serializeStructure(battleCreated, commandBuf);

            battleBuffer.commandBuf = commandBuf;
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    public void onPvpActionEx(BattleBuffer battleBuffer, PvpActionEx pvpActionEx) {
        ByteBuf commandBuf = battleBuffer.commandBuf;
        if(commandBuf == null)
            return;

        commandBuf.writeByte(0);

        commandBuf.writeInt((int) ((pvpActionEx.firstFrame) & 0x00000000FFFFFFFFL));
        commandBuf.writeInt((int) ((pvpActionEx.lastFrame) & 0x00000000FFFFFFFFL));
        int idsSize = pvpActionEx.ids != null ? pvpActionEx.ids.length : 0;
        commandBuf.writeShort((short) ((idsSize) & 0x0000FFFF));
        if(idsSize > 0) {
            for(long item : pvpActionEx.ids) {
                commandBuf.writeInt((int) ((item) & 0x00000000FFFFFFFFL));
            }
        }
    }

    public void onPvpDropPlayer(BattleBuffer battleBuffer, PvpDropPlayer pvpDropPlayer) {
        ByteBuf commandBuf = battleBuffer.commandBuf;
        if(commandBuf == null)
            return;

        commandBuf.writeByte(1);
        commandBuf.writeByte(pvpDropPlayer.playerNum);
        commandBuf.writeShort(pvpDropPlayer.reason.getType());
    }

    public void onPvpEndTurn(BattleBuffer battleBuffer, PvpEndTurn pvpEndTurn) {
        ByteBuf commandBuf = battleBuffer.commandBuf;
        if(commandBuf == null)
            return;

        commandBuf.writeByte(2);
    }

    public void onPvpStartTurn(BattleBuffer battleBuffer, PvpStartTurn pvpStartTurn) {
        ByteBuf commandBuf = battleBuffer.commandBuf;
        if(commandBuf == null)
            return;

        commandBuf.writeByte(3);

        commandBuf.writeByte(pvpStartTurn.turningPlayerNum);
        int droppedPlayersSize = pvpStartTurn.droppedPlayers != null ? pvpStartTurn.droppedPlayers.length : 0;
        commandBuf.writeShort((short) ((droppedPlayersSize) & 0x0000FFFF));
        if(droppedPlayersSize > 0) {
            commandBuf.writeBytes(pvpStartTurn.droppedPlayers);
        }
    }

    public void onFinishBattle(BattleBuffer battleBuffer) {
        ByteBuf commandBuf = battleBuffer.commandBuf;
        if(commandBuf == null)
            return;

        commandBuf.writeByte(4);
        List<BattleParticipant> participants = battleBuffer.getParticipants().stream().filter(p -> !p.isEnvParticipant()).collect(Collectors.toList());
        commandBuf.writeShort(participants.size());
        for(BattleParticipant participant : participants) {
            commandBuf.writeByte(participant.getSocialNetId());
            commandBuf.writeInt((int) participant.getProfileId());
            commandBuf.writeShort(participant.battleResult.getType());
            commandBuf.writeShort(participant.getState().type);
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            String targetRelativeDir = now.getYear() +
                    "-" + (now.getMonthValue() < 10 ? "0" + now.getMonthValue() : "" + now.getMonthValue()) +
                    "-" + (now.getDayOfMonth() < 10 ? "0" + now.getDayOfMonth() : "" + now.getDayOfMonth()) +
                    "/" + (now.getHour() < 10 ? "0" + now.getHour() : "" + now.getHour()) + "/";

            File targetDir = new File(replayStoreBaseDir, targetRelativeDir);
            targetDir.mkdirs();

            String commandBufKey = targetRelativeDir + RandomStringUtils.randomAlphanumeric(replayDataFileNameLength).toUpperCase();
            File targetFile = new File(targetDir, commandBufKey);

            if(targetFile.exists()) {
                commandBufKey = targetRelativeDir + RandomStringUtils.randomAlphanumeric(replayDataFileNameLength).toUpperCase();
                targetFile = new File(targetDir, commandBufKey);
            }

            if(targetFile.exists()) {
                log.error("ошибка создания файла реплея! {}", targetFile);
                return;
            }

            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile));
                 CompressorOutputStream compressorOut = new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.GZIP, out);
            ) {
                compressorOut.write(ByteBufUtil.getBytes(commandBuf));
            }

            FileUtils.writeLines(new File(targetDir, commandBufKey + ".version"), Collections.singletonList(AppParams.versionToString(battleBuffer.getParticipantByNum(0).getVersion())));

            battleBuffer.commandBufKey = commandBufKey;
        } catch (Exception e) {
            log.error("ошибка создания файла реплея: " + e.toString(), e);
        } finally {
            battleBuffer.commandBuf.release();
        }
    }

    public void onDropBattle(BattleBuffer battleBuffer) {
        // перестраховка, для очистки буфера команд
        ByteBuf commandBuf = battleBuffer.commandBuf;
        try {
            if(commandBuf != null && commandBuf.refCnt() > 0)
                commandBuf.release(commandBuf.refCnt());
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    public File getReplayStoreBaseDir() {
        return replayStoreBaseDir;
    }
}
