package com.pragmatix.app.services;

import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.app.model.UserProfile;

import java.util.Date;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.01.2016 18:11
 */
public interface ProfileEventsServiceI {

    void fireProfileEvent(Date date, ProfileEventsService.ProfileEventEnum event, UserProfile profile, Object... params);

    void fireAchieveEvent(Date date, ProfileEventsService.ProfileEventEnum event, ProfileAchievements profileAchievements, Object... params);

}
