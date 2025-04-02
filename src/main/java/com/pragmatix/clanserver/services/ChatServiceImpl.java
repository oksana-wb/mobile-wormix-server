package com.pragmatix.clanserver.services;

import com.pragmatix.app.model.RestrictionItem;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.RestrictionService;
import com.pragmatix.clanserver.domain.ChatMessage;
import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.domain.News;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.ServiceResult;
import com.pragmatix.clanserver.messages.event.ChatMessageEvent;
import com.pragmatix.clanserver.messages.request.AbstractRequest;
import com.pragmatix.clanserver.messages.request.PostNewsRequest;
import com.pragmatix.clanserver.messages.request.PostToChatRequest;
import com.pragmatix.clanserver.messages.response.CommonResponse;
import com.pragmatix.clanserver.messages.response.PostNewsResponse;
import com.pragmatix.clanserver.messages.response.PostToChatResponse;
import com.pragmatix.clanserver.messages.structures.ChatMessageTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Author: Vladimir
 * Date: 15.04.13 9:39
 */
@Service
public class ChatServiceImpl implements ChatService {
    Logger chatLogger = LoggerFactory.getLogger("CLAN_CHAT_LOG");
    Logger newsLogger = LoggerFactory.getLogger("CLAN_NEWS_LOG");

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ConcurrentService concurrentService;

    @Resource
    private RestrictionService restrictionService;

    @Value("${chat.history.size:30}")
    int chatHistorySize;

    @Value("${chat.message.length.limit:256}")
    int chatMessageLengthLimit;

    @Value("${news.history.size:1}")
    int newsHistorySize;

    @Value("${news.length.limit:256}")
    int newsLengthLimit;

    int minClanChatDelay = 500;

    @Resource
    private ProfileService profileService;

    @Override
    public PostToChatResponse postToChat(PostToChatRequest request, ClanMember user) {
        PostToChatResponse response = new PostToChatResponse(request);

        if (user.clan == null) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Не в клане";
        } else if (user.muteMode || restrictionService.isRestricted((long) user.profileId, RestrictionItem.BlockFlag.CLAN_CHAT)) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Игроку запрещено писать в чат";
        } else {
            if (user.lastChatMessageTime > 0 && System.currentTimeMillis() - user.lastChatMessageTime < minClanChatDelay) {
                log.error("сообщения в клановый чат посылаются слишком часто! {}", request);
                return null;
            }
            int messageLength = request.text != null ? request.text.length() : 0;

            if (messageLength <= 0 || messageLength > chatMessageLengthLimit) {
                response.serviceResult = ServiceResult.ERR_INVALID_ARGUMENT;
                response.logMessage = "Сообщение некорректной длины " + messageLength;
            } else {
                ChatMessage message = new ChatMessage(Messages.POST_TO_CHAT_REQUEST, user.socialId, user.profileId, user.name, request.text);

                post(user.clan, message, response);

                logChat(user.clan.id, user.socialId, user.profileId, user.name, request.text);
                user.lastChatMessageTime = System.currentTimeMillis();
            }
        }

        return response;
    }

    @Override
    public PostNewsResponse postNews(final PostNewsRequest request, final ClanMember user) {
        PostNewsResponse response = new PostNewsResponse(request);

        if (user.clan == null) {
            response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
            response.logMessage = "Не в клане";
        } else {
            final int messageLength = request.text != null ? request.text.length() : 0;

            if (!user.rank.canPostNews()) {
                response.serviceResult = ServiceResult.ERR_ACCESS_DENIED;
                response.logMessage = "Не разрешено";
            } else if (messageLength > newsLengthLimit) {
                response.serviceResult = ServiceResult.ERR_INVALID_ARGUMENT;
                response.logMessage = "Слишком длинное сообщение " + messageLength;
            } else {
                ClanTask task = new ClanTask(user.clan) {
                    @Override
                    protected void exec() {
                        if (messageLength > 0) {
                            clan.newsBoard.add(new News(user.socialId, user.profileId, user.name, request.text));

                            while (clan.newsBoard.size() > newsHistorySize) {
                                clan.newsBoard.remove(0);
                            }
                        } else {
                            clan.newsBoard.clear();
                        }
                    }
                };
                concurrentService.execWrite(task, response);

                logNews(user.clan.id, user.socialId, user.profileId, user.name, request.text);

                broadcast(user.clan, chatMessage(request.getCommandId(), user, null, request.text), response);
            }
        }

        return response;
    }

    @Override
    public void postClanAction(Clan clan, ClanMember publisher, ClanMember member, AbstractRequest request, CommonResponse response) {
        ChatMessage chatMessage = chatMessage(request.getCommandId(), publisher, member, null);

        post(clan, chatMessage, response);
    }

    @Override
    public void broadcastClanAction(Clan clan, ClanMember publisher, ClanMember member, AbstractRequest request, CommonResponse response, ClanMember... others) {
        broadcastClanAction(clan, publisher, member, request.getCommandId(), response, others);
    }

    @Override
    public void broadcastClanAction(Clan clan, ClanMember publisher, ClanMember member, int actionId, CommonResponse response, ClanMember... others) {
        ChatMessage chatMessage = chatMessage(actionId, publisher, member, null);

        broadcast(clan, chatMessage, response, others);
    }

    @Override
    public void broadcastClanAction(Clan clan, ClanMember publisher, ClanMember member, int actionId, String text, ClanMember... others) {
        ChatMessage chatMessage = chatMessage(actionId, publisher, member, text);

        broadcast(clan, chatMessage, null, others);
    }

    private void post(Clan clan, final ChatMessage chatMessage, CommonResponse response) {
        ClanTask task = new ClanTask(clan) {
            @Override
            protected void exec() {
                clan.chat.add(chatMessage);

                while (clan.chat.size() > chatHistorySize) {
                    clan.chat.remove(0);
                }
            }
        };
        concurrentService.execWrite(task, response);

        if (response.isOk()) {
            broadcast(clan, chatMessage, response);
        }
    }

    private void broadcast(Clan clan, ChatMessage chatMessage, CommonResponse response, final ClanMember... others) {
        final ChatMessageEvent broadcast = new ChatMessageEvent(new ChatMessageTO(chatMessage));

        ClanTask task = new ClanTask(clan) {
            @Override
            protected void exec() {
                for (ClanMember target : clan.members()) {
                    if (target.isOnline()) {
                        com.pragmatix.gameapp.messages.Messages.toUser(broadcast, profileService.getUserProfile(target.profileId));
                    }
                }
                for (ClanMember target : others) {
                    if (target.isOnline()) {
                        com.pragmatix.gameapp.messages.Messages.toUser(broadcast, profileService.getUserProfile(target.profileId));
                    }
                }
            }
        };
        concurrentService.execRead(task, response);
    }

    private ChatMessage chatMessage(int actionId, ClanMember publisher, ClanMember member, String paramString) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.actionId = actionId;
        chatMessage.logDate = new Date();
        if (publisher != null) {
            chatMessage.publisherSocialId = publisher.socialId;
            chatMessage.publisherProfileId = publisher.profileId;
            chatMessage.publisherName = publisher.name;
        }
        if (member != null) {
            chatMessage.memberSocialId = member.socialId;
            chatMessage.memberProfileId = member.profileId;
            chatMessage.memberName = member.name;
        }
        if (paramString != null) {
            chatMessage.params = paramString;
        }
        return chatMessage;
    }

    private void logChat(Integer clanId, short publisherSocialId, Integer publisherProfileId, String publisherName, String text) {
        if (chatLogger.isInfoEnabled()) {
            chatLogger.info("CHAT\t{}\t{}\t{}\t{}\t{}", clanId, publisherSocialId, publisherProfileId, publisherName, unspace(text));
        }
    }

    private void logNews(Integer clanId, short publisherSocialId, Integer publisherProfileId, String publisherName, String text) {
        if (chatLogger.isInfoEnabled()) {
            newsLogger.info("NEWS\t{}\t{}\t{}\t{}\t{}", clanId, publisherSocialId, publisherProfileId, publisherName, unspace(text));
        }
    }

    private static final Pattern UNSPACE_PATTERN = Pattern.compile("\\s+");

    private String unspace(String params) {
        if (params != null) {
            return UNSPACE_PATTERN.matcher(params).replaceAll(" ").trim();
        } else {
            return "";
        }
    }
}
