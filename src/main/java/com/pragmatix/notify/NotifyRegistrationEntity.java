package com.pragmatix.notify;

import com.pragmatix.gameapp.common.Identifiable;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Transient;
import javax.validation.constraints.Null;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.08.13 12:41
 */
public class NotifyRegistrationEntity implements Identifiable<Long> {

    private Long profileId;

    private short socialNetId;

    private String registrationId;

    private Date registrationDate;

    private Date unregistrationDate;

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public short getSocialNetId() {
        return socialNetId;
    }

    public void setSocialNetId(short socialNetId) {
        this.socialNetId = socialNetId;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Date getUnregistrationDate() {
        return unregistrationDate;
    }

    public void setUnregistrationDate(@Null Date unregistrationDate) {
        this.unregistrationDate = unregistrationDate;
    }

    @Override
    @Transient
    public Long getId() {
        return profileId;
    }

}
