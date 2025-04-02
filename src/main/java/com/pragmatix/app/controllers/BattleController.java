package com.pragmatix.app.controllers;

import com.pragmatix.app.common.BanType;
import com.pragmatix.app.common.BattleState;
import com.pragmatix.app.messages.client.*;
import com.pragmatix.app.messages.server.*;
import com.pragmatix.app.messages.server.ArenaLocked.ArenaLockedCause;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ArenaService;
import com.pragmatix.app.services.BanService;
import com.pragmatix.app.services.BattleService;
import com.pragmatix.app.services.CheatersCheckerService;
import com.pragmatix.app.settings.SimpleBattleSettings;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.pvp.PvpBattleKey;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

/**
 * контроллер обрабатывает команды для боя
 * User: denis
 * Date: 05.12.2009
 * Time: 2:37:43
 */
@Controller
public class BattleController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("#{battleAwardSettings.awardSettingsMap}")
    private Map<Short, SimpleBattleSettings> awardSettingsMap;

    @Resource
    private BattleService battleService;

    @Resource
    private BanService banService;

    @Resource
    private ArenaService arenaService;

    @OnMessage
    public ArenaResult onGetArena(GetArena msg, UserProfile profile) {
        return arenaService.newArenaResult(profile);
    }

    @OnMessage
    public Object onStartBattle(StartBattle msg, UserProfile profile) {
        // начисляем нужное количество битв если пришло время (без необходимости отправки GetArena перед этим)
        battleService.checkBattleCount(profile);

        // первая обучалка доступна даже когда бои кончились
        if(profile.getBattlesCount() > 0 || msg.missionId == -1) {
            SimpleBattleSettings battleSettings = awardSettingsMap.get(msg.missionId);

            if(battleSettings == null) {
                log.error("AwardSettings not found for missionId={} in SIMPLE battle", msg.missionId);
                return new ArenaLocked(ArenaLockedCause.CAUSE_WRONG_MISSION, profile.getCurrentMission());
            } else if(!battleService.validateMission(profile, msg.missionId, battleSettings)) {
                return new ArenaLocked(ArenaLockedCause.CAUSE_MISSION_LOCKED, profile.getCurrentMission());
            } else {
                List<GenericAwardStructure> award = new ArrayList<>();
                long battleId = battleService.startSimpleBattle(profile, battleSettings, msg.missionId, award);
                return new StartBattleResult(battleId, profile.getReagentsForBattle(), award, Sessions.getKey());
            }
        } else {
            long delay = battleService.getDelay(profile.getLevel());
            long lastTime = profile.getLastBattleTime();
            // говорим клиенту, что нужно подождать столько-то секунд
            return new ArenaLocked(lastTime + delay - System.currentTimeMillis(), profile.getCurrentMission());
        }
    }

    @OnMessage
    public EndTurnResponse onEndTurn(EndTurn msg, UserProfile profile) {
        // is Boss battle
        if(profile.getBattleState() == BattleState.SIMPLE && profile.getMissionId() > 0) {
            CheatersCheckerService.ValidationResult validationResult = battleService.onSimpleBattleTurn(profile, msg);
            if(validationResult != CheatersCheckerService.ValidationResult.BANNED) {
                SimpleResultEnum result = validationResult == CheatersCheckerService.ValidationResult.OK ? SimpleResultEnum.SUCCESS : SimpleResultEnum.ERROR;
                return new EndTurnResponse(result, msg, BattleService.lastTurnNum(profile.getMissionLog()));
            }
        }
        return null;
    }

    @OnMessage
    public EndBattleResult onEndBattle(EndBattle msg, UserProfile profile) {
        final EndBattleResult.EndBattleValidateResult endBattleValidateResult;
        final List<GenericAwardStructure> award;
        if(msg.battleId == profile.getLastProcessedBattleId()) {
            endBattleValidateResult = EndBattleResult.EndBattleValidateResult.DUPLICATED;
            award = Collections.emptyList();
        } else {
            List<GenericAwardStructure> defeatBattleAward = profile.defeatBattleAward;
            award = new ArrayList<>();
            endBattleValidateResult = battleService.endBattle(profile, msg, award);
            if(endBattleValidateResult == EndBattleResult.EndBattleValidateResult.OK && CollectionUtils.isNotEmpty(defeatBattleAward)) {
                award.addAll(0, defeatBattleAward);
            }
        }
        return new EndBattleResult(endBattleValidateResult, msg, award, Sessions.getKey());
    }

    @OnMessage
    public void onCheatDetected(CheatDetected msg, UserProfile profile) {
        BanType banType = BanType.valueOf(msg.banType);
        if(banType != null) {
            //добавляем читера в бан лист
            banService.addToBanList(profile.getId(), banType, banType.toString() + ": " + msg.banNote);
        } else {
            log.error("wrong banType in command {}", msg);
        }
        Connections.get().close();
    }

    @OnMessage
    public void onDesyncLog(DesyncLog msg, UserProfile profile) {
        try {
            Calendar cal = Calendar.getInstance();
            int month = cal.get(Calendar.MONTH) + 1;
            int day = cal.get(Calendar.DAY_OF_MONTH);
            String date = cal.get(Calendar.YEAR) + "-" + (month < 10 ? "0" + month : "" + month) + "-" + (day < 10 ? "0" + day : "" + day);
            File destDir = new File("data/desyncLog/" + date);
            destDir.mkdirs();
            String missionKey = "0";
            if(msg.missionIds.length == 1) {
                missionKey = "" + msg.missionIds[0];
            } else if(msg.missionIds.length == 2) {
                missionKey = "" + msg.missionIds[0] + "," + msg.missionIds[1];
            }
            String fileName = String.format("%s-%s-%s-%s-%s.zip", msg.battleId, profile.getId(), PvpBattleKey.valueOf(msg.battleType, msg.wager), msg.mapId, missionKey);
            File destFile = new File(destDir, fileName);
            FileUtils.writeByteArrayToFile(destFile, msg.battleLog);
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

}
