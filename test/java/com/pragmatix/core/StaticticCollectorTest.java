package com.pragmatix.core;

import com.pragmatix.app.messages.client.Ping;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import java.util.Random;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.03.12 13:41
 */
public class StaticticCollectorTest extends AbstractSpringTest {

    @Test
    public void test() throws Exception {
        statCollector.disableAll();
//        statCollector.enableFor("Connection#0");
        statCollector.setExportStatOnExit(true);
        statCollector.setExportCheckCount(10);
        statCollector.enableFor("ProtocolSerializer#serialize");
        statCollector.enableFor("ProtocolSerializer#deserialize");
        statCollector.enableFor("ProtocolSerializer#failureDeserialize");

        loginMain();

        while (true) {
            sendMain(new Ping());
            // 10 пингов в секунду
            Thread.sleep(new Random().nextInt(20));
        }
    }
}
