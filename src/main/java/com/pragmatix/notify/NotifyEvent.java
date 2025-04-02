package com.pragmatix.notify;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.08.13 14:27
 */
public class NotifyEvent implements Delayed {
    /**
     * Время отправки сообщения
     */
    public long time;

    /**
     * Сообщение актуально
     */
    public volatile boolean needSend = true;

    public final long profileId;

    public int timeToLive;

    public String localizedKey;
    public String[] localizedArguments;

    public NotifyEvent(long time, long profileId, int timeToLive, String localizedKey, String[] localizedArguments) {
        this.time = time;
        this.profileId = profileId;
        this.timeToLive = timeToLive;
        this.localizedKey = localizedKey;
        this.localizedArguments = localizedArguments;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        if(time == 0) {
            return 0;
        } else {
            return unit.convert(time - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public int compareTo(Delayed o) {
        final long d1 = this.time;
        final long d2 = ((NotifyEvent) o).time;
        return (int) (d1 - d2);
    }

}
