package com.pragmatix.app.messages.client;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.11.2016 11:14
 * @see com.pragmatix.app.controllers.ProfileController#onSetCookies(SetCookies, UserProfile)
 */
@Command(136)
public class SetCookies {

    public String[] names = ArrayUtils.EMPTY_STRING_ARRAY;

    public String[] values = ArrayUtils.EMPTY_STRING_ARRAY;

    public SetCookies() {
    }

    public SetCookies(String[] names, String[] values) {
        this.names = names;
        this.values = values;
    }

    @Override
    public String toString() {
        Map<String, String> map = new TreeMap<>();
        for(int i = 0; i < names.length; i++) {
            map.put(names[i], values[i]);

        }
        return "SetCookies{" + map + '}';
    }
}
