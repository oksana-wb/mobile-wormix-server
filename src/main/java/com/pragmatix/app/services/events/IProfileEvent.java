package com.pragmatix.app.services.events;

import com.pragmatix.app.model.UserProfile;

/**
 * Created: 27.04.11 11:50
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public interface IProfileEvent<T> {

    T runEvent(UserProfile profile);

}