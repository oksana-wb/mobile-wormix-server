package com.pragmatix.quest.quest04;

import com.google.gson.Gson;
import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.rating.SeasonService;
import com.pragmatix.quest.QuestDef;
import com.pragmatix.quest.dao.QuestEntity;
import io.vavr.Tuple2;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.09.2016 9:37
 */
public class Quest04 implements QuestDef {

    private boolean enabled = true;

    @Resource
    SeasonService seasonService;

    @Override
    public int id() {
        return 4;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String progress(QuestEntity entity) {
        Map<Integer, Integer> progress = entity.q4().progress.getOrDefault(seasonService.getCurrentSeasonStartDate().toString(), Collections.emptyMap());
        return new Gson().toJson(progress);
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
