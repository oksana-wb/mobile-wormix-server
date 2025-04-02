package com.pragmatix.app.services;

import com.pragmatix.app.model.UserProfile;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.09.12 11:10
 */
public interface UserRegistryI {

    int getProfileLevel(Long profileId);

    void updateLevel(UserProfile userProfile);

    boolean isProfileAbandonded(Long profileId);

    void setAbandondedFlag(UserProfile userProfile, boolean value);

    void updateLevelAndSetAbandondedFlag(UserProfile userProfile, boolean value);
}
