package com.pragmatix.pvp.jskills;

import com.pragmatix.pvp.dsl.WagerDuelBattle;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 29.09.2017 11:26
 */
public class ReactionLevelTest extends AbstractSpringTest {

    @Test
    public void test() throws Exception {
       new WagerDuelBattle(binarySerializer)
               .startBattle(100172153L, 94536713L)
               .finishBattle();
    }

}
