package com.pragmatix.app.controllers;

import com.pragmatix.app.common.Connection;
import com.pragmatix.app.messages.client.GetWhoPumpedReaction;
import com.pragmatix.app.messages.client.PumpReactionRate;
import com.pragmatix.app.messages.client.PumpReactionRates;
import com.pragmatix.app.messages.server.PumpReactionRateResult;
import com.pragmatix.app.messages.server.PumpReactionRatesResult;
import com.pragmatix.app.messages.server.WhoPumpedReactionResult;
import com.pragmatix.app.messages.structures.ProfileDoubleKeyStructure;
import com.pragmatix.app.messages.structures.PumpReactionRateStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.ReactionRateService;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;

import javax.annotation.Resource;
import java.util.List;

/**
 * Котроллер обрабатывает команду на прокачку скорости реакции другу
 * <p/>
 * Created: 30.04.11 11:07
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
@Controller
public class PumpReactionRateController {

    @Resource
    private ReactionRateService reactionRateService;

    @Resource
    private ProfileService profileService;

    @OnMessage
    public PumpReactionRateResult onPumpReactionRate(PumpReactionRate msg, UserProfile profile) {
        if(!reactionRateService.isInitialized())
            return null;

        PumpReactionRateResult.ResultEnum resultEnum = reactionRateService.pumpReactionRate(profile, msg.friendId);
        return new PumpReactionRateResult(resultEnum);
    }

    @OnMessage
    public PumpReactionRatesResult onPumpReactionRates(PumpReactionRates msg, UserProfile profile) {
        if(!reactionRateService.isInitialized())
            return null;

        if(msg.friendIds != null && msg.friendIds.length > 0) {
            PumpReactionRateStructure[] pumpedFriends = reactionRateService.pumpReactionRates(profile, msg.friendIds);
            return new PumpReactionRatesResult(pumpedFriends);
        }else{
            return new PumpReactionRatesResult(new PumpReactionRateStructure[0]);
        }
    }

    @OnMessage
    public WhoPumpedReactionResult onGetWhoPumpedReaction(GetWhoPumpedReaction msg, UserProfile profile) {
        if(!reactionRateService.isInitialized())
            return null;

        List<List<Long>> whoPumped = reactionRateService.getWhoPumped(profile, msg.todayOnly);

        Long[] todayPumpedLong = whoPumped.size() > 0 ? whoPumped.get(0).toArray(new Long[whoPumped.get(0).size()]) : new Long[0];
        Long[] yesterdayPumpedLong = whoPumped.size() > 1 ? whoPumped.get(1).toArray(new Long[whoPumped.get(1).size()]) : new Long[0];
        Long[] twoDaysAgoPumpedLong = whoPumped.size() > 2 ? whoPumped.get(2).toArray(new Long[whoPumped.get(2).size()]) : new Long[0];

        return new WhoPumpedReactionResult(fillStructure(todayPumpedLong), fillStructure(yesterdayPumpedLong), fillStructure(twoDaysAgoPumpedLong));
    }

    /**
     * Заполняем структуру строковыми значениями ID
     * @param pumpedLong масив лонговых ID
     * @return массив структур
     */
    private ProfileDoubleKeyStructure[] fillStructure(Long[] pumpedLong) {
        ProfileDoubleKeyStructure[] result = new ProfileDoubleKeyStructure[pumpedLong.length];
        for(int i = 0; i < result.length; i++) {
            result[i]= new ProfileDoubleKeyStructure(pumpedLong[i], profileService.getProfileStringId(pumpedLong[i]));
        }
        return result;
    }

}
