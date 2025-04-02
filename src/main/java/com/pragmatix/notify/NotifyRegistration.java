package com.pragmatix.notify;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.08.13 14:52
 *
 * @see NotifyRegistrationLoader
 */
public class NotifyRegistration {

    public final String registrationId;

    public final short socialNetId;

    public NotifyRegistration(String registrationId, short socialNetId) {
        this.registrationId = registrationId;
        this.socialNetId = socialNetId;
    }

}
