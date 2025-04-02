package com.pragmatix.webadmin;


import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.security.annotations.Authenticate;
import com.pragmatix.gameapp.security.annotations.Filter;
import com.pragmatix.sessions.IUser;
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest;

import javax.annotation.Resource;

/**
 * Filter для обработки админских команд (через groovy скрипты)
 */
@Filter
public class AdminFilter {

    @Resource
    private AdminService adminService;

    @Authenticate(ExecScriptRequest.class)
    public IUser onExecScriptRequest(ExecScriptRequest request) {
        Messages.toUser(adminService.execAdminScript(request));
        return null;
    }

}
