package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.clanserver.controllers.ClanController#auditActions(AuditActionsRequest, ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#auditActions(ClanMember)
 * @see com.pragmatix.clanserver.messages.response.AuditActionsResponse
 */
@Command(Messages.AUDIT_ACTIONS_REQUEST)
public class AuditActionsRequest extends AbstractRequest{

    @Override
    public int getCommandId() {
        return Messages.AUDIT_ACTIONS_REQUEST;
    }

}
