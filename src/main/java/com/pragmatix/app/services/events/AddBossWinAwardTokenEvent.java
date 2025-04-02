package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.DailyRegistry;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class AddBossWinAwardTokenEvent implements IProfileEvent<GenericAwardStructure> {

    private DailyRegistry dailyRegistry;

    private int bossWinAwardToken;

    public AddBossWinAwardTokenEvent(DailyRegistry dailyRegistry, int bossWinAwardToken) {
        this.dailyRegistry = dailyRegistry;
        this.bossWinAwardToken = bossWinAwardToken;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        if(profile != null) dailyRegistry.addBossWinAwardToken(profile.getProfileId(), bossWinAwardToken);
        return new GenericAwardStructure(AwardKindEnum.BOSS_AWARD_TOKEN, bossWinAwardToken);
    }

}