package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.LoginErrorEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * Команда отправляеться сервером в случаи ошибки при логине
 */
@Command(10002)
public class LoginError {

    public LoginErrorEnum error;

    public LoginError() {
    }

    public LoginError(LoginErrorEnum error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "LoginError{"
                + error +
                '}';
    }
}
