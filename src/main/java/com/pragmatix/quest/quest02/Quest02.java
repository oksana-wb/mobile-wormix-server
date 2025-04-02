package com.pragmatix.quest.quest02;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.common.ItemType;
import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.BattleService;
import com.pragmatix.app.services.ProfileBonusService;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.services.StatisticService;
import com.pragmatix.app.settings.GenericAwardProducer;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.quest.QuestDef;
import com.pragmatix.quest.dao.QuestEntity;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.12.2015 9:58
 *         <p>
 *         Гонки на веревках:
 *         - когда игрок побеждает в гонке первый раз за день, он получает награду
 *         - чтобы открыть награду, игрок должен заплатить 3 жетона
 *         - после выдачи награды можно играть в гонки сколько угодно раз без ограничений (даже если игрок не открыл награду) *
 */
public class Quest02 implements QuestDef {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private BattleService battleService;

    @Resource
    private StatisticService statisticService;

    private List<GenericAwardProducer> finishAward;

    private int finishWinCount = 1;

    private int rewardPriceInBattles = 3;

    @Value("${QuestService.debugMode:false}")
    private boolean debugMode = false;

    private boolean enabled = true;

    @Override
    public int id() {
        return 2;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String progress(QuestEntity entity) {
        Data data = entity.q2;
        return String.format("{\"finishWin\": %s, \"win\": %s}", finishWinCount, data.win);
    }

    @Override
    public void start(UserProfile profile, QuestEntity entity) {
        Data data = entity.q2;
        data.finishedDate = null;
        data.startDate = new Date();
        data.win = (short) 0;
        data.rewarded = false;
        entity.dirty = true;
    }

    @Override
    public boolean canStart(QuestEntity entity) {
        Data data = entity.q2;
        if(debugMode) {
            return data.win >= finishWinCount;
        }
        if(data.finishedDate == null) {
            return data.startDate == null;
        } else {
            Calendar cal = Calendar.getInstance();
            Calendar calFinish = Calendar.getInstance();
            calFinish.setTime(data.finishedDate);
            return cal.get(Calendar.DAY_OF_YEAR) != calFinish.get(Calendar.DAY_OF_YEAR);
        }
    }

    @Override
    public void consumeBattleResult(UserProfile profile, QuestEntity entity, PvpBattleResult battleResult) {
        if(battleResult == PvpBattleResult.WINNER) {
            Data data = entity.q2;
            data.win++;
            if(data.win == finishWinCount) {
                data.finishedDate = new Date();
            }
            entity.dirty = true;
        }
    }

    @Override
    public Tuple2<ShopResultEnum, List<GenericAwardStructure>> reward(UserProfile profile, QuestEntity entity, int rewardId) {
        List<GenericAwardStructure> result = Collections.emptyList();
        ShopResultEnum resultEnum = ShopResultEnum.ERROR;
        Data data = entity.q2;
        if(data.finishedDate == null) {
            log.error("quest [2] in progress!");
        } else if(data.rewarded) {
            log.error("quest [2] profile already rewarded!");
        } else if(profile.getBattlesCount() < rewardPriceInBattles) {
            log.error("quest [2] no enough battles! profile.battlesCount={}", profile.getBattlesCount());
            resultEnum = ShopResultEnum.NOT_ENOUGH_MONEY;
        } else {
            battleService.decBattleCount(profile, rewardPriceInBattles);
            result = profileBonusService.awardProfile(finishAward, profile, AwardTypeEnum.QUEST_FINISH,
                    Param.battles, -rewardPriceInBattles,
                    "quest", id(),
                    "started", data.startDate
            );
            statisticService.buyItemStatistic(profile.getId(), 2, rewardPriceInBattles, ItemType.QUEST_REWARD, 1, id(), profile.getLevel());

            data.rewarded = true;
            resultEnum = ShopResultEnum.SUCCESS;
            entity.dirty = true;
        }
        return Tuple.of(resultEnum, result);
    }

    public void setFinishAward(List<GenericAwardProducer> finishAward) {
        this.finishAward = finishAward;
    }

    public List<GenericAwardProducer> getFinishAward() {
        return finishAward;
    }

    public void setFinishWinCount(int finishWinCount) {
        this.finishWinCount = finishWinCount;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setRewardPriceInBattles(int rewardPriceInBattles) {
        this.rewardPriceInBattles = rewardPriceInBattles;
    }

    public int getFinishWinCount() {
        return finishWinCount;
    }

    public int getRewardPriceInBattles() {
        return rewardPriceInBattles;
    }

    @Override
    public boolean isRewarded(QuestEntity questEntity) {
        return questEntity.q2.rewarded;
    }
}
