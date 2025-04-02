package com.pragmatix.intercom.structures;

import com.pragmatix.app.domain.BackpackConfEntity;
import com.pragmatix.serialization.annotations.Structure;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.05.2017 13:56
 */
@Structure
public class ProfileBackpackConfStructure {

    public short[] config1;
    public short[] config2;
    public short[] config3;
    public byte activeConfig;
    public short[] hotkeys;
    public byte[] seasonsBestRank;

    public ProfileBackpackConfStructure() {
    }

    public ProfileBackpackConfStructure(BackpackConfEntity backpackConfEntity) {
        config1 = backpackConfEntity.getConfig();
        config2 = backpackConfEntity.getConfig2();
        config3 = backpackConfEntity.getConfig3();
        activeConfig = backpackConfEntity.getActiveConfig();
        hotkeys = backpackConfEntity.getHotkeys();
        seasonsBestRank = backpackConfEntity.getSeasonsBestRank();
    }

    public void merge(BackpackConfEntity entity){
        entity.setConfig(config1);
        entity.setConfig2(config2);
        entity.setConfig3(config3);
        entity.setActiveConfig(activeConfig);
        entity.setHotkeys(hotkeys);
        entity.setSeasonsBestRank(seasonsBestRank);
        entity.setDirty(true);
    }

    @Override
    public String toString() {
        return "{" +
                "activeConfig=" + activeConfig +
//                ", config1=" + Arrays.toString(config1) +
//                ", config2=" + Arrays.toString(config2) +
//                ", config3=" + Arrays.toString(config3) +
//                ", hotkeys=" + Arrays.toString(hotkeys) +
                '}';
    }
}
