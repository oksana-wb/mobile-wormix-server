package com.pragmatix.quest;

import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.quest.messages.GetQuestProgress;
import com.pragmatix.quest.messages.GetQuestProgressResult;
import com.pragmatix.quest.messages.GetQuestReward;
import com.pragmatix.quest.messages.GetQuestRewardResult;
import io.vavr.Tuple2;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.12.2015 12:45
 */
@Controller
public class QuestController {

    @Resource
    private QuestService questService;

    @OnMessage
    public GetQuestProgressResult onGetQuestProgress(GetQuestProgress msg, UserProfile profile) throws Exception {
       return new GetQuestProgressResult(questService.questsProgress(profile));
    }

    @OnMessage
    public GetQuestRewardResult onGetQuestReward(GetQuestReward msg, UserProfile profile) throws Exception {
        Tuple2<ShopResultEnum, List<GenericAwardStructure>> result = questService.reward(profile, msg.questId, msg.rewardId);
        return new GetQuestRewardResult(result._1, result._2, Sessions.getKey());
    }

}
