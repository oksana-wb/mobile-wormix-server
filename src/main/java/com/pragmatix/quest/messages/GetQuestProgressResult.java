package com.pragmatix.quest.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.quest.QuestProgressStructure;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;
import java.util.List;

/**
 * @see com.pragmatix.quest.QuestController#onGetQuestProgress(GetQuestProgress, UserProfile)
 */
@Command(10117)
public class GetQuestProgressResult {

    public QuestProgressStructure[] questProgress;

    public GetQuestProgressResult() {
    }

    public GetQuestProgressResult(List<QuestProgressStructure> questProgress) {
        this.questProgress = questProgress.toArray(new QuestProgressStructure[questProgress.size()]);
    }

    @Override
    public String toString() {
        return "GetQuestProgressResult{" + Arrays.toString(questProgress) + '}';
    }

}
