package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.StuffService;
import com.pragmatix.common.utils.AppUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 02.10.12 16:51
 */
public class AddTemporalStuffEvent implements IProfileEvent<GenericAwardStructure> {

    private int stuffId;

    // выдать предмет на количество часов
    private int expireHours;

    // выдать предмет до конкретного времени
    private int expireTimeInSeconds;

    private  boolean setStuff;

    private StuffService stuffService;
    
    private boolean expand;

    public AddTemporalStuffEvent(int stuffId, int expireHours, int expireTimeInSeconds, boolean setStuff,  boolean expand, StuffService stuffService) {
        this.stuffId = stuffId;
        this.expireHours = expireHours;
        this.expireTimeInSeconds = expireTimeInSeconds;
        this.setStuff = setStuff;
        this.expand = expand;
        this.stuffService = stuffService;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        int expireTimeInSeconds = this.expireTimeInSeconds;
        if(profile != null)
            if(expireHours > 0){
                stuffService.addStuff(profile, (short) stuffId, expireHours, TimeUnit.HOURS, setStuff, expand);
                expireTimeInSeconds = AppUtils.currentTimeSeconds() + (int) TimeUnit.HOURS.toSeconds(expireHours);
            } else if(expireTimeInSeconds > 0){
                stuffService.addStuffUntilTime(profile, (short) stuffId, expireTimeInSeconds, setStuff);
            } else {
                stuffService.addOrExpandTemporalStuff(profile, (short) stuffId);
            }
        return new GenericAwardStructure(AwardKindEnum.TEMPORARY_STUFF, expireTimeInSeconds, stuffId);
    }

}
