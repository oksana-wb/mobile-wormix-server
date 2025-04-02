package com.pragmatix.admin.controllers;

import com.pragmatix.admin.common.RoleType;
import com.pragmatix.admin.messages.client.*;
import com.pragmatix.admin.messages.server.*;
import com.pragmatix.admin.model.AdminProfile;
import com.pragmatix.admin.services.AuditAdminService;
import com.pragmatix.admin.services.CommandService;
import com.pragmatix.admin.services.RoleService;
import com.pragmatix.app.common.Connection;
import com.pragmatix.app.init.AdminProfileCreator;
import com.pragmatix.app.services.*;
import com.pragmatix.gameapp.cache.PermanentCache;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.services.TaskService;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;

import static java.lang.String.format;

/**
 * User: denis
 * Date: 28.01.2011
 * Time: 14:12:55
 */
@Controller
public class AdminController {

    @Resource
    private RoleService roleService;

    @Resource
    private CommandService commandService;

    @OnMessage(value = CallCommand.class, connections = {Connection.ADMIN})
    public Object onCallCommand(CallCommand msg, AdminProfile adminProfile) {
        if(!roleService.check(adminProfile, CallCommand.class)) {
            return new PermissionDenied();
        }
        return new CallCommandResult(commandService.execute(msg.command, msg.parametr));
    }

}
