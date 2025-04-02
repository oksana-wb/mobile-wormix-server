package com.pragmatix.quest.quest03;

import com.google.gson.Gson;
import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.quest.QuestDef;
import com.pragmatix.quest.dao.QuestEntity;
import io.vavr.Tuple2;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.09.2016 9:37
 */
public class Quest03 implements QuestDef {

    private boolean enabled = true;

    @Override
    public int id() {
        return 3;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String progress(QuestEntity entity) {
        return new Gson().toJson(entity.q3());
    }

    @Override
    public void start(UserProfile profile, QuestEntity entity) {

    }

    @Override
    public boolean canStart(QuestEntity entity) {
        return true;
    }

    @Override
    public void consumeBattleResult(UserProfile profile, QuestEntity questEntity, PvpBattleResult battleResult) {

    }

    @Override
    public Tuple2<ShopResultEnum, List<GenericAwardStructure>> reward(UserProfile profile, QuestEntity entity, int rewardId) {
        return null;
    }

    @Override
    public boolean isRewarded(QuestEntity questEntity) {
        return false;
    }
}
