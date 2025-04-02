package com.pragmatix.steam.web.request;


import com.pragmatix.steam.SteamAPIInterface;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 07.03.2017 16:14
 *         <p>
 * Запрос на получение информации о пользователе
 *
 * @see <a href="https://partner.steamgames.com/documentation/MicroTxn#GetUserInfo">MicroTxn#GetUserInfo</a>
 * @see com.pragmatix.df.server.social.steam.responses.GetUserInfoResponse
 */
public class GetUserInfoRequest extends SteamWebRequest {

    /**
     * SteamID of client
     */
    public String steamid;

    public GetUserInfoRequest(String steamid) {
        this.steamid = steamid;
    }

    @Override
    public SteamAPIInterface.Method getMethod() {
        return SteamAPIInterface.Method.GetUserInfo;
    }

    @Override
    public Map<String, String> toMap(int appId) {
        Map<String, String> res = new LinkedHashMap<>();
        res.put("steamid", steamid);
        return res;
    }


    @Override
    public String toString() {
        return "GetUserInfoRequest{" +
                "steamid='" + steamid + '\'' +
                "}";
    }
}
