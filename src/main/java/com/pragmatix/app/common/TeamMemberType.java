package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 21.04.2014 12:33
 */
public enum TeamMemberType implements TypeableEnum {

    Himself(0),
    Friend(1),
    SoclanMember(2),
    OtherClanMember(3, false),
    Merchenary(4),
//    InaccessibleClanMember(5, false),
    Gladiator(6),
    MerchenaryOther(7),
    ;

    private final int type;
    private final boolean active;

    public final static Map<Integer, TeamMemberType> valuesMap = Arrays.stream(values()).collect(Collectors.toMap(it -> it.type, it -> it));

    public static TeamMemberType valueOf(int value) {
        return valuesMap.get(value);
    }

    TeamMemberType(int type) {
        this.type = type;
        active = true;
    }

    TeamMemberType(int type, boolean active) {
        this.type = type;
        this.active = active;
    }

    public int getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }

    // у наёмника уровень равен уровню владельца
    public boolean isSameLevel() {
        return this == Merchenary;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }
}
