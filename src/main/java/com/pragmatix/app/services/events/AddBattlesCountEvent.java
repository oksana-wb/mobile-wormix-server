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
public class AddBattlesCountEvent implements IProfileEvent<GenericAwardStructure> {

    private BattleService battleService;

    private int battlesCount;

    public AddBattlesCountEvent(BattleService battleService, int battlesCount) {
        this.battleService = battleService;
        this.battlesCount = battlesCount;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        if(profile != null){
            // начисляем нужное количество битв если пришло время
            battleService.checkBattleCount(profile);

            profile.setBattlesCount(profile.getBattlesCount() + battlesCount);
        }
        return new GenericAwardStructure(AwardKindEnum.BATTLES_COUNT, battlesCount);
    }

}