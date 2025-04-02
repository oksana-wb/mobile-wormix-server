package com.pragmatix.quest;

import com.pragmatix.serialization.annotations.Structure;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.12.2015 16:29
 */
@Structure
public class QuestProgressStructure {

    public int questId;

    public boolean canStartNew;

    public String progress;

    public boolean rewarded;

    public QuestProgressStructure() {
    }

    public QuestProgressStructure(int questId, boolean canStartNew, String progress, boolean rewarded) {
        this.questId = questId;
        this.canStartNew = canStartNew;
        this.progress = progress;
        this.rewarded = rewarded;
    }

    @Override
    public String toString() {
        return "{" +
                "questId=" + questId +
                ", canStartNew=" + canStartNew +
                ", progress='" + progress + '\'' +
                ", rewarded=" + rewarded +
                '}';
    }
}
