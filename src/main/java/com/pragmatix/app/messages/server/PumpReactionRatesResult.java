package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.structures.PumpReactionRateStructure;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * Created: 30.04.11 10:40
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
@Command(10033)
public class PumpReactionRatesResult {

    public PumpReactionRateStructure[] pumpedFriends;

    public PumpReactionRatesResult() {
    }

    public PumpReactionRatesResult(PumpReactionRateStructure[] pumpedFriends) {
        this.pumpedFriends = pumpedFriends;
    }

    @Override
    public String toString() {
        return "PumpReactionRatesResult{" +
                "pumpedFriends=" + (pumpedFriends == null ? null : Arrays.asList(pumpedFriends)) +
                '}';
    }
}
