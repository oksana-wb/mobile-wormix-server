package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.structures.InviteStructure;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.04.13 17:23
 *
 * @see com.pragmatix.clan.ClanInteropServiceImpl#sendInvite(short, int, com.pragmatix.clanserver.domain.ClanMember, com.pragmatix.clanserver.domain.Clan, InviteToClanResponse)
 *
 */
@Command(94)
public class InviteFriendToClan {

    public InviteStructure invite;

    public InviteFriendToClan() {
    }

    public InviteFriendToClan(InviteStructure invite) {
        this.invite = invite;
    }

    @Override
    public String toString() {
        return "InviteFriendToClan{" +
                "invite=" + invite +
                '}';
    }

}
