package com.pragmatix.clanserver.messages.request;

/**
 * Author: Vladimir
 * Date: 05.04.2013 09:15
 */
public abstract class AbstractRequest {
    public abstract int getCommandId();

    public String toString() {
        return getClass().getSimpleName() + "{" + propertiesString() + '}';
    }

    protected StringBuilder propertiesString() {
        return new StringBuilder();
    }

    protected StringBuilder appendComma(StringBuilder properties) {
        if (properties.length() > 0) {
            properties.append(", ");
        }

        return properties;
    }
}
