package com.pragmatix.intercom.messages;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 29.05.12 16:56
 */
public interface IntercomResponseI {

    long getProfileId();

    byte getSocialNetId();

    long getRequestId();

}
