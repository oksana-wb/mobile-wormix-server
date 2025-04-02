package com.pragmatix.app.services;

import com.pragmatix.app.common.ItemCheck;
import com.pragmatix.app.common.Race;
import com.pragmatix.app.model.UserProfile;

/**
 * класс для определения расы игрока
 */
public class RaceService {

    /**
     * база на которую сдвигаются расы
     */
    public static final int RACE_BASE = 500;

    /**
     * количество рас
     */
    public static final int RACE_COUNT = 50;

    /**
     * @param mixedHat игрока для которого нужно определить id расы
     * @return id расы
     */
    public static short getRaceId(short mixedHat) {
        if(mixedHat < RACE_COUNT) {
            //значит без шапки и раса хранится в незашифрованом виде            
            return mixedHat;
        } else {
            return (short) ((mixedHat - ItemCheck.STUFF_START_INDEX) / RACE_BASE);
        }
    }

    /**
     * вернет очищеный от инфы о расе, id шапки
     */
    public static short getHatId(short mixedHat) {
        if(mixedHat < RACE_COUNT) {
            //значит без шапки
            return 0;
        } else {
            return (short) (mixedHat - (getRaceId(mixedHat) * RACE_BASE));
        }
    }

    // вернет id шапки в котором будет зашифрован id расы
    public static short getMixedHat(short raceId, short hatId) {
        // если на игроке не было шапки
        if(hatId == 0) {
            return raceId;
        } else {
            return (short) (hatId + raceId * RaceService.RACE_BASE);
        }
    }

    public static byte getRaceExceptExclusive(UserProfile profile) {
        Race origRace = Race.valueOf(profile.getRace());
        if(origRace.exclusive) {
            Race[] values = Race.values();
            for(int i = values.length - 1; i >= 0; i--) {
                Race race = values[i];
                if(!race.exclusive && Race.hasRace(profile.getRaces(), race))
                    return race.getByteType();
            }
        } else {
            return origRace.getByteType();
        }
        return Race.BOXER.getByteType();
    }

    public static boolean hasRace(UserProfile profile, Race race) {
        return Race.hasRace(profile.getRaces(), race);
    }

}