package com.pragmatix.app.settings;

import com.pragmatix.app.services.HeroicMissionService;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Resize;
import com.pragmatix.serialization.annotations.Serialize;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.07.13 14:01
 */
@Structure(annotatedOnly = true)
public class HeroicMissionState {

    private short[] currentMissionIds;

    private int currentMissionMap;

    private List<String> missionsHistory;

    private Map<String, Integer> lastMissionsMap;

    public String getCurrentMission() {
        return HeroicMissionService.missionIdsAsString(currentMissionIds);
    }

    public void setCurrentMission(String currentMission) {
        this.currentMissionIds = HeroicMissionService.missionIdsAsArray(currentMission);
    }

    public int getCurrentMissionMap() {
        return currentMissionMap;
    }

    public void setCurrentMissionMap(int currentMissionMap) {
        this.currentMissionMap = currentMissionMap;
    }

    public Map<String, Integer> getLastMissionsMap() {
        return lastMissionsMap;
    }

    public void setLastMissionsMap(Map<String, Integer> lastMissionsMap) {
        this.lastMissionsMap = lastMissionsMap;
    }

    public short[] getCurrentMissionIds() {
        return currentMissionIds;
    }

    public void setCurrentMissionIds(short[] currentMissionIds) {
        this.currentMissionIds = currentMissionIds;
    }

    public List<String> getMissionsHistory() {
        return missionsHistory;
    }

    public void setMissionsHistory(List<String> missionsHistory) {
        this.missionsHistory = missionsHistory;
    }

    @Serialize
    public short[] getMissionIds() {
        return currentMissionIds;
    }

    @Serialize
    @Resize(TypeSize.UINT32)
    public long getMapId() {
        return currentMissionMap;
    }

    public void setMissionIds(short[] missionIds) {
        currentMissionIds = missionIds;
    }

    public void setMapId(long mapId) {
        currentMissionMap = (int) mapId;
    }

    public String toString() {
        return "HeroicMissionState{" +
                "missionIds=" + Arrays.toString(getMissionIds()) +
                ", mapId=" + getMapId() +
                '}';
    }

    public String mkString() {
        return "HeroicMissionState{" +
                "currentMissionIds=" + Arrays.toString(currentMissionIds) +
                ", currentMissionMap=" + currentMissionMap +
                ", missionsHistory=" + missionsHistory +
                ", lastMissionsMap=" + lastMissionsMap +
                '}';
    }
}
