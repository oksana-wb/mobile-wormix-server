package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.messages.RestrictionItemStructure;
import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.structures.DepositStructure;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.messages.structures.ReconnectToSimpleBattleResultStructure;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.FriendsListService;
import com.pragmatix.clanserver.messages.structures.InviteStructure;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Serialize;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * команда посылается с сервера если пользователь
 * был успешно авторизован
 * <p/>
 * User: denis
 * Date: 09.11.2009
 * Time: 22:03:01
 *
 * @see com.pragmatix.app.services.authorize.SuccessAuthorizeObserver#beforeSendEnterAccount(UserProfile, ILogin, EnterAccount, java.util.ArrayList)
 * @see com.pragmatix.app.services.authorize.LoginService#fillEnterAccount(EnterAccount, String, UserProfile, FriendsListService.FirstPageBean, List)
 * @see com.pragmatix.app.controllers.LoginController#onLogin(Object, com.pragmatix.app.model.UserProfile)
 */
@Command(10001)
public class EnterAccount implements SecuredResponse {

    public UserProfileStructure userProfileStructure;
    /**
     * список профайлов друзей
     */
    public List<UserProfileStructure> profileStructures;
    /**
     * награды выдаваемые при логине в игру
     */
    public List<LoginAwardStructure> loginAwards;
    /**
     * количество друзей online в момент формирования списка друзей
     */
    public short onlineFriends;
    /**
     * количество друзей учитываемых сервером
     */
    public short friends;

    public String sessionKey;
    /**
     * количество доступных обысков друзей
     */
    public byte availableSearchKeys;
    /**
     * реагенты для рецептов
     */
    public int[] reagents;
    /**
     * id последней пройденной миссии
     */
    public short currentMission;
    /**
     * id последней пройденной новой миссии
     */
    public short currentNewMission;

    public InviteStructure[] invites;
    /**
     * количество доступных переименований
     */
    @Serialize(ifExpr = "com.pragmatix.app.settings.AppParams.IS_MOBILE()")
    public byte renameAct;
    /**
     * время сервера в секундах
     */
    public int serverTime;

    public short[] backpackConf1;

    public short[] backpackConf2;

    public short[] backpackConf3;

    public byte activeBackpackConf;

    public short[] hotkeys;

    public byte loginSequence;

    public short races;

    public int selectRaceTime;

    @Serialize(ifExpr = "com.pragmatix.app.settings.AppParams.IS_MOBILE()")
    public DepositStructure[] deposits;

    public byte[] skins;

    public int lastPaymentTime;

    public RestrictionItemStructure[] restrictions;

    public String[] cookies;

    public int vipSubscriptionId;

    public ReconnectToSimpleBattleResultStructure reconnectResult;

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "EnterAccount{" +
                userProfileStructure +
                (races != 0 ? ", races=" + Race.toList(races) : "") +
                (ArrayUtils.isNotEmpty(skins) ? ", skins=" + Arrays.toString(skins) : "") +
//                (availableSearchKeys > 0 ? ", availableSearchKeys=" + availableSearchKeys : "") +
                (lastPaymentTime > 0 ? ", lastPaymentTime=" + AppUtils.formatDateInSeconds(lastPaymentTime) : "") +
                (CollectionUtils.isNotEmpty(loginAwards) ? ", loginAwards=" + loginAwards : "") +
                (ArrayUtils.isNotEmpty(cookies) ? ", cookies=" + toMap(cookies) : "") +
                ", profileStructures(" + CollectionUtils.size(profileStructures) + ")" +
                (ArrayUtils.isNotEmpty(invites) ? ", invites=" + Arrays.toString(invites) : "") +
                (ArrayUtils.isNotEmpty(restrictions) ? ", restrictions=" + Arrays.toString(restrictions) : "") +
                (ArrayUtils.isNotEmpty(deposits) ?  ", deposits=" + Arrays.toString(deposits) : "") +
                (vipSubscriptionId > 0 ? ", vipSubscriptionId=" + vipSubscriptionId : "") +
                (reconnectResult != null ? ", reconnectToSimpleBattleResultStructure=" + reconnectResult : "") +
//                ", loginSequence=" + loginSequence +
//                ", onlineFriends=" + onlineFriends +
//                ", friends=" + friends +
//                ", currentMission=" + currentMission +
//                ", currentNewMission=" + currentNewMission +
//                ", renameAct=" + renameAct +
//                (ArrayUtils.isNotEmpty(invites) ? ", invites=" + Arrays.toString(invites) : "") +
//                ", backpackConfs=[" + ArrayUtils.getLength(backpackConf1) + "," + ArrayUtils.getLength(backpackConf2) + "," + ArrayUtils.getLength(backpackConf3) + "]" +
//                ", hotkeys(" + ArrayUtils.getLength(hotkeys) + ")" +
                ", sessionKey=" + sessionKey +
                '}';
    }

    private Map<String, String> toMap(String[] pairArray) {
        Map<String, String> map = new TreeMap<>();
        for(int i = 0; i < pairArray.length; i += 2) {
            map.put(pairArray[i], pairArray[i + 1]);
        }
        return map;
    }

}
