package com.pragmatix.admin.messages.server;

import com.pragmatix.serialization.annotations.Command;

/**
 * Команда посылается сервером при успешном логине в админку
 * User: denis
 * Date: 28.01.2011
 * Time: 13:36:35
 *
 * @see com.pragmatix.admin.filters.AdminFilter#onLoginByAdmin(com.pragmatix.admin.messages.client.LoginByAdmin)
 */
@Command(10024)
public class EnterAdmin {

    public static final byte MODERATOR_ROLE = 1;

    public static final byte SUPER_ADMIN_ROLE = 2;

    /**
     * права которые доступны админу
     */
    public byte role;
    /**
     * Время на сервере
     */
    public String serverTime;

    public EnterAdmin() {
    }

    public EnterAdmin(byte role, String serverTime) {
        this.role = role;
        this.serverTime = serverTime;
    }

    @Override
    public String toString() {
        return String.format("EnterAdmin{" +
                "role=%1s, serverTime=%s", role, serverTime);
    }
}

