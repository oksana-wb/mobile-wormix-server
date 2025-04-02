package com.pragmatix.notify;

import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.gameapp.social.SocialServiceId;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.08.13 15:17
 */
public interface NotifySenderI {

    boolean send(String registrationId, int timeToLive, String localizedMessage);

    SocialServiceEnum getSocialNetId();

}
