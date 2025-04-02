package com.pragmatix.craft.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.07.12 11:41
 */
public enum Reagent {

    battery(0),
    gear(1),
    wood(2),
    stone(3),
    metal_plate(4),
    sand(5),
    coal(6),
    screw_bolt(7),
    screw_nut(8),
    pipe(9),
    spring(10),
    umbrella(20),
    accumulator(21),
    hammer(22),
    gunpowder(23),
    steel_bar(24),
    screwdriver(25),
    wrench(26),
    crystal(40),
    generator(41),
    microchip(42),
    sniper_scope(43),
    titan_bar(44),
    medal(50),
    prize_key(51),
    mutagen(52),;

    private byte index;

    private static final Map<Integer, Reagent> enumMap = new HashMap<>();

    static {
        for(Reagent reagent : Reagent.values()) {
            Reagent.enumMap.put((int) reagent.getIndex(), reagent);
        }
    }

    Reagent(int index) {
        this.index = (byte) index;
    }

    public byte getIndex() {
        return index;
    }

    public static Reagent valueOf(int index) {
        Reagent reagent = Reagent.enumMap.get(index);
        if(reagent == null)
            throw new IllegalArgumentException("Реагент не найден по id [" + index + "]");
        return reagent;
    }

    @Override
    public String toString() {
        return name() + "(" + getIndex() + ")";
    }

}
