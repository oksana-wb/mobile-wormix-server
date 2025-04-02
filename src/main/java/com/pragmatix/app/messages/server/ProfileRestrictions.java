package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.RestrictionItemStructure;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * Оповещение игрока о наложенных на него ограничениях
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 02.12.2016 13:59
 */
@Command(10138)
public class ProfileRestrictions {

    public RestrictionItemStructure[] restrictions;

    public ProfileRestrictions() {
    }

    public ProfileRestrictions(RestrictionItemStructure[] restrictions) {
        this.restrictions = restrictions;
    }

    @Override
    public String toString() {
        return "ProfileRestrictions{" +
                Arrays.toString(restrictions) +
                '}';
    }

}
