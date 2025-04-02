package com.pragmatix.app.dao;

import com.pragmatix.app.domain.stat.AuditAdminActionEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.06.11 16:46
 */
@Component
public class AuditAdminActionDao extends AbstractDao<AuditAdminActionEntity> {

    protected AuditAdminActionDao() {
        super(AuditAdminActionEntity.class);
    }

}
