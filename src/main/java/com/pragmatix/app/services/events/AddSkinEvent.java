package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.common.Race;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.SkinService;

/**
 * Created: 29.07.15 16:52
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class AddSkinEvent implements IProfileEvent<GenericAwardStructure> {

    private SkinService skinService;

    private byte skinId;

    private boolean setSkin;

    public AddSkinEvent(SkinService skinService, int skinId) {
        this.skinService = skinService;
        this.skinId = (byte) skinId;
    }

    public AddSkinEvent setSkin(boolean value){
        setSkin = value;
        return this;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        boolean addSkinResult  = true;
        if(profile != null) {
            addSkinResult = skinService.addSkin(profile, skinId, setSkin);
        }
        return addSkinResult ? new GenericAwardStructure(AwardKindEnum.SKIN, 1, skinId) : null;
    }

}