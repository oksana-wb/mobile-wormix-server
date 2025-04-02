package com.pragmatix.arena.mercenaries.messages;

import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.serialization.annotations.Structure;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.02.2016 16:18
 */
@Structure
public class BackpackItemShortStruct {
    public short weaponId;
    public short count;

    public BackpackItemShortStruct() {
    }

    public BackpackItemShortStruct(short weaponId, short count) {
        this.weaponId = weaponId;
        this.count = count;
    }

    public BackpackItemShortStruct(BackpackItem item) {
        this.weaponId = (short) item.getWeaponId();
        this.count = (short) item.getCount();
    }

    @Override
    public String toString() {
        return "{" + weaponId + ":" + count + '}';
    }
}
