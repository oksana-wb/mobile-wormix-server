package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.DailyRegistry;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class AddWagerWinAwardTokenEvent implements IProfileEvent<GenericAwardStructure> {

    private DailyRegistry dailyRegistry;

    private int wagerWinAwardToken;

    public AddWagerWinAwardTokenEvent(DailyRegistry dailyRegistry, int wagerWinAwardToken) {
        this.dailyRegistry = dailyRegistry;
        this.wagerWinAwardToken = wagerWinAwardToken;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        if(profile != null) dailyRegistry.addWagerWinAwardToken(profile.getProfileId(), wagerWinAwardToken);
        return new GenericAwardStructure(AwardKindEnum.WAGER_AWARD_TOKEN, wagerWinAwardToken);
    }

}