package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.request.GetJoinClans;
import com.pragmatix.clanserver.messages.structures.ClanTO;
import com.pragmatix.serialization.annotations.Command;

import java.util.*;

/**
 * Список из 20 кланов, в которые игрок может вступить
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.06.13 10:57
 *
 *  @see com.pragmatix.clan.ClanController#onGetJoinClans(GetJoinClans, com.pragmatix.app.model.UserProfile)
 */
@Command(1098)
public class GetJoinClansResponse {

    public ClanTO[] clans;

    public GetJoinClansResponse() {
    }

    public GetJoinClansResponse(ClanTO[] clans) {
        this.clans = clans;
    }

    @Override
    public String toString() {
        return "GetJoinClansResponse{" +
                "clans=" + Arrays.toString(clans) +
                '}';
    }

}
