package com.pragmatix.app.services.authorize;

import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.server.EnterAccount;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.common.utils.VarInt;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.05.2016 14:06
 */
public interface SuccessAuthorizeObserver {

    void beforeSendEnterAccount(UserProfile profile, ILogin login, EnterAccount enterAccount, ArrayList<LoginAwardStructure> loginAwards);

    void fillLoginAwards(UserProfile profile, long referrerId, String[] params, boolean isNewProfile, List<LoginAwardStructure> loginAwards, VarInt firstLoginByDay);

    void afterSentEnterAccount(UserProfile profile);

}
