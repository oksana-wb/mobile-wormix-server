package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;

/**
 * Created: 27.04.11 11:52
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class AddRealMoneyEvent implements IProfileEvent<GenericAwardStructure> {

    private int realMoney;

    public AddRealMoneyEvent(int realMoney) {
        this.realMoney = realMoney;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        if(profile != null) profile.setRealMoney(profile.getRealMoney() + realMoney);
        return new GenericAwardStructure(AwardKindEnum.REAL_MONEY, realMoney);
    }

}