package com.pragmatix.admin.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * команда логина с админки 
 * User: denis
 * Date: 28.01.2011
 * Time: 13:22:43
 *
 * @see com.pragmatix.admin.filters.AdminFilter#onLoginByAdmin(LoginByAdmin)
 */
@Command(32)
public class LoginByAdmin {

    public String login;

    /**
	 * автризационный ключ
	 */
	public String password;

    @Override
    public String toString() {
        return "LoginByAdmin{" +
                "login='" + login + '\'' +
                '}';
    }
}
