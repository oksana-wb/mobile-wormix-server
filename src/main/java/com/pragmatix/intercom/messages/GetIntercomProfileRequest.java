package com.pragmatix.intercom.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.sessions.IAppServer;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 10.08.2016 10:50
 *
 * @see com.pragmatix.intercom.controller.IntercomController#onGetIntercomProfileRequest(GetIntercomProfileRequest, IAppServer)
 */
@Command(5002)
public class GetIntercomProfileRequest implements IntercomRequestI {

    public long profileId;

    public long recipientProfileId;

    public SocialServiceEnum recipientNetId;

    public long requestNum;

    public String secureToken;

    public  boolean banProfile;


    public static GetIntercomProfileRequest GetIntercomProfileRequest(long profileId, UserProfile recipientProfile, boolean banProfile, String secureToken) {
        GetIntercomProfileRequest request = new GetIntercomProfileRequest();
        request.profileId = profileId;
        request.recipientProfileId = recipientProfile.getProfileId();
        request.recipientNetId = SocialServiceEnum.valueOf(recipientProfile.getSocialId());
        request.requestNum = recipientProfile.remoteServerRequestNum.incrementAndGet();
        request.banProfile = banProfile;
        request.secureToken = secureToken;
        return request;
    }

    @Override
    public String toString() {
        return "GetIntercomProfileRequest{" +
                "profileId=" + profileId +
                ", recipientProfileId=" + recipientProfileId +
                ", recipientNetId=" + recipientNetId +
                ", requestNum=" + requestNum +
                ", banProfile=" + banProfile +
                ", secureToken=" + secureToken +
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
