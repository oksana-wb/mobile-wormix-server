package com.pragmatix.admin;

import ch.qos.logback.classic.PatternLayout;
import com.pragmatix.admin.messages.client.*;
import com.pragmatix.admin.messages.server.CallCommandResult;
import com.pragmatix.admin.messages.server.CommandResult;
import com.pragmatix.admin.messages.server.EnterAdmin;
import com.pragmatix.app.messages.client.BuyShopItems;
import com.pragmatix.app.messages.client.LoginByProfileStringId;
import com.pragmatix.app.messages.server.ShopResult;
import com.pragmatix.app.messages.structures.ShopItemStructure;
import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.clanserver.messages.request.LoginRequest;
import com.pragmatix.clanserver.messages.request.LowerInRankRequest;
import com.pragmatix.clanserver.messages.response.EnterAccount;
import com.pragmatix.clanserver.messages.response.LowerInRankResponse;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import com.pragmatix.testcase.SocketClientConnection;
import com.pragmatix.utils.logging.AccountConverter;
import com.pragmatix.utils.logging.ColorOffConverter;
import com.pragmatix.utils.logging.ColorOnConverter;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.10.12 14:56
 */
public class MaintenanceService extends AbstractSpringTest {

    private static Map<String, String[]> connParams = new HashMap<String, String[]>();

    protected SocketClientConnection admimConnection;

    public MaintenanceService() {
        startServer = false;
    }

    static {
        connParams.put("vk", new String[]{"darwin.rmart.ru", "60179", "58027749", "4ha2t6"});
        connParams.put("ok", new String[]{"aurora.rmart.ru", "60279", "6548482", "Qw3rt6uey"});
        connParams.put("fb", new String[]{"aurora.rmart.ru", "60378", "63674136", "q2w3e4"});
        connParams.put("mailru", new String[]{"aurora.rmart.ru", "60479", "63674136", "q2w3e4"});
        connParams.put("mob", new String[]{"lord.rmart.ru", "7669", "63674136", "q2w3e4"});
        connParams.put("android", new String[]{"lord.rmart.ru", "6041", "63674136", "q2w3e4"});
        connParams.put("my_vk", new String[]{"my.rmart.ru", "7369", "58027749", "4рф2е6"});
        connParams.put("my_dev", new String[]{"my.rmart.ru", "7569", "58027749", "4рф2е6"});
        connParams.put("dev", new String[]{"localhost", "7369", "58027749", "58027749"});
    }

    @Test
    public void setValidationTicketToFalse() throws Exception {
//        loginAdmin(connParams.get("ok"));
//        callMaintenanceMethod("setValidationTicketToFalse");
//        loginAdmin(connParams.get("mailru"));
//        callMaintenanceMethod("setValidationTicketToFalse");
        loginAdmin(connParams.get("fb"));
        callMaintenanceMethod("setValidationTicketToFalse");
    }

    public String callMaintenanceMethod(Logger log, String methodName, Object... paramsArr) throws InterruptedException {
        if(log == null) {
            System.out.println(methodName + " start ...");
        }

        CallCommand command = new CallCommand();
        command.command = "execGScript";
        String params = "";
        for(Object param : paramsArr) {
            params += ";" + param;
        }
        command.parametr = "maintenance;" + methodName + params;
        sendAdmin(command);
        CallCommandResult result = admimConnection.receiveNullable(CallCommandResult.class, 10_000_000);
        if(log == null) {
            System.err.println(result.result);
        } else {
            log.info("result: " + result.result);
        }

        if(log == null) {
            System.out.println("done.");
        }

        return result.result;
    }

    public String callMaintenanceMethod(String methodName, Object... paramsArr) throws InterruptedException {
        return callMaintenanceMethod(null, methodName, paramsArr);
    }

    public void loginAdmin(String[] params) throws Exception {
        if(params == null || params.length == 0) {
            return;
        }
        String host = params[0];
        int port = Integer.parseInt(params[1]);
        String login = params[2];
        String password = params[3];

        admimConnection = new SocketClientConnection(binarySerializer).connect(host, port);
        LoginByAdmin message = new LoginByAdmin();
        message.login = login;
        message.password = password;

        sendAdmin(message);
        admimConnection.receive(EnterAdmin.class, 1000);
    }

    public void sendAdmin(Object message) throws InterruptedException {
        admimConnection.send(message);
    }

}
