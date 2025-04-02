package com.pragmatix.app.messages.client;

import com.pragmatix.app.messages.structures.SelectStuffStructure;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * Одеть себя и(или) команду
 *
 * @see com.pragmatix.app.controllers.StuffController#onSelectStuff(SelectStuffs, com.pragmatix.app.model.UserProfile)
 */
@Command(25)
public class SelectStuffs {

    public SelectStuffStructure[] selectStuffs;

    public SelectStuffs() {
    }

    public SelectStuffs(int profileId, int hatId, int kitId) {
        this.selectStuffs = new SelectStuffStructure[]{new SelectStuffStructure(profileId, (short) hatId, (short) kitId)};
    }

    @Override
    public String toString() {
        return "SelectStuffs{" +
                Arrays.toString(selectStuffs) +
                '}';
    }

}
