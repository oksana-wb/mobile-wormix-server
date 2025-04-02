package com.pragmatix.arena.coliseum;

import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.arena.coliseum.messages.*;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.sessions.Sessions;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class ColiseumController {

    @Autowired(required = false)
    private ColiseumService coliseumService;

    @OnMessage
    public ColiseumStateResponse onGetColiseumState(GetColiseumState msg, UserProfile profile) {
        ColiseumEntity result = coliseumService.coliseumEntity(profile);
        return successResponse(result);
    }

    @OnMessage
    public ColiseumStateResponse onAddTeamMember(AddTeamMember msg, UserProfile profile) {
        Pair<ColiseumEntity, ColiseumErrorEnum> result = coliseumService.addTeamMember(profile, msg.teamMemberIndex);
        if(result.getKey() != null) {
            return successResponse(result.getKey());
        } else {
            return errorResponse(result.getValue());
        }
    }

    @OnMessage
    public ColiseumStateResponse onBuyColiseumTicket(BuyColiseumTicket msg, UserProfile profile) {
        Pair<ColiseumEntity, ColiseumErrorEnum> result = coliseumService.buyColiseumTicket(profile);
        if(result.getKey() != null) {
            return successResponse(result.getKey());
        } else {
            return errorResponse(result.getValue());
        }
    }

    @OnMessage
    public GetRewardResponse onGetReward(GetReward msg, UserProfile profile) {
        GenericAwardStructure[] reward = coliseumService.getReward(profile);
        return new GetRewardResponse(reward, Sessions.getKey());
    }

    @OnMessage
    public ColiseumStateResponse onInterruptSeries(InterruptSeries msg, UserProfile profile) {
        ColiseumEntity coliseumEntity = coliseumService.interruptSeries(profile);
        return successResponse(coliseumEntity);
    }

    private ColiseumStateResponse errorResponse(ColiseumErrorEnum error) {
        Pair<Integer, Integer> openTime = coliseumService.openTime();
        return new ColiseumStateResponse(error.getType(), openTime.getKey(), openTime.getValue());
    }

    private ColiseumStateResponse successResponse(ColiseumEntity state) {
        Pair<Integer, Integer> openTime = coliseumService.openTime();
        return new ColiseumStateResponse(state.num, state.open, state.win, state.defeat, state.draw, state.candidats,
                coliseumService.removeNulls(state.team), openTime.getKey(), openTime.getValue());
    }

}
