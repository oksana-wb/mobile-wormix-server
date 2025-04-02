package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.03.14 8:47
 *
 * @see com.pragmatix.app.controllers.ProfileController#onSetName(SetName, com.pragmatix.app.model.UserProfile)
 */
@Command(104)
public class SetName {

    public String name;

    @Override
    public String toString() {
        return "SetName{" +
                "name='" + name + '\'' +
                '}';
    }

}
