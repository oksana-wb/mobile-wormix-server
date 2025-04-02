package com.pragmatix.app.services;

import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.achieve.services.AchieveCommandService;
import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.dao.BossBattleExtraRewardDao;
import com.pragmatix.app.domain.BossBattleExtraRewardEntity;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.settings.BossBattleSettings;
import com.pragmatix.app.settings.GenericAward;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;

@Service
public class BossBattleExtraRewardService {

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private AchieveCommandService achieveService;

    @Resource
    private BossBattleExtraRewardDao extraRewardDao;

    private volatile Map<Short, Function<UserProfile, List<GenericAward>>> bossBattleExtraReward = Map.of();

    public void loadReward() {
        bossBattleExtraReward = extraRewardDao.getAllList().stream()
                .filter(it -> !it.isArchive())
                .map(this::fillReward)
                .collect(Collectors.groupingBy(BossBattleExtraRewardEntity::getMissionId))
                .entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        it -> new RewardFactory(it.getValue())
                ));
    }

    private BossBattleExtraRewardEntity fillReward(BossBattleExtraRewardEntity entity) {
        var reward = new GenericAward();
        var joiner = new StringJoiner(", ");
        if (entity.getReaction() > 0) {
            joiner.add("reaction=" + entity.getReaction());
        }
        if (!entity.getReagents().isEmpty()) {
            joiner.add("reagents=" + entity.getReagents());
        }
        if (!entity.getWeapons().isEmpty()) {
            joiner.add("weapons=" + entity.getWeapons());
        }
        if (!entity.getStuff().isEmpty()) {
            joiner.add("stuff=" + entity.getStuff());
        }
        reward.setName("missionId=%d, id=%d%s%s".formatted(entity.getMissionId(), entity.getId(), joiner.length() > 0 ? " => " : "", joiner.toString()));
        reward.setMoney(entity.getMoney());
        reward.setRealMoney(entity.getRealMoney());
        reward.setReactionRate(entity.getReaction());
        reward.setReagentsStr(entity.getReagents());
        profileBonusService.setAwardItems(entity.getWeapons(), reward.getAwardItems());
        profileBonusService.setAwardItems(entity.getStuff(), reward.getAwardItems());

        entity.reward = reward;
        return entity;
    }

    public void onEndBattleGrandReward(UserProfile profile, short missionId, BossBattleSettings bossBattleSettings, List<GenericAwardStructure> award) {
//        if (bossBattleSettings.extraAward() != null) {
//            var extraAward = bossBattleSettings.extraAward().get();
//            if (extraAward != null) {
//                award.addAll(profileBonusService.grantBossBattleItemsAward(profile, List.of(extraAward)));
//
//                int index = WormixAchievements.AchievementName.boss_extra_reward.getIndex();
//                achieveService.increaseAchievement(profile, index);
//            }
//        }
        var extraReward = bossBattleExtraReward.getOrDefault(missionId, p -> List.of()).apply(profile);
        if (extraReward.isEmpty()) {
            return;
        }
        extraReward.forEach(genericAward -> award.addAll(
                profileBonusService.awardProfile(genericAward, profile, AwardTypeEnum.BOSS_BATTLE_EXTRA, genericAward.getName())
        ));

//        int index = WormixAchievements.AchievementName.boss_extra_reward.getIndex();
//        achieveService.increaseAchievement(profile, index);
    }

    private static class RewardFactory implements Function<UserProfile, List<GenericAward>> {
        private final Random random = new Random();
        private final List<BossBattleExtraRewardEntity> source;

        private RewardFactory(List<BossBattleExtraRewardEntity> source) {
            this.source = source;
        }

        @Override
        public List<GenericAward> apply(UserProfile profile) {
            return source.stream()
                    .filter(it -> (it.getStart() == null || now().isAfter(it.getStart()))
                            && (it.getFinish() == null || now().isBefore(it.getFinish()))
                    )
                    .filter(it -> profile.getLevel() >= it.getLevelFrom() && profile.getLevel() <= it.getLevelTo())
                    .filter(it -> {
                        if (it.getChance() >= 100) {
                            return true;
                        } else {
                            return random.nextInt(1, 101) <= it.getChance();
                        }
                    })
                    .map(it -> it.reward)
                    .toList();
        }
    }

    public Map<Short, Function<UserProfile, List<GenericAward>>> bossBattleExtraReward() {
        return bossBattleExtraReward;
    }
}
