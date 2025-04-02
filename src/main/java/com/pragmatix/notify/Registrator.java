package com.pragmatix.notify;

import com.pragmatix.gameapp.cache.SoftCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.Date;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.08.13 16:38
 */
@Service
public class Registrator {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private SoftCache softCache;

    @Resource
    private NotifyRegistrationDAO dao;

    public void registrate(Long profileId, short socialNetId, String registrationId) {
        NotifyRegistrationEntity entity = dao.selectByRegistrationId(registrationId);
        if(entity != null) {// уже зарегистрирован данный registrationId
            if(!entity.getProfileId().equals(profileId)) {// на другой профиль
                softCache.remove(NotifyRegistration.class, entity.getProfileId());
                dao.delete(entity);

                // ищем предыдущий registrationId данного профиля и удаляем его если находим
                NotifyRegistration notifyRegistration = softCache.get(NotifyRegistration.class, profileId);
                if(notifyRegistration != null){
                    softCache.remove(NotifyRegistration.class, profileId);
                    dao.delete(profileId);
                }

                entity = new NotifyRegistrationEntity();
                entity.setProfileId(profileId);
                entity.setSocialNetId(socialNetId);
                entity.setRegistrationId(registrationId);
                entity.setRegistrationDate(new Date());

                dao.insert(entity);

                if(log.isDebugEnabled()) {
                    log.debug(String.format("update notify registration: %s:%s -> '%s'", socialNetId, profileId, registrationId));
                }
            }
        } else {
            entity = dao.get(profileId);

            if(entity == null) {
                entity = new NotifyRegistrationEntity();
                entity.setProfileId(profileId);
                entity.setSocialNetId(socialNetId);
                entity.setRegistrationId(registrationId);
                entity.setRegistrationDate(new Date());

                dao.insert(entity);

                if(log.isDebugEnabled()) {
                    log.debug(String.format("add notify registration: %s:%s -> '%s'", socialNetId, profileId, registrationId));
                }
            } else {
                entity.setSocialNetId(socialNetId);
                entity.setRegistrationId(registrationId);
                entity.setRegistrationDate(new Date());
                entity.setUnregistrationDate(null);

                dao.update(entity);

                if(log.isDebugEnabled()) {
                    log.debug(String.format("update notify registration: %s:%s -> '%s'", socialNetId, profileId, registrationId));
                }
            }
        }
        softCache.put(NotifyRegistration.class, profileId, new NotifyRegistration(registrationId, socialNetId));
    }

    public void unregistrate(String inactiveRegistrationId) {
        log.info("Unregistered device: " + inactiveRegistrationId);

        final NotifyRegistrationEntity entity = dao.selectByRegistrationId(inactiveRegistrationId);
        if(entity != null) {
            dao.delete(entity);
            softCache.remove(NotifyRegistration.class, entity.getProfileId());
        }
    }

    public void updateRegistration(String oldRegistrationId, String newRegistrationId) {
        final NotifyRegistrationEntity entity = dao.selectByRegistrationId(oldRegistrationId);
        if(entity != null) {
            dao.delete(dao.selectByRegistrationId(newRegistrationId));

            entity.setRegistrationId(newRegistrationId);
            entity.setRegistrationDate(new Date());
            entity.setUnregistrationDate(null);

            dao.update(entity);

            if(log.isDebugEnabled()) {
                log.debug(String.format("update notify registration: %s:%s -> '%s'", entity.getSocialNetId(), entity.getProfileId(), entity.getRegistrationId()));
            }

            softCache.put(NotifyRegistration.class, entity.getProfileId(), new NotifyRegistration(entity.getRegistrationId(), entity.getSocialNetId()));
        }
    }

    @Null
    public NotifyRegistration getNotifyRegistration(Long profileId) {
        return softCache.get(NotifyRegistration.class, profileId);
    }

}
