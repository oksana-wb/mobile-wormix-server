package com.pragmatix.app.services.persist;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.09.12 18:23
 */
public class WhoPumpedReactionByDaysKeeperPerfomanceTest {

    @Test
    public void testReadObject() throws Exception {
        WhoPumpedReactionByDaysKeeper keeper = new WhoPumpedReactionByDaysKeeper();
        keeper.prepareRead(new FileInputStream(new File("d:/temp/ReactionRateService.whoPumpedRateByDays")));
        List result = keeper.readObject(new ArrayList());
        System.out.println(result.size());
        Thread.sleep(Long.MAX_VALUE);
        System.out.println(result.size());
    }

}
