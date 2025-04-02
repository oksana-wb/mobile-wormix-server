package com.pragmatix.achieve.services;

import org.springframework.beans.factory.FactoryBean;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.07.11 17:24
 */
public class KeyFactoryBean implements FactoryBean<AchieveServiceKey> {

    private String socialId;

    private String applicationId;

    public void setSocialId(String socialId) {
        this.socialId = socialId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    @Override
    public AchieveServiceKey getObject() throws Exception {
        return new AchieveServiceKey(applicationId, socialId);
    }

    @Override
    public Class<?> getObjectType() {
        return AchieveServiceKey.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
