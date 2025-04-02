package com.pragmatix.app.settings;

import com.pragmatix.craft.domain.Reagent;

import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 04.05.11 11:39
 */
public interface IItemRequirements {

    int needMoney();

    int needRealMoney();

    int needBattles();

    Map<Reagent, Integer> needReagents();

    int needLevel();

}
