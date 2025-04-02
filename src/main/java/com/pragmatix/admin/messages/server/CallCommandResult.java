package com.pragmatix.admin.messages.server;

import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.05.11 18:35
 */
@Command(10035)
public class CallCommandResult {

    public String result;

    public CallCommandResult() {
    }

    public CallCommandResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "CallCommandResult{" +
                "result='" + result + '\'' +
                '}';
    }
}
