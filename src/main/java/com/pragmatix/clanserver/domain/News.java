package com.pragmatix.clanserver.domain;

import com.pragmatix.clanserver.utils.Utils;

import java.util.Date;

/**
 * Author: Vladimir
 * Date: 18.04.13 14:16
 */
public class News {
    /**
     * Дата и время публикации
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
     * Текст новости
     */
    public String text;

    public News() {
    }

    public News(short publisherSocialId, int publisherProfileId, String publisherName, String text) {
        this.logDate = new Date();
        this.publisherSocialId = publisherSocialId;
        this.publisherProfileId = publisherProfileId;
        this.publisherName = publisherName;
        this.text = text;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + propertiesString() + '}';
    }

    protected StringBuilder propertiesString() {
        return new StringBuilder()
                .append(", logDate=").append(Utils.LOG_DATE_FORMAT.format(logDate))
                .append(", publisherSocialId=").append(publisherSocialId)
                .append(", publisherProfileId=").append(publisherProfileId)
                .append(", text=").append(text)
                ;
    }
}
