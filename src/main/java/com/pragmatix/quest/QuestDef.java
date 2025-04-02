package com.pragmatix.quest;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.quest.dao.QuestEntity;
import io.vavr.Tuple2;

import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.12.2015 9:58
 */
public interface QuestDef {

    int id();

    boolean isEnabled();

    String progress(QuestEntity entity);

    void start(UserProfile profile, QuestEntity entity);

    boolean canStart(QuestEntity entity);

    void consumeBattleResult(UserProfile profile, QuestEntity questEntity, PvpBattleResult battleResult);

    Tuple2<ShopResultEnum, List<GenericAwardStructure>> reward(UserProfile profile, QuestEntity entity, int rewardId);

    boolean isRewarded(QuestEntity questEntity);
}
