package com.pragmatix.arena.mercenaries;

import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.arena.mercenaries.messages.*;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.sessions.Sessions;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Controller
public class MercenariesController {

    @Autowired(required = false)
    private MercenariesService mercenariesService;

    @OnMessage
    public MercenariesDefsResponse onGetMercenariesDefs(GetMercenariesDefs msg, UserProfile profile) {
        Stream<MercenariesTeamMember> sortedResult = mercenariesService.mercenariesDefs().stream().sorted(Comparator.comparing(MercenariesTeamMember::getId));
        return new MercenariesDefsResponse(sortedResult.toArray(MercenariesTeamMember[]::new));
    }

    @OnMessage
    public MercenariesStateResponse onGetMercenariesState(GetMercenariesState msg, UserProfile profile) {
        MercenariesEntity result = mercenariesService.mercenariesEntity(profile);
        return successResponse(profile, result);
    }

    @OnMessage
    public MercenariesStateResponse onSetTeamMember(SetMercenariesTeamMembers msg, UserProfile profile) {
        Pair<MercenariesEntity, MercenariesErrorEnum> result = mercenariesService.setTeamMembers(profile, msg.teams);
        if(result.getKey() != null) {
            return successResponse(profile, result.getKey());
        } else {
            return errorResponse(profile, result.getValue());
        }
    }

    @OnMessage
    public MercenariesStateResponse onBuyMercenariesTicket(BuyMercenariesTicket msg, UserProfile profile) {
        Pair<MercenariesEntity, MercenariesErrorEnum> result = mercenariesService.buyTicket(profile);
        if(result.getKey() != null) {
            return successResponse(profile, result.getKey());
        } else {
            return errorResponse(profile, result.getValue());
        }
    }

    @OnMessage
    public MercenariesRewardResponse onGetMercenariesReward(GetMercenariesReward msg, UserProfile profile) {
        List<GenericAwardStructure> reward = mercenariesService.getReward(profile);
        return new MercenariesRewardResponse(reward.toArray(new GenericAwardStructure[reward.size()]), Sessions.getKey());
    }

    private MercenariesStateResponse errorResponse(UserProfile profile, MercenariesErrorEnum error) {
        int attemptsRemainToday = mercenariesService.attemptsRemainToday(profile);
        return new MercenariesStateResponse(error.getType(), attemptsRemainToday);
    }

    private MercenariesStateResponse successResponse(UserProfile profile, MercenariesEntity state) {
        int attemptsRemainToday = mercenariesService.attemptsRemainToday(profile);
        return new MercenariesStateResponse(state.num, state.open, state.win, state.defeat, state.draw, state.total_win, state.total_defeat, state.total_draw,
                state.team, attemptsRemainToday);
    }

}
