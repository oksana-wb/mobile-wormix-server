package com.pragmatix.achieve.common;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.11.11 14:39
 */
public class AchieveUtils {

    public static short intToShort(int intValue) {
        if(intValue >= 65535) return -1;
        if(intValue <= 0) return 0;
        return (short) intValue;
    }

    public static int normalize(int intValue) {
        if(intValue >= 65535) return 0;
        if(intValue <= 0) return 0;
        return intValue;
    }

    public static int shortToInt(short shortValue) {
        return 0xFFFF & shortValue;
    }

    public static boolean haveBoolAchievement(byte[] boolAchievements, int achievementIndex) {
        int arrIndex = achievementIndex / 8;
        int offset = achievementIndex % 8;

        return ((byte) (boolAchievements[arrIndex] << offset) & (byte) 0b10000000) != 0;
    }

    public static void setBoolAchievement(byte[] boolAchievements, int achievementIndex, boolean achievementValue) {
        int arrIndex = achievementIndex / 8;
        int offset = achievementIndex % 8;

        byte flag = (byte) (0b10000000 >> offset);
        if(achievementValue) {
            boolAchievements[arrIndex] |= flag;
        } else {
            boolAchievements[arrIndex] &= ~flag;
        }
    }

}
