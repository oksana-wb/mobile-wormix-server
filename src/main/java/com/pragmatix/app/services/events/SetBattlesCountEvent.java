package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.BattleService;

/**
 * Created: 27.04.11 11:52
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class SetBattlesCountEvent implements IProfileEvent<GenericAwardStructure> {

    private int exactBattlesCount;

    public SetBattlesCountEvent(int exactBattlesCount) {
        this.exactBattlesCount = exactBattlesCount;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        if(profile != null) profile.setBattlesCount(exactBattlesCount + BattleService.MAX_BATTLE_COUNT);
        return new GenericAwardStructure(AwardKindEnum.BATTLES_COUNT, exactBattlesCount);
    }

}