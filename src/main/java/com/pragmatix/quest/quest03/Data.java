package com.pragmatix.quest.quest03;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.09.2016 9:32
 */
public class Data {

    public int buySkinForReagent = 0;

    public Map<Short, Integer> unluckyCraftCounts = new HashMap<>(1);

    // история перемещения профиля между соц. сетями
    public List<String[]> moveProfileHistory = new ArrayList<>(1);

    public boolean isEmpty() {
        return buySkinForReagent == 0 && unluckyCraftCounts.isEmpty() && moveProfileHistory.isEmpty();
    }

}
