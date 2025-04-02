package com.pragmatix.achieve.services;


/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.07.11 19:00
 */
public class AchieveServiceKey {

    String application;

    String socialNetwork;

    public AchieveServiceKey(String application, String socialNetwork) {
        this.application = application;
        this.socialNetwork = socialNetwork;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        AchieveServiceKey that = (AchieveServiceKey) o;

        if(!application.equals(that.application)) return false;
        if(!socialNetwork.equals(that.socialNetwork)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = application.hashCode();
        result = 31 * result + socialNetwork.hashCode();
        return result;
    }
}
