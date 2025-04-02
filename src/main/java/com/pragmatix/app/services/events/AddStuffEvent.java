package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.StuffService;

import java.util.concurrent.TimeUnit;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 02.10.12 16:51
 */
public class AddStuffEvent implements IProfileEvent<GenericAwardStructure> {

    private int stuffId;

    private boolean setStuff = true;

    private StuffService stuffService;

    public AddStuffEvent(int stuffId, StuffService stuffService) {
        this.stuffId = stuffId;
        this.stuffService = stuffService;
    }

    public AddStuffEvent(int stuffId, StuffService stuffService, boolean setStuff) {
        this.stuffId = stuffId;
        this.stuffService = stuffService;
        this.setStuff = setStuff;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        if(profile != null) stuffService.addStuff(profile, (short) stuffId, setStuff);
        return new GenericAwardStructure(AwardKindEnum.STUFF, -1, stuffId);
    }

}
