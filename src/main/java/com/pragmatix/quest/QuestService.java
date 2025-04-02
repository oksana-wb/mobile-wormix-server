package com.pragmatix.quest;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileEventsService;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.craft.domain.ReagentsEntity;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.quest.dao.QuestDao;
import com.pragmatix.quest.dao.QuestEntity;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.END_QUEST_BATTLE;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.12.2015 9:11
 */
@Service
public class QuestService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private QuestDao dao;

    @Resource
    private ProfileService profileService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Autowired(required = false)
    private List<QuestDef> registeredQuests = new ArrayList<>();

    public QuestEntity getQuestEntity(final UserProfile profile) {
        if(profile.getQuestEntity() == null) {
            synchronized (profile) {
                if(profile.getQuestEntity() == null) {
                    Long profileId = profile.getId();
                    QuestEntity entity = dao.get(profileId);
                    if(entity == null)
                        entity = dao.newEntity(profileId);
                    profile.setQuestEntity(entity);
                }
            }
        }
        return profile.getQuestEntity();
    }

    public List<QuestProgressStructure> questsProgress(UserProfile profile) {
        List<QuestProgressStructure> result = new ArrayList<>(registeredQuests.size());
        QuestEntity questEntity = getQuestEntity(profile);
        for(QuestDef registeredQuest : registeredQuests) {
            if(registeredQuest.isEnabled())
                result.add(new QuestProgressStructure(registeredQuest.id(), registeredQuest.canStart(questEntity), registeredQuest.progress(questEntity), registeredQuest.isRewarded(questEntity)));
        }
        return result;
    }

    public void persistEntity(QuestEntity entity) {
        if(entity != null)
            dao.persist(entity);
    }

    public void consumeBattleResult(UserProfile profile, short questId, PvpBattleResult battleResult, String battleTime) {
        consumeBattleResult(profile, questId, battleResult, null, null, battleTime);
    }

    public void consumeBattleResult(UserProfile profile, short questId, PvpBattleResult battleResult, byte[] collectedReagents, PvpBattleType battleType, String battleTime) {
        getQuestDef(questId).ifPresent(registeredQuest -> {
            QuestEntity questEntity = getQuestEntity(profile);
            if(registeredQuest.canStart(questEntity))
                registeredQuest.start(profile, questEntity);
            registeredQuest.consumeBattleResult(profile, questEntity, battleResult);

            if(collectedReagents != null && battleType != null)
                profileEventsService.fireProfileEventAsync(END_QUEST_BATTLE, profile,
                        "questId", questId,
                        "battleType", battleType,
                        Param.result, battleResult,
                        Param.battleTime, battleTime,
                        Param.reagents, ReagentsEntity.getReagentValues(collectedReagents)

                );
        });
    }

    public Tuple2<ShopResultEnum, List<GenericAwardStructure>> reward(final UserProfile profile, int questId, int rewardId) {
        return getQuestDef(questId).map(registeredQuest -> {
            QuestEntity questEntity = getQuestEntity(profile);
            Tuple2<ShopResultEnum, List<GenericAwardStructure>> result = registeredQuest.reward(profile, questEntity, rewardId);
            if(result._2.size() > 0) {
                profileService.updateAsync(profile);
            }
            return result;
        }).orElse(Tuple.of(ShopResultEnum.ERROR, Collections.emptyList()));
    }

    public boolean isQuestEnabled(int questId) {
        return getQuestDef(questId).map(QuestDef::isEnabled).orElse(false);
    }

    public Optional<QuestDef> getQuestDef(int questId) {
        for(QuestDef registeredQuest : registeredQuests) {
            if(registeredQuest.id() == questId) {
                if(registeredQuest.isEnabled()) {
                    return Optional.of(registeredQuest);
                } else {
                    log.warn("quest [id:{}] is disabled", questId);
                    return Optional.empty();
                }
            }
        }
        log.error("quest not found by id [{}]", questId);
        return Optional.empty();
    }

    public void wipeQuestsState(UserProfile profile) {
        dao.deleteById(profile.getId());
        profile.setQuestEntity(null);
    }

}
