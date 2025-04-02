package com.pragmatix.app.messages.client;

import com.pragmatix.app.messages.structures.EndBattleStructure;
import com.pragmatix.gameapp.social.SocialServiceEnum;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.03.2016 11:36
 */
public interface ILogin<T> {

    String LOCALE = "locale";

    String REFERRER_PARAM_NAME = "referrerKey";

    String FLASH_VERSION_PARAM_NAME = "flv";

    String REFERRAL_LINK_TOKEN = "rlt";

    String DEBUG_LOGIN_AWARDS = "debug_login_awards";

    String MOVE_PROFILE = "move_profile";

    String RECONNECT_TO_BATTLE = "reconnect_to_battle";

    String MISSION_ID = "mission_id";

    String LAST_TURN_TUM = "last_turn_num";

    T getId();

    String getAuthKey();

    int getVersion();

    SocialServiceEnum getSocialNet();

    String[] getParams();

    EndBattleStructure[] getOfflineBattles();

    int getFriendsCount();
}
