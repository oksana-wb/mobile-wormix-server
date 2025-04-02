package com.pragmatix.app.messages.structures;

import com.pragmatix.serialization.annotations.Structure;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.04.2014 10:22
 */
@Structure
public class SelectStuffStructure {

    public int memberId;

    /**
     * id шапки (0-снять)
     */
    public short hatId;

    /**
     * id снаряжения (0-снять)
     */
    public short kitId;

    public SelectStuffStructure() {
    }

    public SelectStuffStructure(int memberId, short hatId, short kitId) {
        this.memberId = memberId;
        this.hatId = hatId;
        this.kitId = kitId;
    }

    @Override
    public String toString() {
        return "{" +
                memberId +
                ", hatId=" + hatId +
                ", kitId=" + kitId +
                '}';
    }
}
