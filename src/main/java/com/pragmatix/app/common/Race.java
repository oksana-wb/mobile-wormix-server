package com.pragmatix.app.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.pragmatix.gameapp.common.TypeableEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 04.05.11 12:22
 */
public enum Race implements TypeableEnum {

    // расы расставлены по силе (стоимости)
    BOXER(2),
    ZOMBIE(5),
    RABBIT(4),
    DEMON(3),
    CAT(6),
    DRAGON(7),
    ROBOT(8),
    BOAR(0),
    RHINO(9),
    ALIEN(10, /*exclusive=*/true),
    ;

    public static final Map<Integer, Race> valuesMap = Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(it -> it.type, it -> it));

    @JsonCreator
    public static Race valueOf(int type) {
        var race = valuesMap.get(type);
        if (race == null) {
            throw new IllegalArgumentException("Раса с type=" + type + " не зарегистрирована!");
        }
        return race;
    }

    public final int type;

    public final boolean exclusive;

    Race(int type) {
        this.type = type;
        this.exclusive = false;
    }

    Race(int type, boolean exclusive) {
        this.type = type;
        this.exclusive = exclusive;
    }

    @Override
    public int getType() {
        return type;
    }

    public byte getByteType() {
        return (byte) type;
    }

    public short getShortType() {
        return (short) type;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }

    public static short addRace(short races, Race race) {
        return (short) (races | 1 << race.getType());
    }

    public static short setRace(Race race) {
        return (short) (1 << race.getType());
    }

    public static short setRaces(Race... races) {
        short result = 0;
        for (Race race : races) {
            result = addRace(result, race);
        }
        return result;
    }

    public static short removeRace(short races, Race race) {
        return (short) (races & ~(1 << race.getType()));
    }

    public static List<Race> toList(short races) {
        List<Race> result = new ArrayList<>(valuesMap.values().size());
        for (Race race : valuesMap.values()) {
            if (hasRace(races, race))
                result.add(race);
        }
        return result;
    }

    public static boolean hasRace(short races, Race race) {
        return (races & 1 << race.getType()) != 0;
    }
}
