package com.pragmatix.intercom.messages;

import com.pragmatix.achieve.common.GrantAwardResultEnum;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.intercom.structures.ProfileAchieveStructure;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.sessions.IAppServer;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 10.08.2016 10:50
 * @see com.pragmatix.intercom.controller.IntercomController#onIntercomAchieveResponse(IntercomAchieveResponse, IAppServer)
 */
@Command(5001)
public class IntercomAchieveResponse implements IntercomResponseI {

    public IntercomAchieveRequest.RequestType requestType;

    public String profileId;

    public long requestNum;

    public int resultEnumType = -1;

    public ProfileAchieveStructure profileAchievements;

    public static IntercomAchieveResponse GiveBonusItemResponse(IntercomAchieveRequest request, GrantAwardResultEnum grantAwardResult) {
        IntercomAchieveResponse response = new IntercomAchieveResponse();
        response.requestType = request.requestType;
        response.profileId = request.profileId;
        response.requestNum = request.requestNum;
        response.resultEnumType = grantAwardResult.getType();
        return response;
    }

    public static IntercomAchieveResponse BuyResetBonusItemsResponse(IntercomAchieveRequest request, ShopResultEnum shopResultEnum) {
        IntercomAchieveResponse response = new IntercomAchieveResponse();
        response.requestType = request.requestType;
        response.profileId = request.profileId;
        response.requestNum = request.requestNum;
        response.resultEnumType = shopResultEnum.getType();
        return response;
    }

    public static IntercomAchieveResponse GetAchievementsResponse(IntercomAchieveRequest request, ProfileAchieveStructure profileAchievements) {
        IntercomAchieveResponse response = new IntercomAchieveResponse();
        response.requestType = request.requestType;
        response.profileId = request.profileId;
        response.requestNum = request.requestNum;
        response.profileAchievements = profileAchievements;
        return response;
    }

    @Override
    public long getProfileId() {
        return 0;
    }

    @Override
    public byte getSocialNetId() {
        return 0;
    }

    @Override
    public long getRequestId() {
        return requestNum;
    }

    @Override
    public String toString() {
        return "IntercomAchieveResponse{" +
                "requestType=" + requestType +
                ", profileId='" + profileId + '\'' +
                ", requestNum=" + requestNum +
                ", resultEnumType=" + (requestType == IntercomAchieveRequest.RequestType.GiveBonusItem ? GrantAwardResultEnum.valueOf(resultEnumType) : ShopResultEnum.valueOf(resultEnumType)) +
                '}';
    }
}
