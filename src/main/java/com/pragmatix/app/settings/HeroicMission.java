package com.pragmatix.app.settings;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.07.13 13:21
 */
public class HeroicMission {

    private String key;

    private Set<Integer> maps;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Set<Integer> getMaps() {
        return maps;
    }

    public void setMaps(Set<Integer> maps) {
        this.maps = maps;
    }

    public void setMapsSet(String maps) {
        this.maps = new HashSet<>();
        for(String s : StringUtils.split(maps, " ")) {
            this.maps.add(Integer.valueOf(s));
        }
    }

}
