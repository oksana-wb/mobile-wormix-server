package com.pragmatix.app.messages.server;

import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Arrays;
import java.util.Collection;

/**
 * Команда для передачи клиенту массива id игроков отсылавших ранее профилю предложение вернуться в игру
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.09.12 13:14
 */
@Command(10091)
public class CallbackersList {

    @Resize(TypeSize.UINT32)
    public long[] callbackers;

    public String[] callbackersString;

    public CallbackersList() {
    }

    public CallbackersList(Collection<Long> callbackers, String[] callbackersString) {
        this.callbackers = new long[callbackers.size()];
        int i = 0;
        for(Long callbacker : callbackers) {
            this.callbackers[i] = callbacker;
            i++;
        }
        this.callbackersString = callbackersString;
    }

    @Override
    public String toString() {
        return "CallbackersList{" +
                "callbackers=" + (callbackers == null ? null : Arrays.asList(callbackers)) +
                "callbackersString=" + (callbackersString == null ? null : Arrays.asList(callbackersString)) +
                '}';
    }

}
