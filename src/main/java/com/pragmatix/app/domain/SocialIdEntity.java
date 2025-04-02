package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.Identifiable;

import javax.persistence.Transient;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.09.11 11:22
 */
public class SocialIdEntity implements Identifiable<Long> {

    private String stringId;

    private short socialNetId;

    private Long profileId;

    public String getStringId() {
        return stringId;
    }

    public void setStringId(String stringId) {
        this.stringId = stringId;
    }

    public short getSocialNetId() {
        return socialNetId;
    }

    public void setSocialNetId(short socialNetId) {
        this.socialNetId = socialNetId;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    @Override
    @Transient
    public Long getId() {
        return profileId;
    }

}
