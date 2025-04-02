package com.pragmatix.app.messages.structures;

import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.serialization.annotations.Structure;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.04.2014 10:25
 */
@Structure
public class SelectStuffResultStructure {

    public int memberId;

    public SimpleResultEnum resultHat;

    /**
    * id шапки
    */
    public short hatId;

    public SimpleResultEnum resultKit;

    /**
     * id снаряжения
     */
    public short kitId;

    public SelectStuffResultStructure() {
    }

    public SelectStuffResultStructure(int memberId) {
        this.memberId = memberId;
    }

    @Override
    public String toString() {
        return "{" +
                memberId +
                ", resultHat=" + resultHat +
                ", hatId=" + hatId +
                ", resultKit=" + resultKit +
                ", kitId=" + kitId +
                '}';
    }

}
