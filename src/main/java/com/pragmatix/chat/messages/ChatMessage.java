package com.pragmatix.chat.messages;

import com.pragmatix.chat.ChatAction;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Resize;
import com.pragmatix.serialization.annotations.Structure;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * @see ChatMessageEvent
 */

@Structure(nullable = true)
public class ChatMessage implements Serializable, Comparable<ChatMessage> {

    @Override
    public int compareTo(ChatMessage o) {
        return o.logDate - this.logDate;
    }

    private static final long serialVersionUID = 3665872750062792695L;

    @Resize(TypeSize.UINT32)
    public long id;
    /**
     * Код действия
     */
    public ChatAction action;
    /**
     * Дата и время публикации сообщения
     */
    public int logDate;
    /**
     * Идентификатор профиля субъекта сообщения
     */
    public int profileId;
    /**
     * Идентификатор профиля в соц. сети субъекта сообщения
     */
    public String profileStringId;
    /**
     * Имя субъекта сообщения
     */
    public String profileName;
    /**
     * Необязательные параметры
     */
    public String message = "";
    /**
     * Необязательные параметры
     */
    public String params = "";

    @Override
    public String toString() {
        return "(" + id + "){" + action +
                ", " + AppUtils.formatDateInSeconds(logDate) +
                ", profileId=" + profileId +
                (StringUtils.isNoneEmpty(profileStringId) ? ":" + profileStringId : "") +
                (StringUtils.isNoneEmpty(profileName) ? ", name='" + profileName + '\'' : "") +
                (StringUtils.isNoneEmpty(message) ? ", message='" + message + '\'' : "") +
                (StringUtils.isNoneEmpty(params) ? ", params='" + params + '\'' : "") +
                '}';
    }

}
