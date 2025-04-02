package com.pragmatix.clanserver.messages.request;

import com.pragmatix.serialization.annotations.Command;

/**
 * Запросить список из 20 кланов, в которые игрок может вступить
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.06.13 10:26
 *
 *  @see com.pragmatix.clan.ClanController#onGetJoinClans(GetJoinClans, com.pragmatix.app.model.UserProfile)
 */
@Command(98)
public class GetJoinClans {

    @Override
    public String toString() {
        return "GetJoinClans{}";
    }

}
