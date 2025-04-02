package com.pragmatix.app.login;

import com.pragmatix.app.messages.client.CrashLog;
import com.pragmatix.app.messages.client.Ping;
import com.pragmatix.testcase.AbstractSpringTest;
import com.pragmatix.testcase.SocketClientConnection;
import com.pragmatix.testcase.WebSocketClientConnection;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.06.2017 15:03
 */
public class WebSocketTest extends AbstractSpringTest {

    @Test
    public void onCrashLogTest() throws Exception {
        SocketClientConnection connection = new WebSocketClientConnection(binarySerializer).connect("127.0.0.1", 6001);

        CrashLog crashLog = new CrashLog();
        crashLog.userId = "" + testerProfileId;
        crashLog.platform = "test";
        crashLog.log = StringUtils.repeat(" ", 5_000);
        connection.send(crashLog);

        Thread.sleep(300L);
    }

}
