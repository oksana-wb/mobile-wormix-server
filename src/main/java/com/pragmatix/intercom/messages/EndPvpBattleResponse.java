package com.pragmatix.intercom.messages;

import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 29.05.12 18:16
 * @see com.pragmatix.pvp.controllers.PvpIntercomController#onEndPvpBattleResponse(EndPvpBattleResponse, com.pragmatix.sessions.IAppServer)
 */
@Command(4004)
public class EndPvpBattleResponse extends IntercomResponse implements PvpCommandI {

    @Resize(TypeSize.UINT32)
    public long battleId;

    public List<GenericAwardStructure> award;

    public EndPvpBattleResponse() {
    }

    public EndPvpBattleResponse(IntercomRequestI request, long battleId, List<GenericAwardStructure> award) {
        super(request);
        this.battleId = battleId;
        this.award = award;
    }

    public long getBattleId() {
        return battleId;
    }

    public void setBattleId(long battleId) {
        this.battleId = battleId;
    }

    @Override
    public String toString() {
        return "EndPvpBattleResponse{" +
                "profileId=" + profileId +
                ", socialNet=" + SocialServiceEnum.valueOf(socialNetId) +
                ", requestId=" + requestId +
                ", battleId=" + battleId +
                ", award=" + award +
                '}';
    }

}
