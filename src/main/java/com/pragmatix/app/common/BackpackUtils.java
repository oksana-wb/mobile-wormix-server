package com.pragmatix.app.common;

import com.pragmatix.app.model.BackpackItem;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.11.2016 16:49
 */
public class BackpackUtils {

    public static short weaponId(int item) {
        return (short) (item >> 16);
    }

    public static short weaponCount(int item) {
        return (short) item;
    }

    public static int toItem(BackpackItem backpackItem) {
        return toItem((short) backpackItem.getWeaponId(), (short) backpackItem.getCount());
    }

    public static int toItem(int weaponId, int weaponCount) {
        return toItem((short)weaponId, (short)weaponCount);
    }

    public static int toItem(short weaponId, short weaponCount) {
        return ((int) weaponId) << 16 | weaponCount & 0x0000FFFF;
    }

    public static String backpackToString(int[] backpack) {
        return Arrays.stream(backpack)
                .mapToObj(backpackItem -> {
                    int weaponId = BackpackUtils.weaponId(backpackItem);
                    int weaponCount = BackpackUtils.weaponCount(backpackItem);
                    return weaponCount == -1 ? "" + weaponId : "" + weaponId + ":" + weaponCount;
                }).collect(Collectors.joining(",", "[", "]"));
    }

    public static BackpackItem toBackpackItem(int item) {
        return new BackpackItem(BackpackUtils.weaponId(item), BackpackUtils.weaponCount(item), false);
    }

}
