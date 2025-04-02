package com.pragmatix.app.messages.server;

import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.08.2016 11:11
 */
@Command(10009)
public class ActivatePromoKeyError {

    public String key;

    public ActivatePromoKeyError() {
    }

    public ActivatePromoKeyError(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "ActivatePromoKeyError{" +
                "key='" + key + '\'' +
                '}';
    }

}
