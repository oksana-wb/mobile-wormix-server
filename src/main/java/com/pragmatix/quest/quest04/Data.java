package com.pragmatix.quest.quest04;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.09.2016 9:32
 */
public class Data {

    public Map<String,Map<Integer, Integer>> progress = new HashMap<>(1);

    public boolean isEmpty() {
        return progress.isEmpty();
    }

}
