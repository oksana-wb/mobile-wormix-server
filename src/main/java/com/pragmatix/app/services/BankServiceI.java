package com.pragmatix.app.services;

import com.pragmatix.gameapp.social.SocialServiceId;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.08.12 18:16
 */
public interface BankServiceI {

    SocialServiceId getSocialId();

    int getMoneByVoites(int voites);

    int getRealMoneByVoites(int voites);

}
