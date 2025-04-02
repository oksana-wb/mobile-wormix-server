package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.common.utils.AppUtils;

/**
 * Created: 27.04.11 11:52
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class AddReactionRateEvent implements IProfileEvent<GenericAwardStructure> {

    private int minRate;
    private int maxRate;

    public AddReactionRateEvent(int minRate, int maxRate) {
        this.minRate = minRate;
        this.maxRate = maxRate;
    }

    public AddReactionRateEvent(int rate) {
        this.minRate = rate;
        this.maxRate = rate;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        int reactionRate = maxRate > minRate ? minRate + AppUtils.generateRandom(maxRate - minRate + 1) : minRate;
        if(profile != null) profile.setReactionRate(profile.getReactionRate() + reactionRate);
        return new GenericAwardStructure(AwardKindEnum.REACTION_RATE, reactionRate);
    }

}