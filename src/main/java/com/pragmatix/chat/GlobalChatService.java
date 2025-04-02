package com.pragmatix.chat;

import com.pragmatix.app.common.BattleState;
import com.pragmatix.app.common.Locale;
import com.pragmatix.app.model.RestrictionItem;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.RestrictionService;
import com.pragmatix.chat.messages.ChatMessage;
import com.pragmatix.chat.messages.ChatMessageEvent;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.GameApp;
import com.pragmatix.gameapp.services.TaskService;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.gameapp.sessions.NettyConnectionImpl;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.notify.message.SetLocale;
import io.netty.buffer.ByteBuf;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 06.10.2016 16:32
 */
@Service
public class GlobalChatService {

    private final Map<Locale, ChatRoom> chatRooms = new EnumMap(Locale.class);

    private int maxMessageLength = 128;
    private int chatHistorySize = 50;
    private int minChatDelay = 1000;

    @Value("${debug.validatePostToChat:true}")
    boolean validatePostToChat = true;

    @Value("${GlobalChatService.validateLastPaymentDate:false}")
    boolean validateLastPaymentDate = false;

    // постить в чат могут игроки задонатившиее не позднее lastPaymentThresholdInMonths месяцев
    @Value("${GlobalChatService.lastPaymentThresholdInMonths:6}")
    private int lastPaymentThresholdInMonths = 6;

    @Resource
    private GameApp gameApp;

    @Resource
    private TaskService taskService;

    @Resource
    private ProfileService profileService;

    @Resource
    private PersistenceService persistenceService;

    @Resource
    private RestrictionService restrictionService;

    private final String chatHistoryKeepFileName = "GlobalChatService.chatHistory";

    @PostConstruct
    public void init() {
        for(Locale locale : Locale.values()) {
            ChatRoom room = new ChatRoom(locale);
            chatRooms.put(locale, room);

            List<ChatMessage> chat = persistenceService.restoreObjectFromFile(List.class, chatHistoryKeepFileName + "_" + locale.name());
            if(CollectionUtils.isNotEmpty(chat)) {
                room.chat.addAll(chat);
                room.chatHistory.set(chat.toArray(new ChatMessage[0]));
            }
        }
    }

    public void persistToDisk() {
        for(ChatRoom room : chatRooms.values()) {
            persistenceService.persistObjectToFile(room.chat, chatHistoryKeepFileName + "_" + room.locale.name());
        }
    }

    public ChatMessage[] joinToChat(UserProfile profile) {
        ChatRoom chatRoom = chatRooms.get(getLocale(profile));
        chatRoom.inChatProfiles.put(profile.id, new ChatState());
        return chatRoom.chatHistory.get();
    }

    private Locale getLocale(UserProfile profile) {
        return profile == null || profile.getLocale() == Locale.NONE ? Locale.RU : profile.getLocale();
    }

    public void leaveFromChat(UserProfile profile) {
        ChatRoom chatRoom = chatRooms.get(getLocale(profile));
        chatRoom.inChatProfiles.remove(profile.id);
    }

    public void postToChat(ChatAction chatAction, String params, UserProfile profile) {
        ChatRoom chatRoom = chatRooms.get(getLocale(profile));

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.id = chatRoom.messageCounter.incrementAndGet();
        chatMessage.action = chatAction;
        chatMessage.profileId = profile.id;
        chatMessage.profileStringId = profile.getProfileStringId();
        chatMessage.profileName = profile.getName();
        chatMessage.logDate = AppUtils.currentTimeSeconds();
        chatMessage.params = params;

        postToChat(chatMessage, 0);
    }

    public ChatMessageEvent postToChat(UserProfile publisher, ChatAction action, String name, String message) {
        Tuple2<ChatMessageEvent.ChatMessageEventType, ChatState> canPostToChat = canPostToChat(publisher);
        if(canPostToChat._1 != ChatMessageEvent.ChatMessageEventType.SUCCESS)
            return new ChatMessageEvent(canPostToChat._1);
        if(!action.client)
            return new ChatMessageEvent(ChatMessageEvent.ChatMessageEventType.ILLEGAL_ACTION);

        ChatRoom chatRoom = chatRooms.get(getLocale(publisher));

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.id = chatRoom.messageCounter.incrementAndGet();
        chatMessage.action = action;
        chatMessage.profileId = publisher.id;
        chatMessage.profileStringId = publisher.getProfileStringId();
        chatMessage.profileName = StringUtils.isEmpty(name) ? publisher.getName() : name;
        chatMessage.logDate = AppUtils.currentTimeSeconds();
        chatMessage.message = StringUtils.abbreviate(message.trim(), maxMessageLength);

        postToChat(chatMessage, publisher.id);
        canPostToChat._2.updateLastMessageTime();

        return new ChatMessageEvent(chatMessage);
    }

    public void removeMessageFromHistory(long messageId, int profileId) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.action = ChatAction.RemoveFromHistory;
        chatMessage.params = messageId + "," + profileId;

        for(Locale locale : Locale.values()) {
            ChatRoom chatRoom = chatRooms.get(locale);
            synchronized (chatRoom.chat) {
                if(chatRoom.chat.removeIf(chatMsg -> chatMsg.id == messageId && chatMsg.profileId == profileId)) {
                    chatRoom.chatHistory.set(chatRoom.chat.toArray(new ChatMessage[0]));
                    broadcast(chatMessage, 0, chatRoom);
                    break;
                }
            }
        }
    }

    private void postToChat(ChatMessage chatMessage, int publisherId) {
        ChatRoom chatRoom = chatRooms.get(getLocale(profileService.getUserProfile(publisherId)));
        List<ChatMessage> chat = chatRoom.chat;
        synchronized (chat) {
            chat.add(chatMessage);
            while (chat.size() > chatHistorySize)
                chat.remove(0);
            chatRoom.chatHistory.set(chat.toArray(new ChatMessage[0]));
        }

        broadcast(chatMessage, publisherId, chatRoom);
    }

    private void broadcast(ChatMessage chatMessage, int publisherId, ChatRoom chatRoom) {
        taskService.addSimpleTask(() -> {
            ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
            Map<Integer, ChatState> inChatProfiles = chatRoom.inChatProfiles;
            for(Integer profileId : inChatProfiles.keySet()) {
                if(profileId == publisherId)
                    continue;
                UserProfile profile = profileService.getUserProfile(profileId, false);
                if(profile == null || !profile.isOnline() || !(profile.inBattleState(BattleState.NOT_IN_BATTLE) || profile.inBattleState(BattleState.WAIT_START_BATTLE))) {
                    inChatProfiles.remove(profileId);
                    continue;
                }
                Session session = gameApp.getSessions().get(profile);
                if(session == null) {
                    inChatProfiles.remove(profileId);
                    continue;
                }
                NettyConnectionImpl connection = (NettyConnectionImpl) session.getConnection();
                if(connection == null) {
                    inChatProfiles.remove(profileId);
                    continue;
                }
                channelGroup.add(connection.getChannel());
            }
            ByteBuf byteBuf = gameApp.getSerializer().serializeCommandToByteBuf(new ChatMessageEvent(chatMessage));
            channelGroup.writeAndFlush(byteBuf);
        });
    }

    public ChatMessage[] chatHistory() {
        return chatRooms.values().stream()
                .flatMap(room ->
                        Arrays.stream(room.chatHistory.get())
                                .map(msg -> (ChatMessage) SerializationUtils.clone(msg))
                                .peek(msg -> msg.message = "[" + room.locale.name() + "] " + msg.message)
                ).sorted()
                .toArray(ChatMessage[]::new);
    }

    private Tuple2<ChatMessageEvent.ChatMessageEventType, ChatState> canPostToChat(UserProfile profile) {
        if(!validatePostToChat)
            return Tuple.of(ChatMessageEvent.ChatMessageEventType.SUCCESS, null);

        if(validateLastPaymentDate && profile.getLastPaymentDateTime().isBefore(LocalDateTime.now().minusMonths(lastPaymentThresholdInMonths)))
            return Tuple.of(ChatMessageEvent.ChatMessageEventType.NOT_ALLOWED, null);

        if(restrictionService.isRestricted(profile.getId(), RestrictionItem.BlockFlag.GLOBAL_CHAT))
            return Tuple.of(ChatMessageEvent.ChatMessageEventType.RESTRICTED, null);

        ChatRoom chatRoom = chatRooms.get(getLocale(profile));
        ChatState chatState = chatRoom.inChatProfiles.get(profile.id);
        if(chatState == null)
            return Tuple.of(ChatMessageEvent.ChatMessageEventType.ILLEGAL_STATE, null);

        if(chatState.lastMessageTime > 0 && System.currentTimeMillis() < chatState.lastMessageTime + minChatDelay)
            return Tuple.of(ChatMessageEvent.ChatMessageEventType.TOO_FAST, null);

        return Tuple.of(ChatMessageEvent.ChatMessageEventType.SUCCESS, chatState);
    }

    public ChatMessage[] setLocaleAndChangeChatRoom(SetLocale msg, UserProfile profile) {
        if(msg.locale != profile.getLocale()) {
            leaveFromChat(profile);
        }
        profile.setLocale(msg.locale);

        return joinToChat(profile);
    }

    public void setMaxMessageLength(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }

    public void setChatHistorySize(int chatHistorySize) {
        this.chatHistorySize = chatHistorySize;
    }

    public void setMinChatDelay(int minChatDelay) {
        this.minChatDelay = minChatDelay;
    }

    public void setLastPaymentThresholdInMonths(int lastPaymentThresholdInMonths) {
        this.lastPaymentThresholdInMonths = lastPaymentThresholdInMonths;
    }
}
