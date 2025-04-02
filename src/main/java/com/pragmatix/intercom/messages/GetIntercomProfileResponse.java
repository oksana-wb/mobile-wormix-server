package com.pragmatix.intercom.messages;

import com.pragmatix.intercom.structures.UserProfileIntercomStructure;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.sessions.IAppServer;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 10.08.2016 10:50
 * @see com.pragmatix.intercom.controller.IntercomController#onGetBigProfileIntercomResponse(GetIntercomProfileResponse, IAppServer)
 */
@Command(5003)
public class GetIntercomProfileResponse implements IntercomResponseI {

    public long profileId;

    public long recipientProfileId;

    public long requestNum;

    public UserProfileIntercomStructure profileIntercomStructure;

    public static GetIntercomProfileResponse GetBigProfileIntercomResponse(GetIntercomProfileRequest request, UserProfileIntercomStructure profileIntercomStructure) {
        GetIntercomProfileResponse response = new GetIntercomProfileResponse();
        response.profileId = request.profileId;
        response.requestNum = request.requestNum;
        response.recipientProfileId = request.recipientProfileId;
        response.profileIntercomStructure = profileIntercomStructure;
        return response;
    }

    @Override
    public String toString() {
        return "GetIntercomProfileResponse{" +
                "profileId=" + profileId +
                ", recipientProfileId=" + recipientProfileId +
                ", requestNum=" + requestNum +
                ", " + profileIntercomStructure +
                '}';
    }

    @Override
    public long getProfileId() {
        return profileId;
    }

    @Override
    public byte getSocialNetId() {
        return 0;
    }

    @Override
    public long getRequestId() {
        return requestNum;
    }
}
