package com.pragmatix.pvp.services.battletracking;

import com.pragmatix.intercom.messages.EndPvpBattleResponse;
import com.pragmatix.intercom.messages.GetProfileError;
import com.pragmatix.intercom.messages.GetProfileResponse;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.client.PvpActionEx;
import com.pragmatix.pvp.messages.battle.client.PvpDropPlayer;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurnResponse;
import com.pragmatix.pvp.messages.handshake.client.*;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.ReplayService;
import com.pragmatix.pvp.services.battletracking.translators.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.06.11 17:01
 */
@Component
public class CommandTranslator {

    private final Map<Class<? extends PvpCommandI>, TranslatePvpCommandI> translatorsMap = new HashMap<Class<? extends PvpCommandI>, TranslatePvpCommandI>();

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private PvpService pvpService;

    @Resource
    private ReplayService replayService;

    @PostConstruct
    public void init() {
        translatorsMap.put(PvpActionEx.class, new CountedCommandTranslator(PvpBattleActionEnum.PvpAction));
        translatorsMap.put(PvpEndTurn.class, new CountedCommandTranslator(PvpBattleActionEnum.EndTurn));
        translatorsMap.put(PvpEndTurnResponse.class, new PvpEndTurnResponseTranslator());
        translatorsMap.put(GetProfileResponse.class, new GetProfileResponseTranslator());
        translatorsMap.put(GetProfileError.class, new SimpleTranslator(PvpBattleActionEnum.CancelBattle));
        translatorsMap.put(RejectBattleOffer.class, new SimpleTranslator(PvpBattleActionEnum.CancelBattle));
        translatorsMap.put(CancelBattle.class, new SimpleTranslator(PvpBattleActionEnum.CancelBattle));
        translatorsMap.put(JoinToBattle.class, new JoinToBattleTranslator(pvpService));
        translatorsMap.put(ReadyForBattle.class, new ReadyForBattleTranslator());
        translatorsMap.put(EndPvpBattleResponse.class, new EndPvpBattleResponseTranslator(pvpService));
        translatorsMap.put(PvpDropPlayer.class, new PvpDropPlayerTranslator(replayService));
        translatorsMap.put(WidenSearch.class, new SimpleTranslator(PvpBattleActionEnum.WidenSearch));
    }

    public PvpBattleActionEnum translateCommand(PvpCommandI cmd, PvpUser profile, BattleBuffer battleBuffer) {
        TranslatePvpCommandI pvpCommandTranslator = translatorsMap.get(cmd.getClass());
        if(pvpCommandTranslator != null) {
            return pvpCommandTranslator.translateCommand(cmd, profile, battleBuffer);
        } else {
            log.error("battleId={}, CommandTranslator not found for PvpCommand [{}]", battleBuffer.getBattleId(), cmd.getClass());
            return PvpBattleActionEnum.Desync;
        }
    }

}
