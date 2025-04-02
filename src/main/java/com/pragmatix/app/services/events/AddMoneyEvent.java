package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;

/**
 * Created: 27.04.11 11:51
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class AddMoneyEvent implements IProfileEvent<GenericAwardStructure> {

    private int money;
    private int boostFactor;

    public AddMoneyEvent(int money, int boostFactor) {
        this.money = money;
        this.boostFactor = boostFactor;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        int awardMoney = money * boostFactor;
        if(profile != null) {
            profile.setMoney(profile.getMoney() + awardMoney);
        }
        return new GenericAwardStructure(AwardKindEnum.MONEY, awardMoney).setBoostFactor(boostFactor);
    }

}