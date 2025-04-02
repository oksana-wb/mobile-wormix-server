package com.pragmatix.clan;

import com.pragmatix.app.services.ProfileService;
import com.pragmatix.clanserver.messages.structures.InviteStructure;
import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.domain.Invite;
import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.clanserver.services.ClanSeasonService;
import com.pragmatix.clanserver.services.ClanService;
import com.pragmatix.clanserver.services.InviteRepo;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 29.04.13 15:06
 */
@Service
public class WormixClanService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ClanService clanService;

    @Resource
    private ClanSeasonService clanSeasonService;

    @Resource
    private InviteRepo inviteRepo;

    @Resource
    private ProfileService profileService;

    public InviteStructure[] getInvitesFor(byte socialId, Long profileId) {
        if(clanSeasonService.isDiscard()) {
            return new InviteStructure[0];
        } else {
            Invite[] invites = inviteRepo.getInvites(socialId, profileId.intValue());
            if(invites != null) {
                List<InviteStructure> result = new ArrayList<>();
                for(Invite invite : invites) {
                    InviteStructure inv = new InviteStructure();
                    Clan clan = clanService.getClan(invite.clanId);
                    if(clan != null) {
                        inv.clanId = clan.id;
                        inv.clanName = clan.name;
                        inv.clanEmblem = clan.emblem;
                        inv.profileId = invite.profileId;
                        inv.stringProfileId = profileService.getProfileStringId((long) invite.profileId);
                        inv.inviteDate = invite.inviteDate;

                        ClanMember clanMember = clanService.getClanMember(invite.socialId, invite.profileId);
                        if(clanMember != null) {
                            inv.rank = clanMember.rank;
                            inv.name = clanMember.name;
                        } else {
                            log.warn("ClanMember не найден по id [{}:{}]", invite.socialId, invite.profileId);
                            inv.rank = Rank.SOLDIER;
                            inv.name = "";
                        }

                        result.add(inv);
                    } else {
                        log.warn("не найден клан по id [{}]", invite.clanId);
                    }
                }
                return result.toArray(new InviteStructure[result.size()]);
            } else {
                return new InviteStructure[0];
            }
        }
    }

}
