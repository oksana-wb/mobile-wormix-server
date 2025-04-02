package com.pragmatix.intercom.messages;

import com.pragmatix.app.common.BattleState;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.serialization.annotations.Command;

/**
 * Запрос на синхронизацию состояния боя игрока
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 29.05.12 18:16
 *
 * @see com.pragmatix.app.controllers.AppIntercomController#onCompareAndSetBattleState(CompareAndSetBattleState, com.pragmatix.sessions.IAppServer)
 */
@Command(4006)
public class CompareAndSetBattleState extends IntercomRequest {

    public BattleState updateState;

    public BattleState expectState;

    public long battleId;

    public PvpBattleType pvpBattleType;

    public CompareAndSetBattleState() {
    }

    public CompareAndSetBattleState(BattleParticipant participant, BattleState expectState, BattleState updateState, long battleId, PvpBattleType pvpBattleType) {
        this.requestId=REQUEST_SEQ.incrementAndGet();
        this.profileId = participant.getProfileId();
        this.socialNetId = participant.getSocialNetId();
        this.expectState = expectState;
        this.battleId = battleId;
        this.updateState = updateState;
        this.pvpBattleType = pvpBattleType;
    }

    @Override
    public String toString() {
        return "CompareAndSetBattleState{" +
                "profileId=" + profileId +
                ", socialNet=" + SocialServiceEnum.valueOf(socialNetId) +
                ", requestId=" + requestId +
                ", updateState=" + updateState +
                ", expectState=" + expectState +
                ", battleId=" + battleId +
                ", pvpBattleType=" + pvpBattleType +
                '}';
    }
}
