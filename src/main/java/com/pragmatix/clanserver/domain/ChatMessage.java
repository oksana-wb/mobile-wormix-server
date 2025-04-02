package com.pragmatix.clanserver.domain;

import com.pragmatix.clanserver.utils.Utils;

import java.util.Date;

/**
 * Author: Vladimir
 * Date: 15.04.13 10:55
 */
public class ChatMessage {
    /**
     * Идентификатор сообщения (код команды)
     */
    public int actionId;

    /**
     * Дата и время публикации сообщения
     */
    public Date logDate;

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

    public ChatMessage() {
    }

    public ChatMessage(int actionId, short publisherSocialId, int publisherProfileId, String publisherName, String params) {
        this.actionId = actionId;
        this.logDate = new Date();
        this.publisherSocialId = publisherSocialId;
        this.publisherProfileId = publisherProfileId;
        this.publisherName = publisherName;
        this.params = params;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + propertiesString() + '}';
    }

    protected StringBuilder propertiesString() {
        return new StringBuilder()
                .append("actionId=").append(actionId)
                .append(", logDate=").append(logDate != null ? Utils.LOG_DATE_FORMAT.format(logDate) : null)
                .append(", publisherSocialId=").append(publisherSocialId)
                .append(", publisherProfileId=").append(publisherProfileId)
                .append(", publisherName=").append(publisherName)
                .append(", memberSocialId=").append(memberSocialId)
                .append(", memberProfileId=").append(memberProfileId)
                .append(", memberName=").append(memberName)
                .append(", params=").append(params)
                ;
    }
}
