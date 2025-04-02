package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;

public class AddRenameEvent implements IProfileEvent<GenericAwardStructure> {

    private final int rename;

    public AddRenameEvent(int rename) {
        this.rename = rename;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        if (profile != null) {
            profile.setRenameAct(profile.getRenameAct() + rename);
        }
        return new GenericAwardStructure(AwardKindEnum.RENAME, rename);
    }

}