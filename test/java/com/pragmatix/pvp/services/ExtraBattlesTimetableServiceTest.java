package com.pragmatix.pvp.services;

import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.04.2017 15:54
 */
public class ExtraBattlesTimetableServiceTest extends AbstractSpringTest {

    @Resource
    ExtraBattlesTimetableService extraBattlesTimetableService;


    @Test
    public void printTimetableAsJson() throws Exception {
       println(extraBattlesTimetableService.printTimetableAsJson());
    }

}