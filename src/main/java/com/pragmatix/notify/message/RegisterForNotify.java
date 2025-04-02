package com.pragmatix.notify.message;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * Если клиент (в данном контексте это мобильное приложение) планирует получать уведомления от сервера
 * то в процесее регистрации на сервисе оповещений данной платформы (Apple/Google) он посредством этой команды передает выданный ему идентификатор
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.08.13 13:14
 * @see com.pragmatix.notify.NotifyController#onRegisterForNotify(RegisterForNotify, com.pragmatix.app.model.UserProfile)
 */
@Command(200)
public class RegisterForNotify extends SecuredCommand {

    /**
     * ключ полученный клиентом от сервиса уведомлений
     */
    public String registrationId;

    public SocialServiceEnum socialNet;

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
        return "RegiserForNotify{" +
                "registrationId='" + registrationId + '\'' +
                ", socialNet=" + socialNet +
                ", sessionKey='" + sessionKey + '\'' +
                ", secureResult=" + secureResult +
                '}';
    }

}
