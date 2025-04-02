package com.pragmatix.app.services;

import com.pragmatix.app.init.StuffCreator;
import com.pragmatix.app.messages.server.StuffExpired;
import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.IGameApp;
import com.pragmatix.gameapp.sessions.Connection;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.server.Server;
import io.vavr.Predicates;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.pragmatix.app.common.Connection.MAIN;

@Service
public class TemporalStuffService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final int TEMP_STUF_SIZE = 6;

    @Resource
    private StuffCreator stuffCreator;

    public byte[] removeExpiredStuff(byte[] temporalStuff, @Null List<Short> expiredStuff) {
        if(temporalStuff == null) {
            return new byte[0];
        }
        boolean needRemove = false;
        for(int i = 0; i < temporalStuff.length; i += TEMP_STUF_SIZE) {
            if(System.currentTimeMillis() >= getStuffExpireDate(temporalStuff, i)) {
                needRemove = true;
                break;
            }
        }
        if(needRemove) {
            byte[] stuff = new byte[temporalStuff.length];
            int l = 0;
            for(int i = 0; i < temporalStuff.length; i += TEMP_STUF_SIZE) {
                if(System.currentTimeMillis() < getStuffExpireDate(temporalStuff, i)) {
                    System.arraycopy(temporalStuff, i, stuff, l, TEMP_STUF_SIZE);
                    l += TEMP_STUF_SIZE;
                } else if(expiredStuff != null) {
                    expiredStuff.add(readStuffId(temporalStuff, i));
                }
            }

            return Arrays.copyOf(stuff, l);
        } else {
            return temporalStuff;
        }
    }

    public static String toStringTemporalStuff(byte[] temporalStuff) {
        StringBuilder sb = new StringBuilder("[");
        for(int i = 0; i < temporalStuff.length; i += TEMP_STUF_SIZE) {
            sb.append(String.format("%s%s %3$tF %3$tT", (i == 0 ? "" : ", "), readStuffId(temporalStuff, i), new Date(getStuffExpireDate(temporalStuff, i))));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * выдать шапку на время
     *
     * @param profile  профайл игрока
     * @param stuff    предмет
     * @param expire   на сколько выдать шапку
     */
    public boolean addStuff(UserProfile profile, Stuff stuff, long expire, TimeUnit timeUnit) {
        return addStuff(profile, stuff, expire, timeUnit, false);
    }

    public boolean addStuff(UserProfile profile, Stuff stuff, long expire, TimeUnit timeUnit, boolean expand) {
        if(!stuff.isTemporal()) {
            log.error("предмет должен быть временным {}", stuff);
            return false;
        }

        short stuffId = stuff.getStuffId();
        int index = getStuffIndex(profile, stuffId);

        if(index < 0) {
            addStuffFor(profile, stuffId, expire, timeUnit);

            return true;
        } else {
            long expireItemDate = getStuffExpireDate(profile.getTemporalStuff(), index);

            long timeLeft = 0;
            if(expand){
                timeLeft = Math.max(0, expireItemDate - System.currentTimeMillis());
            }
            long expireDate = System.currentTimeMillis() + timeUnit.toMillis(expire) + timeLeft;
            if(expireItemDate != expireDate) {
                writeExpireDate((int) TimeUnit.MILLISECONDS.toSeconds(expireDate), profile.getTemporalStuff(), index);
                profile.setDirty(true);

                return true;
            }
        }
        return false;
    }

    public Tuple3<Integer, Stuff, Integer> findStuff(UserProfile profile, Predicate<Stuff> predicate){
        byte[] temporalStuff = profile.getTemporalStuff();
        for(int i = 0; i < temporalStuff.length; i += TEMP_STUF_SIZE) {
            short stuffId = readStuffId(temporalStuff, i);
            Stuff stuff = stuffCreator.getStuff(stuffId);
            if(stuff != null){
                if(predicate.test(stuff)) {
                    int expireDate = readExpireDate(temporalStuff, i);
                    return Tuple.of(i, stuff, expireDate);
                }
            }
        }
        return null;
    }

    /**
     * проверяем есть ли такой предмет
     */
    public boolean isExist(UserProfile profile, short itemId) {
        return getStuffIndex(profile, itemId) >= 0;
    }

    public long getExpireDate(UserProfile profile, short itemId) {
        return getStuffExpireDate(profile.getTemporalStuff(), getStuffIndex(profile, itemId));
    }

    public int getExpireDateInSeconds(UserProfile profile, short itemId) {
        return readExpireDate(profile.getTemporalStuff(), getStuffIndex(profile, itemId));
    }

    /**
     * @return начальный индекс предмета в массиве temporalStuff или -1
     */
    public int getStuffIndex(UserProfile profile, short itemId) {
        byte[] temporalStuff = profile.getTemporalStuff();
        for(int i = 0; i < temporalStuff.length; i += TEMP_STUF_SIZE) {
            short stuffId = readStuffId(temporalStuff, i);
            if(stuffId == itemId) {
                return i;
            }
        }
        return -1;
    }

    public static long getStuffExpireDate(byte[] temporalStuff, int i) {
        return TimeUnit.SECONDS.toMillis(readExpireDate(temporalStuff, i));
    }

    public static void writeStuffId(short itemId, byte[] temporalStuff, int i) {
        temporalStuff[i] = (byte) ((itemId >>> 8) & 0xFF);
        temporalStuff[i + 1] = (byte) ((itemId) & 0xFF);
    }

    public static short readStuffId(byte[] temporalStuff, int i) {
        int ch1 = temporalStuff[i] & 0xFF;
        int ch2 = temporalStuff[i + 1] & 0xFF;
        return (short) ((ch1 << 8) + (ch2));
    }

    public static void writeExpireDate(int addedDate, byte[] temporalStuff, int i) {
        temporalStuff[i + 2] = (byte) ((addedDate >>> 24) & 0xFF);
        temporalStuff[i + 3] = (byte) ((addedDate >>> 16) & 0xFF);
        temporalStuff[i + 4] = (byte) ((addedDate >>> 8) & 0xFF);
        temporalStuff[i + 5] = (byte) ((addedDate) & 0xFF);
    }

    public static int readExpireDate(byte[] temporalStuff, int i) {
        int ch1 = temporalStuff[i + 2] & 0xFF;
        int ch2 = temporalStuff[i + 3] & 0xFF;
        int ch3 = temporalStuff[i + 4] & 0xFF;
        int ch4 = temporalStuff[i + 5] & 0xFF;
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
    }

    /**
     * добавить новый временный предмет
     */
    public static void addStuffFor(UserProfile userProfile, short itemId, long expire, TimeUnit timeUnit) {
        byte[] temporalStuff = Arrays.copyOf(userProfile.getTemporalStuff(), userProfile.getTemporalStuff().length + TEMP_STUF_SIZE);
        writeStuffId(itemId, temporalStuff, userProfile.getTemporalStuff().length);
        writeItemExpireDate(expire, timeUnit, temporalStuff, userProfile.getTemporalStuff().length);

        userProfile.setTemporalStuff(temporalStuff);
    }

    public static void addStuffFor(UserProfile userProfile, short itemId, int expireInSeconds) {
        byte[] temporalStuff = Arrays.copyOf(userProfile.getTemporalStuff(), userProfile.getTemporalStuff().length + TEMP_STUF_SIZE);
        writeStuffId(itemId, temporalStuff, userProfile.getTemporalStuff().length);
        writeExpireDate(expireInSeconds, temporalStuff, userProfile.getTemporalStuff().length);

        userProfile.setTemporalStuff(temporalStuff);
    }

    /**
     * обновить временный предмет по индексу
     */
    public static void updateStuffFor(UserProfile userProfile, short itemId, int expireInSeconds, int index) {
        byte[] temporalStuff = userProfile.getTemporalStuff();
        writeStuffId(itemId, temporalStuff, index);
        writeExpireDate(expireInSeconds, temporalStuff, index);

        userProfile.setTemporalStuff(temporalStuff);
    }

    public static void writeItemExpireDate(long expire, TimeUnit timeUnit, byte[] temporalStuff, int index) {
        writeExpireDate((int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() + timeUnit.toMillis(expire)), temporalStuff, index);
    }

    /**
     * удалить предмет(шляпу, амулет) из списка предметов игрока
     *
     * @param item id предмета
     */
    public void removeStuffFor(UserProfile profile, short item) {
        if(profile.getTemporalStuff().length == 0) {
            return;
        }
        byte[] stuff = new byte[profile.getTemporalStuff().length - TEMP_STUF_SIZE];
        //далее удаляем шапку из массива шапок
        byte[] temporalStuff = profile.getTemporalStuff();
        int l = 0;
        for(int i = 0; i < temporalStuff.length; i += TEMP_STUF_SIZE) {
            short stuffId = readStuffId(temporalStuff, i);
            if(stuffId != item) {
                System.arraycopy(temporalStuff, i, stuff, l, TEMP_STUF_SIZE);
                l += TEMP_STUF_SIZE;
            }
        }

        profile.setTemporalStuff(stuff);
    }

}
