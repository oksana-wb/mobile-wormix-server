package com.pragmatix.admin.filters;


import com.pragmatix.admin.messages.client.LoginByAdmin;
import com.pragmatix.admin.messages.server.CommandResult;
import com.pragmatix.admin.messages.server.EnterAdmin;
import com.pragmatix.admin.model.AdminProfile;
import com.pragmatix.admin.services.AuditAdminService;
import com.pragmatix.gameapp.cache.PermanentCache;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.security.annotations.Authenticate;
import com.pragmatix.gameapp.security.annotations.Filter;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.sessions.IUser;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Filter для обработки админских команд
 * <p/>
 * User: denis
 * Date: 13.12.2009
 * Time: 21:24:35
 */
@Filter
public class AdminFilter {

    @Resource
    private PermanentCache permanentCache;

    @Authenticate(LoginByAdmin.class)
    public IUser onLoginByAdmin(LoginByAdmin msg) {
        AdminProfile adminProfile = permanentCache.get(AdminProfile.class, msg.login);
        if(adminProfile != null && adminProfile.getPassword().equals(msg.password)) {
            if(Sessions.get(adminProfile) != null) {
                Messages.toUser(new CommandResult("already logged"));
                return adminProfile;
            }
            Messages.toUser(new EnterAdmin(adminProfile.getRole().getType(), new SimpleDateFormat("dd.MM.yy HH.mm").format(new Date())));
            return adminProfile;
        } else {
            Messages.toUser(new CommandResult("incorrect login or password"));
            return null;
        }
    }

}
