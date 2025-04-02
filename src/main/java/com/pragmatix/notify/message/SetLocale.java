package com.pragmatix.notify.message;

import com.pragmatix.app.common.*;
import com.pragmatix.app.common.Locale;
import com.pragmatix.serialization.annotations.Command;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.01.14 15:33
 *
 *  @see com.pragmatix.notify.NotifyController#onSetLocale(SetLocale, com.pragmatix.app.model.UserProfile)
 */
@Command(202)
public class SetLocale {

    public com.pragmatix.app.common.Locale locale;

    public SetLocale() {
    }

    public SetLocale(Locale locale) {
        this.locale = locale;
    }

    @Override
    public String toString() {
        return "SetLocale{" +
                "locale=" + locale +
                '}';
    }

}
