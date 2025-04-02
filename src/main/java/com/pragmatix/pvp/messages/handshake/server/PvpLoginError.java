package com.pragmatix.pvp.messages.handshake.server;

import com.pragmatix.gameapp.common.LoginErrorEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * Команда отправляется сервером в случае ошибки при логине
 */
@Command(2004)
public class PvpLoginError {

    public LoginErrorEnum error;

    public PvpLoginError(LoginErrorEnum typeError) {
        this.error = typeError;
    }

    public PvpLoginError() {
    }

    @Override
    public String toString() {
        return "PvpLoginError{" +
                "error=" + error +
                '}';
    }

}
