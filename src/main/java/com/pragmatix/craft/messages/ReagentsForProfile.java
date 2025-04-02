package com.pragmatix.craft.messages;

import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 07.07.12 14:29
 */
@Command(10085)
public class ReagentsForProfile {

    public int[] reagents;

    public ReagentsForProfile() {
    }

    public ReagentsForProfile(int[] reagents) {
        this.reagents = reagents;
    }

    @Override
    public String toString() {
        return "ReagentsForProfile{" +
                "reagents=" + Arrays.toString(reagents) +
                '}';
    }
}
