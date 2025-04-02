package com.pragmatix.clanserver.messages.structures;

import com.pragmatix.clanserver.domain.News;
import com.pragmatix.clanserver.utils.Utils;
import com.pragmatix.serialization.annotations.Structure;

/**
 * Author: Vladimir
 * Date: 18.04.13 14:25
 */
@Structure
public class NewsTO {
    public static final NewsTO[] EMPTY_ARR = {};

    /**
     * Дата и время публикации
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
     * Текст новости
     */
    public String text;

    public NewsTO() {
    }

    public NewsTO(News news) {
        this.logDate = Utils.toSeconds(news.logDate);
        this.publisherSocialId = news.publisherSocialId;
        this.publisherProfileId = news.publisherProfileId;
        this.publisherName = news.publisherName;
        this.text = news.text;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + propertiesString() + '}';
    }

    protected StringBuilder propertiesString() {
        return new StringBuilder()
                .append("logDate=").append(Utils.LOG_DATE_FORMAT.format(Utils.toDate(logDate)))
                .append(", publisherSocialId=").append(publisherSocialId)
                .append(", publisherProfileId=").append(publisherProfileId)
                .append(", publisherName=").append(publisherName)
                .append(", text=").append(text)
                ;
    }
}
