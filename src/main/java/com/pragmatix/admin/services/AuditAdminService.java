package com.pragmatix.admin.services;

import com.pragmatix.app.domain.stat.AuditAdminActionEntity;
import com.pragmatix.app.services.DaoService;
import com.pragmatix.gameapp.services.TaskService;
import com.pragmatix.gameapp.sessions.Connection;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.webadmin.AdminHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.06.11 14:50
 */
@Service
public class AuditAdminService {

    @Resource
    private DaoService daoService;

    @Resource
    private TaskService taskService;

    //==== Утилитные методы ====

    public AuditAdminActionEntity prepareEntity(int id, String profileId, String note, String login) {
        final AuditAdminActionEntity entity = new AuditAdminActionEntity();
        entity.setDate(new Date());
        entity.setLogin(login);
        entity.setIp(getRequestIp());
        entity.setUserProfileId(profileId);
        entity.setCommandId(id);
        entity.setAdminNote(note);
        return entity;
    }

    public void addTask(final AuditAdminActionEntity entity) {
        TransactionCallback transactionTask = status -> {
            daoService.getAuditAdminActionDao().insert(entity);
            return null;
        };
        taskService.addTransactionTask(transactionTask);
    }

    private String getRequestIp() {
        Connection connection = Connections.get();
        if(connection != null) {
            return connection.getIP();
        } else {
            AdminHandler adminHandler = AdminHandler.get();
            if(adminHandler != null) {
                return adminHandler.getRemoteAddr();
            } else {
                return "[unknown]";
            }
        }
    }

}
