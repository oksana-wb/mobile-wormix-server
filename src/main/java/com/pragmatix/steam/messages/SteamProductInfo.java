package com.pragmatix.steam.messages;

import com.pragmatix.serialization.annotations.Structure;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.07.2017 12:50
 */
@Structure
public class SteamProductInfo {

    public String code;

    public String category;

    public int cost;

    public String description;

    public SteamProductInfo() {
    }

    public SteamProductInfo(String code, String category, int cost, String description) {
        this.code = code;
        this.category = category;
        this.cost = cost;
        this.description = description;
    }

    @Override
    public String toString() {
        return "{" +
                "code='" + code + '\'' +
                ", category='" + category + '\'' +
                ", cost=" + cost +
                ", description='" + description + '\'' +
                '}';
    }
}
