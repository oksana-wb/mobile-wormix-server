package com.pragmatix.app.services.rating;

import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.server.Server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.05.2016 11:54
 */
public class ProgressData {

    private final int PROGRESS_DEEP;

    private Map<Long, short[]> progressMap = new ConcurrentHashMap<>();

    private final String progressKeepFileName;

    public ProgressData(int PROGRESS_DEEP, String progressKeepFileName) {
        this.PROGRESS_DEEP = PROGRESS_DEEP;
        this.progressKeepFileName = progressKeepFileName;
    }

    public void clean() {
        progressMap = new ConcurrentHashMap<>();
    }

    public void restoreFromDisk(PersistenceService persistenceService) {
        try {
            Map<Long, short[]> progressMap = persistenceService.restoreObjectFromFile(Map.class, progressKeepFileName);
            if(progressMap != null && progressMap.size() > 0) {
                Server.sysLog.info("{}: Loaded progress from file for {} items", progressKeepFileName, progressMap.size());
                this.progressMap = progressMap;
            }
        } catch (Exception e) {
            Server.sysLog.error(progressKeepFileName + ": Error during restore progress: " + e.toString(), e);
        }
    }

    public void persistToDisk(PersistenceService persistenceService) {
        persistenceService.persistObjectToFile(progressMap, progressKeepFileName);
        Server.sysLog.info("{}: Persisted progress for {} items", progressKeepFileName, progressMap.size());
    }

    public void updateProgress(List<Long> uids) {
        Set<Long> updatedUids = new HashSet<>();
        for(int i = 0; i < uids.size(); i++) {
            Long uid = uids.get(i);
            updateProgress(uid, i);
            updatedUids.add(uid);
        }
        progressMap.keySet().stream()
                .filter(uid -> !updatedUids.contains(uid))
                .forEach(this::setEmptyPositionAndRemoveEmptiedProgress);
    }

    public int getOldPlace(Long uid) {
        short[] progress = progressMap.get(uid);
        return progress == null ? 0 : progress[0] & 0x0000FFFF;
    }

    public int getCurrentPlace(Long uid) {
        short[] progress = progressMap.get(uid);
        return progress == null ? 0 : progress[PROGRESS_DEEP - 1] & 0x0000FFFF;
    }

    private void updateProgress(Long uid, int position) {
        short[] progress = progressMap.get(uid);
        short positionAsShort = (short) ((position + 1) & 0x0000FFFF);
        if(progress != null) {
            progress = Arrays.copyOf(progress, progress.length);
            System.arraycopy(progress, 1, progress, 0, progress.length - 1);
            progress[progress.length - 1] = positionAsShort;
            progressMap.put(uid, progress);
        } else {
            progress = new short[PROGRESS_DEEP];
            Arrays.fill(progress, positionAsShort);
            progressMap.put(uid, progress);
        }
    }

    private void setEmptyPositionAndRemoveEmptiedProgress(Long uid) {
        short[] progress = progressMap.get(uid);
        if(progress != null) {
            for(int i = 1; i < progress.length; i++) {
                // присутствует минимум одна положительная позиция
                if(progress[i] != 0) {
                    progress = Arrays.copyOf(progress, progress.length);
                    System.arraycopy(progress, 1, progress, 0, progress.length - 1);
                    progress[progress.length - 1] = 0;
                    progressMap.put(uid, progress);
                    return;
                }
            }
            // всё по нулям
            progressMap.remove(uid);
        }
    }
}
