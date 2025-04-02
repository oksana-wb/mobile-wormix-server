package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileExperienceService;

/**
 * Created: 27.04.11 11:51
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class AddExperienceEvent implements IProfileEvent<GenericAwardStructure> {

    private int exp;
    private int boostFactor;
    private ProfileExperienceService profileExperienceService;

    public AddExperienceEvent(int exp, int boostFactor, ProfileExperienceService profileExperienceService) {
        this.exp = exp;
        this.boostFactor = boostFactor;
        this.profileExperienceService = profileExperienceService;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        int awardExp = exp * boostFactor;
        if(profile != null) {
            profileExperienceService.addExperience(profile, awardExp);
        }
        return new GenericAwardStructure(AwardKindEnum.EXPERIENCE, awardExp).setBoostFactor(boostFactor);
    }

}