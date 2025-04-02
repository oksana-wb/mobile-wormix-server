package com.pragmatix.app.controllers;

import com.pragmatix.app.messages.client.Connect;
import com.pragmatix.app.messages.client.LoginByProfileStringId;
import com.pragmatix.app.messages.client.Ping;
import com.pragmatix.app.messages.server.ConnectResult;
import com.pragmatix.app.messages.server.EnterAccount;
import com.pragmatix.app.messages.server.Pong;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import com.pragmatix.testcase.HttpClientConnection;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 13.06.13 16:47
 */
public class ConnectTest extends AbstractSpringTest {

    public ConnectTest() {
        startServer = false;
    }

    @Test
    public void testOnConnect() {
        HttpClientConnection connection = new HttpClientConnection("http://my.rmart.ru/wormix_mobile/", binarySerializer);
        LoginByProfileStringId login = new LoginByProfileStringId();
        login.id = "1C:AB:A7:47:0F:BB";
        login.socialNet = SocialServiceEnum.mobile;
//        login.authKey = AuthFilter.MASTER_AUTH_KEY;

        connection.send(login);

        EnterAccount enterAccount = connection.receive(EnterAccount.class, 1000);
        System.out.println("sessionKey: " + enterAccount.sessionKey);
        System.out.println("profileStringId: " + enterAccount.userProfileStructure.profileStringId);
        connection.setSessionId(enterAccount.sessionKey);

        connection.send(new Ping());
        connection.receive(Pong.class, 1000);

        Connect connect = new Connect();
        //Connect{socialProfileId=100005804191101, socialNetId=facebook(9), ids=[100000839130427, 100003371354027, 100004725662577, 100005714502155, 100005730941824, 100005782800713]
        connect.socialProfileId = "100005804191101";
        connect.socialNetId = SocialServiceEnum.facebook;
        connect.ids = Arrays.asList("100000839130427", "100003371354027", "100004725662577", "100005714502155", "100005730941824", "100005782800713");

        connection.send(connect);
        connection.receive(ConnectResult.class, 1000);
    }
}
