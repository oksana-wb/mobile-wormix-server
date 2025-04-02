package com.pragmatix.admin.messages.server;

import com.pragmatix.serialization.annotations.Command;

/**
 * результат работы команды
 * User: denis
 * Date: 13.12.2009
 * Time: 22:06:54
 */
@Command(10010)
public class CommandResult {             

    public String status;

    public CommandResult(String status) {
        this.status = status;
    }

    public CommandResult() {
    }

    @Override
    public String toString() {
        return "CommandResult{" +
                "status='" + status + '\'' +
                '}';
    }
}
