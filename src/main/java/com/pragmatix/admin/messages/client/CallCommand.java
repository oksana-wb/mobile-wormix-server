package com.pragmatix.admin.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * Вызов метода по его имени
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.05.11 18:32
 *
   @see com.pragmatix.admin.controllers.AdminController#onCallCommand(CallCommand, com.pragmatix.admin.model.AdminProfile)
 * @see com.pragmatix.admin.messages.server.CallCommandResult
 */
@Command(47)
public class CallCommand {

    public String command;

    public String parametr;

    public CallCommand() {
    }

    public CallCommand(String command, String parametr) {
        this.command = command;
        this.parametr = parametr;
    }

    @Override
    public String toString() {
        return "CallCommand{" +
                "command='" + command + '\'' +
                ", parametr='" + parametr + '\'' +
                '}';
    }
}
