package com.pragmatix.app.model;

import com.pragmatix.app.settings.IItemRequirements;
import com.pragmatix.craft.domain.Reagent;

import java.util.Collections;
import java.util.Map;

/**
 * User: denis
 * Date: 29.09.2010
 * Time: 17:38:36
 */
public interface IItem extends IItemRequirements {

    String getName();

    @Override
    default int needBattles() {
        return 0;
    }

    @Override
    default Map<Reagent, Integer> needReagents() {
        return Collections.emptyMap();
    }

}
