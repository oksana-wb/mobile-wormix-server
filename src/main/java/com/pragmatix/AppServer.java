package com.pragmatix;

import com.pragmatix.server.Server;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 25.09.2017 14:25
 */
public class AppServer {

    public static void main(String[] args) {
        Server.run("beans.xml");
    }

}
