package com.pragmatix.quest.quest01;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.BattleService;
import com.pragmatix.app.services.ProfileBonusService;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.app.settings.GenericAwardFactory;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.quest.QuestDef;
import com.pragmatix.quest.dao.QuestEntity;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         <p>
 *         Сундуки с наградами для ПВП http://jira.pragmatix-corp.com/browse/WORMIX-4387
 */
public class Quest01 implements QuestDef {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private BattleService battleService;

    private GenericAwardFactory awardFactory;

    private int awardSlotsCount = 4;

    @Value("${QuestService.debugMode:false}")
    private boolean debugMode = false;

    private boolean enabled = true;

    @Override
    public int id() {
        return 1;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String progress(QuestEntity entity) {
        return Arrays.toString(entity.q1.awardSlots);
    }

    @Override
    public void start(UserProfile profile, QuestEntity entity) {
        Data data = entity.q1;
        data.startDate = new Date();
        data.awardSlots = new byte[awardSlotsCount];
    }

    @Override
    public boolean canStart(QuestEntity entity) {
        return entity.q1.startDate == null;
    }

    @Override
    public void consumeBattleResult(UserProfile profile, QuestEntity entity, PvpBattleResult battleResult) {
        if(battleResult == PvpBattleResult.WINNER) {
            Data data = entity.q1;
            for(int i = 0; i < data.awardSlots.length; i++) {
                //Всего у игрока 4 слота под сундуки. Сундук выдается игроку за каждую победу на ставках, если есть свободный слот.
                if(data.awardSlots[i] == 0) {
                    data.awardSlots[i] = (byte) awardFactory.getGenericAward().key;
                    data.updateDate = new Date();
                    entity.dirty = true;
                    break;
                }
            }
        }
    }

    @Override
    public Tuple2<ShopResultEnum, List<GenericAwardStructure>> reward(UserProfile profile, QuestEntity entity, int rewardId) {
        Data data = entity.q1;
        for(int i = 0; i < data.awardSlots.length; i++) {
            if(data.awardSlots[i] == rewardId) {
                GenericAward award = awardFactory.getAwardsMap().get((int) data.awardSlots[i]);
                int rewardPriceInBattles = award.price;
                if(profile.getBattlesCount() < rewardPriceInBattles) {
                    log.error("quest [{}] no enough battles! profile.battlesCount={}", id(), profile.getBattlesCount());
                    return Tuple.of(ShopResultEnum.NOT_ENOUGH_MONEY, Collections.emptyList());
                } else {
                    battleService.decBattleCount(profile, rewardPriceInBattles);
                    List<GenericAwardStructure> result = profileBonusService.awardProfile(award, profile, AwardTypeEnum.QUEST_FINISH,
                            Param.battles, -rewardPriceInBattles,
                            "quest", id(),
                            "rewardId", rewardId,
                            "awardSlots", Arrays.toString(data.awardSlots)
                    );
                    data.awardSlots[i] = 0;
                    entity.dirty = true;
                    return Tuple.of(ShopResultEnum.SUCCESS, result);
                }
            }
        }
        log.error("quest [{}]: rewardId {} nof found! awardSlots={}", id(), rewardId, Arrays.toString(data.awardSlots));
        return Tuple.of(ShopResultEnum.ERROR, Collections.emptyList());
    }

    @Override
    public boolean isRewarded(QuestEntity questEntity) {
        return false;
    }

    public void setAwardFactory(GenericAwardFactory awardFactory) {
        this.awardFactory = awardFactory;
    }

    public GenericAwardFactory getAwardFactory() {
        return awardFactory;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setAwardSlotsCount(int awardSlotsCount) {
        this.awardSlotsCount = awardSlotsCount;
    }

}
