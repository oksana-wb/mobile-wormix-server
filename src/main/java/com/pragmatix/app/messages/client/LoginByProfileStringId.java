package com.pragmatix.app.messages.client;

import com.pragmatix.app.messages.structures.EndBattleStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.authorize.LoginService;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.gameapp.secure.SecuredLogin;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;
import com.pragmatix.serialization.annotations.Serialize;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @see com.pragmatix.app.filters.AuthFilter#authenticateByStringProfileId(LoginByProfileStringId)
 * @see LoginService#onSuccessAuthorize(String, UserProfile, List, long, ILogin, boolean)
 * @see com.pragmatix.app.controllers.LoginController#onLogin(Object, UserProfile)
 */
@Command(20)
public class LoginByProfileStringId extends SecuredLogin implements ILogin<String> {

    public static final EndBattleStructure[] EMPTY_ARR = new EndBattleStructure[0];

    /**
     * id пользователя
     */
    public String id;

    /**
     * id друга который пригласил в игру
     */
    public String referrerId;

    /**
     * автризационный ключ
     */
    public String authKey;

    /**
     * версия приложения
     */
    public int version;

    /**
     * id профайлов которые нобходимо прислать
     */
    public List<String> ids;

    @Resize(TypeSize.BYTE)
    public SocialServiceEnum socialNet;

    public String[] params;

    @Serialize(ifExpr = "com.pragmatix.app.settings.AppParams.IS_MOBILE()")
    public EndBattleStructure[] offlineBattles = EMPTY_ARR;

    public LoginByProfileStringId() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getAuthKey() {
        return authKey;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public SocialServiceEnum getSocialNet() {
        return socialNet;
    }

    @Override
    public String[] getParams() {
        return params;
    }

    @Override
    public EndBattleStructure[] getOfflineBattles() {
        return offlineBattles;
    }

    @Override
    public int getFriendsCount() {
        return ids.size();
    }

    @Override
    public String toString() {
        return "Login{" +
                "stringId='" + id + '\'' +
                (StringUtils.isNoneEmpty(referrerId) ? ", referrerId=" + referrerId + '\'' : "") +
//                ", authKey='" + authKey + '\'' +
                ", version='" + AppParams.versionToString(version) + '\'' +
                ", socialNet=" + socialNet +
                (ArrayUtils.isNotEmpty(params) ? ", params=" + Arrays.toString(params) : "") +
                (ArrayUtils.isNotEmpty(offlineBattles) ? ", offlineBattles=" + Arrays.toString(offlineBattles) : "") +
//                ", secureResult=" + secureResult +
//                ", ids=" + (ids == null ? null : Arrays.asList(ids)) +
                '}';
    }

}
