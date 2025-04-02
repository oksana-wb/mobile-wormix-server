package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.messages.request.AbstractRequest;
import com.pragmatix.clanserver.messages.request.PostNewsRequest;
import com.pragmatix.clanserver.messages.request.PostToChatRequest;
import com.pragmatix.clanserver.messages.response.CommonResponse;
import com.pragmatix.clanserver.messages.response.PostNewsResponse;
import com.pragmatix.clanserver.messages.response.PostToChatResponse;

/**
 * Author: Vladimir
 * Date: 15.04.13 9:33
 */
public interface ChatService {
    PostToChatResponse postToChat(PostToChatRequest request, ClanMember user);

    PostNewsResponse postNews(PostNewsRequest request, ClanMember user);

    void postClanAction(Clan clan, ClanMember publisher, ClanMember member, AbstractRequest request, CommonResponse response);

    void broadcastClanAction(Clan clan, ClanMember publisher, ClanMember member, AbstractRequest request, CommonResponse response, ClanMember... others);

    void broadcastClanAction(Clan clan, ClanMember publisher, ClanMember member, int actionId, CommonResponse response, ClanMember... others);

    void broadcastClanAction(Clan clan, ClanMember publisher, ClanMember member, int actionId, String text, ClanMember... others);
}
