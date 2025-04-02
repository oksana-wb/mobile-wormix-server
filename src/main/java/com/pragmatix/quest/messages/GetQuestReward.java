package com.pragmatix.quest.messages;

import com.pragmatix.serialization.annotations.Command;

@Command(118)
public class GetQuestReward {

    public int questId;

    public int rewardId;

    public GetQuestReward() {
    }

    public GetQuestReward(int questId) {
        this.questId = questId;
    }

    public GetQuestReward(int questId, int rewardId) {
        this.questId = questId;
        this.rewardId = rewardId;
    }

    @Override
    public String toString() {
        return "GetReward{" +
                "questId=" + questId +
                ", rewardId=" + rewardId +
                '}';
    }

}
