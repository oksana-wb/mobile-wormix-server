package com.pragmatix.app.messages.structures;

import com.pragmatix.serialization.annotations.Structure;

import java.util.Date;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 14.11.13 10:18
 */
@Structure
public class TemporalStuffStructure {

    public short stuffId;

    /**
     * время когда предмет будет изъят (в секундах)
     */
    public int expireDate;

    public TemporalStuffStructure() {
    }

    public TemporalStuffStructure(short stuffId, int expireDate) {
        this.stuffId = stuffId;
        this.expireDate = expireDate;
    }

    @Override
    public String toString() {
        return "Stuff{" +
                "id=" + stuffId +
                ", expire=" + String.format("%1$tF %1$tT", new Date(expireDate * 1000l)) +
                '}';
    }
}
