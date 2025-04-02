package com.pragmatix.app.messages.client;

import com.pragmatix.app.messages.structures.EndBattleStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.authorize.LoginService;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.gameapp.secure.SecuredLogin;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Arrays;
import java.util.List;

/**
 * @see com.pragmatix.app.filters.AuthFilter#authenticate(Login)
 * @see LoginService#onSuccessAuthorize(String, UserProfile, List, long, ILogin, boolean)
 * @see com.pragmatix.app.controllers.LoginController#onLogin(Object, UserProfile)
 */
@Command(1)
public class Login extends SecuredLogin implements ILogin<Long> {
    /**
     * id пользователя
     */
    @Resize(TypeSize.UINT32)
    public Long id;
    /**
     * id друга который пригласил в игру
     */
    @Resize(TypeSize.UINT32)
    public long referrerId;
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
    @Resize(TypeSize.UINT32)
    public List<Long> ids;

    @Resize(TypeSize.BYTE)
    public SocialServiceEnum socialNet;

    public String[] params;

    @Ignore
    public EndBattleStructure[] offlineBattles = LoginByProfileStringId.EMPTY_ARR;

    public Login() {
    }

    @Override
    public String toString() {
        return "Login{" +
                "id=" + id +
                ", socialNet=" + socialNet +
                (referrerId > 0 ?  ", referrerId=" + referrerId : "")+
                ", version='" + AppParams.versionToString(version) + '\'' +
//                ", authKey='" + authKey + '\'' +
                (params != null && params.length > 0 ? ", params=" + Arrays.toString(params) : "") +
                (offlineBattles != null && offlineBattles.length > 0 ? ", offlineBattles=" + Arrays.toString(offlineBattles) : "") +
//                ", secureResult=" + secureResult +
//                ", ids=" + (ids == null ? null : Arrays.asList(ids)) +
                '}';
    }

    @Override
    public Long getId() {
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
}
