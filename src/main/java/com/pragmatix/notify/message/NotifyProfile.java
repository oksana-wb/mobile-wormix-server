package com.pragmatix.notify.message;

import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Arrays;

/**
 * Отправить уведомление клиенту (в данном контексте это мобильное приложение) посредством сервиса уведомлений от Apple или Google
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.08.13 13:25
 * @see com.pragmatix.notify.NotifyController#onNotifyProfile(NotifyProfile, com.pragmatix.app.model.UserProfile)
 */
@Command(201)
public class NotifyProfile implements SecuredResponse {

    /**
     * id пользователя
     */
    @Resize(TypeSize.UINT32)
    public long recipientProfileId;

    public SocialServiceEnum socialNet;

    /**
     * ключ сообщения, для целей локализации
     */
    public String localizedKey;

    /**
     * аргументы сообщения
     */
    public String[] localizedArguments;

    /**
     * задержка (в секундах)
     */
    public int delay;

    /**
     * время актуальности сообщения (в секундах)
     */
    public int timeToLive;

    /**
     * ключ текущей сессии
     */
    public String sessionKey;

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "NotifyProfile{" +
                "recipientProfileId=" + recipientProfileId +
                ", socialNet=" + socialNet +
                ", localizedKey='" + localizedKey + '\'' +
                ", localizedArguments=" + Arrays.toString(localizedArguments) +
                ", delay=" + delay +
                ", timeToLive=" + timeToLive +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
