package com.pragmatix.clanserver.messages.request;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.common.utils.ArrayUtils;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 08.04.13 12:11
 *
 * @see com.pragmatix.clanserver.controllers.ClanAuthController#onLoginCreateRequest(LoginCreateRequest, UserProfile)
 */
@Command(Messages.LOGIN_CREATE_REQUEST)
public class LoginCreateRequest extends LoginBase {
    /**
     * Название клана
     */
    public String clanName;

    /**
     * Эмблема клана
     */
    public byte[] clanEmblem;
    /**
     * Описание клана
     */
    public String clanDescription;

    @Override
    public int getCommandId() {
        return Messages.LOGIN_CREATE_REQUEST;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("clanName=").append(clanName)
//                .append(", clanEmblem=").append(ArrayUtils.toString(clanEmblem))
                .append(", clanDescription=").append(clanDescription)
                ;
    }
}
