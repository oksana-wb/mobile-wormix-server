package com.pragmatix.app.services.persist;

import com.pragmatix.app.settings.HeroicMissionDailyProgress;
import com.pragmatix.gameapp.services.persist.DefaultKeeperImpl;
import com.pragmatix.server.Server;

import java.io.IOException;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.05.11 14:50
 */
public class HeroicMissionDailyProgressKeeper extends DefaultKeeperImpl {

    @Override
    public void writeObject(Object o) throws IOException {
        HeroicMissionDailyProgress[] array = (HeroicMissionDailyProgress[]) o;
        out.writeInt(array.length);
        for(int i = 0; i < array.length; i++) {
            HeroicMissionDailyProgress progress = array[i];
            out.writeInt(progress.getDefeatCount());
            out.writeInt(progress.winners.size());
            for(Long winner : progress.winners) {
                out.writeInt(winner.intValue());
            }
            Server.sysLog.info("Persisted for level: {} defeatCount={} winCount={}", i + 1, progress.getDefeatCount(), progress.getWinCount());
        }
    }

    @Override
    public <T> T readObject(T objectClass) throws IOException, ClassNotFoundException {
        int size = in.readInt();
        HeroicMissionDailyProgress[] array = new HeroicMissionDailyProgress[size];
        for(int i = 0; i < array.length; i++) {
            HeroicMissionDailyProgress progress = new HeroicMissionDailyProgress();
            progress.setDefeatCount(in.readInt());
            size = in.readInt();
            for(int j = 0; j < size; j++) {
                progress.winners.add((long) in.readInt());
            }
            array[i] = progress;
            Server.sysLog.info("Restored for level: {} defeatCount={} winCount={}", i + 1, progress.getDefeatCount(), progress.getWinCount());
        }
        return (T) array;
    }

}
