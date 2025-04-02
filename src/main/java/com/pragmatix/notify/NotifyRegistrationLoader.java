package com.pragmatix.notify;

import com.pragmatix.gameapp.cache.loaders.ILoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.08.13 13:11
 */
@Component
public class NotifyRegistrationLoader implements ILoader<NotifyRegistration, Long> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private NotifyRegistrationDAO dao;

    @Override
    public NotifyRegistration load(Long key) {
        NotifyRegistrationEntity entity = dao.get(key);
        if(entity != null) {
            if(log.isDebugEnabled()) {
                log.debug("by key {} loaded from DB: {}", key,  entity);
            }
            return entity.getUnregistrationDate() == null ? new NotifyRegistration(entity.getRegistrationId(), entity.getSocialNetId()) : null;
        } else {
            return null;
        }
    }

    @Override
    public Class<NotifyRegistration> getLoadedClass() {
        return NotifyRegistration.class;
    }

}
