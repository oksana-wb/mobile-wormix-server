package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.structures.ClanAuditActionTO;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.ServiceResult;
import com.pragmatix.clanserver.messages.request.AuditActionsRequest;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;
import java.util.List;

/**
 * @see com.pragmatix.clanserver.controllers.ClanController#auditActions(AuditActionsRequest, ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#auditActions(ClanMember)
 */
@Command(Messages.AUDIT_ACTIONS_RESPONSE)
public class AuditActionsResponse extends CommonResponse<AuditActionsRequest> {

    public ClanAuditActionTO[] actions;

    public AuditActionsResponse() {
    }

    public AuditActionsResponse(List<ClanAuditActionTO> actions, AuditActionsRequest request) {
        super(ServiceResult.OK, request, "");
        this.actions = actions.toArray(new ClanAuditActionTO[actions.size()]);
    }

    @Override
    public String toString() {
        return "AuditActionsResponse{" +
                "actions=" + Arrays.toString(actions) +
                '}';
    }
}
