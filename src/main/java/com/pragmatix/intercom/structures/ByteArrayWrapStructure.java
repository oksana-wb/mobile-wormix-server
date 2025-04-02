package com.pragmatix.intercom.structures;

import com.pragmatix.serialization.annotations.Structure;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.05.2017 13:06
 */
@Structure
public class ByteArrayWrapStructure {

    public byte[] value;

    public ByteArrayWrapStructure() {
    }

    public ByteArrayWrapStructure(byte[] value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Arrays.toString(value);
    }
}
