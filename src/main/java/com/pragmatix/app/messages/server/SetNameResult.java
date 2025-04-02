package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.serialization.annotations.Command;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 12.03.14 17:00
 */
@Command(10103)
public class SetNameResult {

    public SimpleResultEnum result;

    public byte renameAct;

    public String name;

    public SetNameResult() {
    }

    public SetNameResult(SimpleResultEnum result, int renameAct, String name) {
        this.result = result;
        this.renameAct = (byte)renameAct;
        this.name = name;
    }

    @Override
    public String toString() {
        return "SetNameResult{" +
                "result=" + result +
                ", renameAct=" + renameAct +
                ", name='" + name + '\'' +
                '}';
    }
}
