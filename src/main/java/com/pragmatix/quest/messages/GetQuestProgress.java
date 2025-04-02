package com.pragmatix.quest.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.quest.QuestController#onGetQuestProgress(GetQuestProgress, UserProfile)
 */
@Command(117)
public class GetQuestProgress {

    public GetQuestProgress() {
    }

    @Override
    public String toString() {
        return "GetQuestProgress{}";
    }
}
