package com.pragmatix.clanserver.messages.event;

import com.pragmatix.serialization.annotations.Structure;

/**
 * Author: Vladimir
 * Date: 15.04.2013 09:31
 */
@Structure(isAbstract = true)
public abstract class AbstractEvent {
    public abstract int getCommandId();

    public String toString() {
        return getClass().getSimpleName() + "{" + propertiesString() + '}';
    }

    protected StringBuilder propertiesString() {
        return new StringBuilder();
    }
}
