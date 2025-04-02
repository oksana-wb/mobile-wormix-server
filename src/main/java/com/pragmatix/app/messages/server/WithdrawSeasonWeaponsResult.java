package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.05.2016 16:55
 */
@Command(10134)
public class WithdrawSeasonWeaponsResult {

    public List<BackpackItemStructure> withdrawnWeapons;

    public int compensationInMoney;

    public int compensationInKeys;

    public int[] prevSeasonWeapons;

    public int[] currentSeasonWeapons;

    public int[] currentSeasonStuff;

    public WithdrawSeasonWeaponsResult() {
    }

    public WithdrawSeasonWeaponsResult(
            List<BackpackItemStructure> withdrawnWeapons,
            int compensationInMoney,
            int compensationInKeys,
            int[] prevSeasonWeapons,
            int[] currentSeasonWeapons,
            int[] currentSeasonStuff
    ) {
        this.withdrawnWeapons = withdrawnWeapons;
        this.compensationInMoney = compensationInMoney;
        this.compensationInKeys = compensationInKeys;
        this.prevSeasonWeapons = prevSeasonWeapons;
        this.currentSeasonWeapons = currentSeasonWeapons;
        this.currentSeasonStuff = currentSeasonStuff;
    }

    @Override
    public String toString() {
        return "WithdrawSeasonWeaponsResult{" +
                "withdrawnWeapons=" + withdrawnWeapons +
                ", compensationInMoney=" + compensationInMoney +
                ", compensationInKeys=" + compensationInKeys +
                ", prevSeasonWeapons=" + Arrays.toString(prevSeasonWeapons) +
                ", currentSeasonWeapons=" + Arrays.toString(currentSeasonWeapons) +
                ", currentSeasonStuff=" + Arrays.toString(currentSeasonStuff) +
                '}';
    }

}
