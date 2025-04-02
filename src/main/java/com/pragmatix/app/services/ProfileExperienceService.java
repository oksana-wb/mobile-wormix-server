package com.pragmatix.app.services;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.common.TeamMemberType;
import com.pragmatix.app.init.LevelCreator;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.app.model.Level;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.app.settings.SimpleBattleSettings;
import com.pragmatix.gameapp.social.SocialService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 13.05.11 11:56
 */
@Service
public class ProfileExperienceService {

    @Resource
    private LevelCreator levelCreator;

    @Resource(name = "botsBattleAwardSettings")
    private SimpleBattleSettings awardSettings;

    @Resource
    private SearchTheHouseService searchTheHouseService;

    @Resource
    private SocialService socialService;

    @Resource
    private UserRegistryI userRegistry;

    @Resource
    private GroupService groupService;

    @Resource
    private ProfileService profileService;

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private BattleService battleService;

    /**
     * Добавить опыт к профайлу и если это необходимо, то увеличить уровени игрока
     *
     * @param profile профайл к которому необходимо
     * @param exp     количество опыта которое необходимо добавить
     */
    public boolean addExperience(UserProfile profile, int exp) {
        return addExperience(profile, exp, true);
    }

    /**
     * Добавить опыт к профайлу и если это необходимо, то увеличить уровени игрока
     *
     * @param profile    профайл к которому необходимо
     * @param exp        количество опыта которое необходимо добавить
     * @param canLevelUp может ли игрок перейти на след. уровень
     */
    public boolean addExperience(UserProfile profile, int exp, boolean canLevelUp) {
        if(exp > 0) {
            Level level = levelCreator.getLevel(profile.getLevel());
            profile.setExperience(profile.getExperience() + exp);
            // проверяем нужно или нет увеличить уровень игроку
            if(level != null && profile.getExperience() >= level.getNextLevelExp()) {
                if(canLevelUp) {
                    Level nextLevel = levelCreator.getLevel(profile.getLevel() + 1);
                    if(nextLevel != null) {
                        profile.setLevel(profile.getLevel() + 1);
                        onLevelUp(profile, level, nextLevel);
                        return true;
                    } else {
                        profile.setExperience(level.getNextLevelExp());
                    }
                } else {
                    profile.setExperience(level.getNextLevelExp() - 1);
                }
            }
        }
        return false;
    }

    public void onLevelUp(UserProfile profile, Level level, Level nextLevel) {
        profile.setLevelUpTime(new Date());

        userRegistry.updateLevel(profile);

        profile.setExperience(profile.getExperience() - level.getNextLevelExp());

        // даем возможность заработать на обыске, если достигнут "зачетный" уровень
        searchTheHouseService.fireLevelUp(profile);

        // оповещяем соц. сеть об увеличении уровня
        socialService.setUserLevel(profile, profile.getLevel(), result -> {/*do nothing*/});

        groupService.tryAddFreeMerchenary(profile, true);

        //восстанавливаем бои на арене после левел апа
        battleService.checkBattleCount(profile);
        int battlesCount = 0;
        if(profile.getBattlesCount() < BattleService.MAX_BATTLE_COUNT) {
            battlesCount = BattleService.MAX_BATTLE_COUNT - profile.getBattlesCount();
        }

        // выдаем награду за уровень
        GenericAward award = nextLevel.getAward().clone()
                .addBattles(battlesCount);
        profileBonusService.awardProfile(award, profile, AwardTypeEnum.LEVEL_UP, Param.note, "" + profile.getLevel());

        profileService.updateSync(profile);

        //если надо перестраиваем кешь группы т.к. он зависит от уровня игрока
        // InaccessibleClanMember теперь не используется
//        UserProfileStructure userProfileStructure = profile.getUserProfileStructure();
//        if(userProfileStructure != null) {
//            for(WormStructure wormStructure : userProfileStructure.wormsGroup()) {
//                if(wormStructure.teamMemberType == TeamMemberType.InaccessibleClanMember) {
//                    userProfileStructure.wormsGroup = profileService.createWormGroupStructures(profile);
//                    break;
//                }
//            }
//        }
    }

}
