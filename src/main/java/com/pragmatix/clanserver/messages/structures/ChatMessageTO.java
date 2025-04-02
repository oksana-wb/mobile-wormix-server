package com.pragmatix.clanserver.messages.structures;

import com.pragmatix.clanserver.domain.ChatMessage;
import com.pragmatix.clanserver.utils.Utils;
import com.pragmatix.serialization.annotations.Structure;

/**
 * Author: Vladimir
 * Date: 15.04.13 10:55
 */
@Structure
public class ChatMessageTO {
    public static final ChatMessageTO[] EMPTY_ARR = {};

    /**
     * Код действия
     */
    public int actionCode;

    /**
     * Дата и время публикации сообщения
     */
    public int logDate;

    /**
     * Идентификатор сети субъекта сообщения
     */
    public short publisherSocialId;

    /**
     * Идентификатор профиля субъекта сообщения
     */
    public int publisherProfileId;

    public String publisherName;

    /**
     * Идентификатор сети объекта сообщения
     */
    public short memberSocialId;

    /**
     * Идентификатор профиля объекта сообщения
     */
    public int memberProfileId;

    public String memberName;

    /**
     * Необязательные параметры
     */
    public String params = "";

    public ChatMessageTO() {
    }

    public ChatMessageTO(ChatMessage message) {
        this.actionCode = message.actionId;
        this.logDate = Utils.toSeconds(message.logDate);
        this.publisherSocialId = message.publisherSocialId;
        this.publisherProfileId = message.publisherProfileId;
        this.publisherName = message.publisherName;
        this.memberSocialId = message.memberSocialId;
        this.memberProfileId = message.memberProfileId;
        this.memberName = message.memberName;
        this.params = message.params;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + propertiesString() + '}';
    }

    protected StringBuilder propertiesString() {
        return new StringBuilder()
                .append("actionId=").append(actionCode)
                .append(", logDate=").append(Utils.LOG_DATE_FORMAT.format(Utils.toDate(logDate)))
                .append(", publisherSocialId=").append(publisherSocialId)
                .append(", publisherProfileId=").append(publisherProfileId)
                .append(", publisherName=").append('\'').append(publisherName).append('\'')
                .append(", memberSocialId=").append(memberSocialId)
                .append(", memberProfileId=").append(memberProfileId)
                .append(", memberName=").append('\'').append(memberName).append('\'')
                .append(", params=").append('\'').append(params).append('\'')
                ;
    }
}
