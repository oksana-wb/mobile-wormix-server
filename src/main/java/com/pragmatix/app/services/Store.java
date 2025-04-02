package com.pragmatix.app.services;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.07.13 14:58
 */
public interface Store {

    <T> T load(String key, Class<T> clazz);

    void save(String key, Object value);

}
