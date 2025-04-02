package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.app.common.BattleState;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.intercom.messages.CompareAndSetBattleState;
import com.pragmatix.intercom.messages.GetProfileError;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.handshake.client.CancelBattle;
import com.pragmatix.pvp.messages.handshake.client.RejectBattleOffer;
import com.pragmatix.pvp.messages.handshake.server.BattleCreationFailure;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.pragmatix.pvp.messages.handshake.server.BattleCreationFailure.CallToBattleResultEnum.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.11.12 11:46
 */
@Component
public class CancelHandler extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battle) {

        BattleCreationFailure battleCreationFailure;
        if(command != null) {
            if(command instanceof GetProfileError) {
                GetProfileError msg = (GetProfileError) command;
                battleCreationFailure = commandFactory.constructBattleCreationFailure(battle, msg);
            } else if(command instanceof CancelBattle) {
                CancelBattle msg = (CancelBattle) command;
                if(msg.error == 0) {
                    battleCreationFailure = commandFactory.constructBattleCreationFailure(battle, profile, CANCELED);
                } else {
                    log.error("клиент прислал ошибку создания боя [{}]\n {}", msg.error, battle.dumpBattleInitInfo());
                    battleCreationFailure = commandFactory.constructBattleCreationFailure(battle, profile, CLIENT_ERROR);
                }
                // закрыть соедидение с игроком приславшем CancelBattle
                Connections.closeConnectionDeferred();
            } else if(command instanceof RejectBattleOffer) {
                BattleCreationFailure.CallToBattleResultEnum reason = ((RejectBattleOffer) command).manual ? REJECTED : BUSY;
                battleCreationFailure = commandFactory.constructBattleCreationFailure(battle, profile, reason);
            } else {
                log.error("не предусмотренная команда привела к отмене боя: " + command);
                battleCreationFailure = commandFactory.constructBattleCreationFailure(battle, profile, SERVER_ERROR);
            }
        } else if(action == PvpBattleActionEnum.Disconnect) {
            battleCreationFailure = commandFactory.constructBattleCreationFailure(battle, profile, DISCONNECTED);
        } else if(action == PvpBattleActionEnum.Desync) {
            battleCreationFailure = commandFactory.constructBattleCreationFailure(battle, profile, SERVER_ERROR);
        } else if(action == PvpBattleActionEnum.StateTimeout) {
            battleCreationFailure = commandFactory.constructBattleCreationFailure(battle, profile, TIMEOUT);
        } else {
            log.error("не предусмотренное действие привело к отмене боя: " + action);
            battleCreationFailure = commandFactory.constructBattleCreationFailure(battle, 0, 0, SERVER_ERROR);
        }

        // отсылаем причину отмены боя, всем кто уже успел подконнектиться
        pvpService.dispatchSilentToAll(battle, battleCreationFailure);

        //возвращаем статусы тем кому мы его меняли в рамках подготовки боя, т.е. тем тем чьи структуры мы получили
        for(BattleParticipant participant : battle.getParticipants()) {
            if(participant.getPvpProfileStructure() != null) {
                if(log.isDebugEnabled()) {
                    log.debug("battleId={}: бой отменен, для {} возврящаем состояние NOT_IN_BATTLE", battle.getBattleId(), PvpService.formatPvpUserId(participant.getPvpUserId()));
                }
                CompareAndSetBattleState compareAndSetBattleState = new CompareAndSetBattleState(participant, BattleState.WAIT_START_BATTLE, BattleState.NOT_IN_BATTLE, 0, battle.getBattleType());
                Messages.toServer(compareAndSetBattleState, participant.getMainServer(), false);
            }
        }

        return null;
    }

}
