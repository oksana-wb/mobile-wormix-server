package com.pragmatix.pvp.model;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.12.12 12:55
 */
public class CyclicSet<T> {

    private final T[] buffer;

    private int poiner = 0;

    public CyclicSet(int size, Class<T> clazz) {
        buffer = (T[]) Array.newInstance(clazz, size);
    }

    public void add(T item) {
        if(!contains(item)) {
            buffer[poiner] = item;
            poiner++;
            if(poiner >= buffer.length) {
                poiner = 0;
            }
        }
    }

    public boolean contains(T item) {
        for(T it : buffer) {
            if(it != null && it.equals(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return Arrays.toString(buffer);
    }

}
