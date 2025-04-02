package com.pragmatix.arena.mercenaries.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.02.2016 13:58
 *         @see com.pragmatix.arena.mercenaries.MercenariesController#onGetMercenariesDefs(GetMercenariesDefs, UserProfile)
 */
@Command(124)
public class GetMercenariesDefs {

    @Override
    public String toString() {
        return "GetMercenariesDefs{}";
    }
}
