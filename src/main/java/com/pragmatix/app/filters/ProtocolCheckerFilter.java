package com.pragmatix.app.filters;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.gameapp.security.annotations.Filter;
import com.pragmatix.gameapp.security.annotations.InMessage;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.sessions.IUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Фильтр для проверки входящих защищенных команд
 */
@Filter(checkAncestors = true)
public class ProtocolCheckerFilter {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolCheckerFilter.class);

    @InMessage(SecuredCommand.class)
    public boolean checkMessage(SecuredCommand msg, IUser profile) {
        boolean valid = msg.isValid();
        if(!valid) {
            String cause = msg.secureResult ? "Wrong sessionKey" : "Wrong signature";
            logger.error("msg is invalid! {} {}", cause, msg);
            //закрываем текущее соединение с читером
            Connections.closeConnectionDeferred();
        }
        return valid;
    }

}
