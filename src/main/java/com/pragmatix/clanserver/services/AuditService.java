package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.common.ClanActionEnum;
import com.pragmatix.clanserver.dao.DAO;
import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.01.2015 18:01
 */
@Service
public class AuditService {

    @Resource
    private DAO dao;

    public void logClanAction(Clan clan, ClanActionEnum action, int publisherId, ClanMember member, int param) {
        if(action.logable)
            dao.logClanAction(clan.id, action.type, publisherId, member != null ? member.profileId : 0, param, clan.treas);
    }

    public void logClanAction(Clan clan, ClanActionEnum action, int publisherId, int param) {
        if(action.logable)
            dao.logClanAction(clan.id, action.type, publisherId, 0, param, clan.treas);
    }

}
