package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.StuffService;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class AddBoosterEvent implements IProfileEvent<GenericAwardStructure> {

    private short boosterId;

    private StuffService stuffService;

    private int boostExpireTimeInSeconds;

    public AddBoosterEvent(int boosterId, int boostExpireTimeInSeconds, StuffService stuffService) {
        this.boosterId = (short) boosterId;
        this.boostExpireTimeInSeconds = boostExpireTimeInSeconds;
        this.stuffService = stuffService;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        int expireTimeInSeconds = 0;
        if(profile != null) {
            expireTimeInSeconds = stuffService.addBooster(profile, stuffService.getStuff(boosterId), boostExpireTimeInSeconds);
        }
        return new GenericAwardStructure(AwardKindEnum.TEMPORARY_STUFF, expireTimeInSeconds, boosterId);
    }

}
