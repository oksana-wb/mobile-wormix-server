package com.pragmatix.notify;

import com.pragmatix.app.common.Locale;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.common.utils.VarObject;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.*;
import java.util.concurrent.DelayQueue;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.08.13 11:33
 */
@Service
public class NotifyService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private Registrator notifyRegistrator;

    @Resource
    private ProfileService profileService;

    @Resource
    private Properties messages;

    private final Map<SocialServiceEnum, NotifySenderI> notifySendersMap = new EnumMap<>(SocialServiceEnum.class);

    /**
     * задачи которые должны выполниться с некоторым ожиданием
     */
    private final DelayQueue<NotifyEvent> delayQueue = new DelayQueue<>();

    private Locale defaultLocale = Locale.RU;

    private boolean initialized = false;

    public void init() {
        initialized = notifySendersMap.size() > 0;
    }

    @Null
    public String getLocalizeMessage(Locale recipientLocale, String localizedKey, String... localizedArguments) {
        if(recipientLocale == Locale.NONE) {
            recipientLocale = defaultLocale;
        }
        String propsKey = localizedKey + "." + recipientLocale.name();
        String localizedMessageTemplate = messages.getProperty(propsKey);
        if(localizedMessageTemplate == null) {
            log.error("не задано сообщение для {}", propsKey);
            return null;
        }
        return String.format(localizedMessageTemplate, localizedArguments);
    }

    @Null
    public NotifyEvent send(long recipientProfileId, com.pragmatix.app.common.Locale recipientLocale, int delay, int timeToLive, NotifyEventType notifyEventType) {
        if(!initialized)
            return null;

        return send(recipientProfileId, recipientLocale, delay, timeToLive, notifyEventType.name());
    }

    @Null
    public NotifyEvent send(long recipientProfileId, com.pragmatix.app.common.Locale recipientLocale, int delay, int timeToLive, String localizedKey, String... localizedArguments) {
        if(!initialized)
            return null;

        VarObject<NotifySenderI> notifySender = new VarObject<>();
        String registrationId = getRegistrationId(recipientProfileId, notifySender);
        if(registrationId != null) {
            String localizedMessage = getLocalizeMessage(recipientLocale, localizedKey, localizedArguments);
            if(localizedMessage != null) {
                if(delay == 0) {
                    if(log.isDebugEnabled()) log.debug("notify profile [{}] >> '{}' ...", recipientProfileId, localizedMessage);
                    boolean sendResult = notifySender.value.send(registrationId, timeToLive, localizedMessage);
                    if(log.isDebugEnabled()) log.debug("notification profile [{}] is {}", recipientProfileId, sendResult ? "SUCCESS" : "FAILURE");
                } else {
                    long time = System.currentTimeMillis() + delay;
                    if(log.isDebugEnabled()) log.debug(String.format("schedule notify profile [%s] >> '%s' in %tT", recipientProfileId, localizedMessage, new Date(time)));
                    NotifyEvent notifyEvent = new NotifyEvent(time, recipientProfileId, timeToLive, localizedKey, localizedArguments);
                    delayQueue.offer(notifyEvent);
                    return notifyEvent;
                }
            }
        }
        return null;
    }

    @Null
    protected String getRegistrationId(Long recipientProfileId, VarObject<NotifySenderI> varNotifySender) {
        NotifyRegistration notifyRegistration = notifyRegistrator.getNotifyRegistration(recipientProfileId);

        if(notifyRegistration != null) {
            SocialServiceEnum socialServiceEnum = SocialServiceEnum.valueOf(notifyRegistration.socialNetId);
            NotifySenderI notifySender = notifySendersMap.get(socialServiceEnum);
            if(notifySender == null) {
                log.error("Реализация NotifySenderI не зарегистрирована для {}", socialServiceEnum);
                return null;
            }

            varNotifySender.value = notifySender;
            return notifyRegistration.registrationId;
        } else {
            if(log.isDebugEnabled()) {
                log.debug("recipient profile [{}] not registrated for notify", recipientProfileId);
            }
        }
        return null;
    }

    @Scheduled(fixedRate = 5000)
    public void proceedQueue() {
        if(!initialized)
            return;

        ArrayList<NotifyEvent> notifyEvents = new ArrayList<>();

        delayQueue.drainTo(notifyEvents);

        for(NotifyEvent notifyEvent : notifyEvents) {
            if(log.isDebugEnabled()) log.debug("notifyEvent: {}", notifyEvent);
            if(notifyEvent.needSend) {
                VarObject<NotifySenderI> notifySender = new VarObject<>();
                String registrationId = getRegistrationId(notifyEvent.profileId, notifySender);
                if(registrationId == null) {
                    continue;
                }
                UserProfile profile = profileService.getUserProfile(notifyEvent.profileId);
                if(profile == null) {
                    continue;
                }
                String localizedMessage = getLocalizeMessage(profile.getLocale(), notifyEvent.localizedKey, notifyEvent.localizedArguments);

                if(log.isDebugEnabled()) {
                    log.debug("notify profile [{}] >> '{}' ...", notifyEvent.profileId, localizedMessage);
                }

                boolean sendResult = notifySender.value.send(registrationId, notifyEvent.timeToLive, localizedMessage);

                if(log.isDebugEnabled()) {
                    log.debug("notification profile [{}] is {}", notifyEvent.profileId, sendResult ? "SUCCESS" : "FAILURE");
                }
                notifyEvent.needSend = false;
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("notify canceled for profile [{}] '{}'", notifyEvent.profileId, notifyEvent.localizedKey);
                }
            }
        }
    }

    @Autowired(required = false)
    public void setNotifySenders(Set<NotifySenderI> notifySenders) {
        for(NotifySenderI notifySender : notifySenders) {
            notifySendersMap.put(notifySender.getSocialNetId(), notifySender);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
