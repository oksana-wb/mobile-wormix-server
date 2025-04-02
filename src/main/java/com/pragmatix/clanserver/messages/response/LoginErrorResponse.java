package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.ServiceResult;
import com.pragmatix.clanserver.messages.request.LoginBase;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 08.04.13 10:16
 */
@Command(Messages.LOGIN_ERROR_RESPONSE)
public class LoginErrorResponse extends CommonResponse<LoginBase> {
    public LoginErrorResponse() {
    }

    public LoginErrorResponse(LoginBase request) {
        super(request);
    }

    public LoginErrorResponse(ServiceResult serviceResult, LoginBase request, String logMessage) {
        super(serviceResult, request, logMessage);
    }
}
