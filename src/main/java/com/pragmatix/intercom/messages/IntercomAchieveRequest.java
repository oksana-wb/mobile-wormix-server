package com.pragmatix.intercom.messages;

import com.pragmatix.app.messages.structures.AchieveAwardStructure;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.sessions.IAppServer;

import java.util.Collections;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 10.08.2016 10:50
 *
 * @see com.pragmatix.intercom.controller.IntercomController#onIntercomAchieveRequest(IntercomAchieveRequest, IAppServer)
 */
@Command(5000)
public class IntercomAchieveRequest implements IntercomRequestI {

    public enum RequestType {
        GrantAwards,
        BuyResetBonusItems,
        GiveBonusItem,
        WipeAchievements,
        SetInvestedAwardPoints,
        GetAchievements,
    }

    public RequestType requestType;

    public String profileId;

    public long timeSequence;

    public List<AchieveAwardStructure> awards;

    public long requestNum;

    public byte profilesBonusItemsCount;

    public static IntercomAchieveRequest GrantAwardsRequest(String profileId, long timeSequence, List<AchieveAwardStructure> awards, long requestNum) {
        IntercomAchieveRequest request = new IntercomAchieveRequest();
        request.requestNum = requestNum;
        request.requestType = RequestType.GrantAwards;
        request.profileId = profileId;
        request.timeSequence = timeSequence;
        request.awards = awards;
        return request;
    }

    public static IntercomAchieveRequest GiveBonusItemRequest(String profileId, int itemId, int awardType, long requestNum) {
        IntercomAchieveRequest request = new IntercomAchieveRequest();
        request.requestNum = requestNum;
        request.requestType = RequestType.GiveBonusItem;
        request.profileId = profileId;
        request.awards = Collections.singletonList(new AchieveAwardStructure(itemId, awardType));
        return request;
    }

    public static IntercomAchieveRequest BuyResetBonusItems(String profileId, long requestNum) {
        IntercomAchieveRequest request = new IntercomAchieveRequest();
        request.requestNum = requestNum;
        request.requestType = RequestType.BuyResetBonusItems;
        request.profileId = profileId;
        return request;
    }

    public static IntercomAchieveRequest WipeAchievements(String profileId, long requestNum) {
        IntercomAchieveRequest request = new IntercomAchieveRequest();
        request.requestNum = requestNum;
        request.requestType = RequestType.WipeAchievements;
        request.profileId = profileId;
        return request;
    }

    public static IntercomAchieveRequest GetAchievements(String profileId, long requestNum) {
        IntercomAchieveRequest request = new IntercomAchieveRequest();
        request.requestNum = requestNum;
        request.requestType = RequestType.GetAchievements;
        request.profileId = profileId;
        return request;
    }

    public static IntercomAchieveRequest SetInvestedAwardPoints(String profileId, byte profilesBonusItemsCount, long requestNum) {
        IntercomAchieveRequest request = new IntercomAchieveRequest();
        request.requestNum = requestNum;
        request.requestType = RequestType.SetInvestedAwardPoints;
        request.profileId = profileId;
        request.profilesBonusItemsCount = profilesBonusItemsCount;
        return request;
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
        return "IntercomAchieveRequest{" +
                "requestType=" + requestType +
                ", profileId='" + profileId + '\'' +
                ", timeSequence=" + timeSequence +
                ", awards=" + awards +
                ", profilesBonusItemsCount=" + profilesBonusItemsCount +
                ", requestNum=" + requestNum +
                '}';
    }

}
