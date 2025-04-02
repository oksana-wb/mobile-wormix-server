package com.pragmatix.app.messages.structures;

import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Structure;
import com.pragmatix.serialization.annotations.Resize;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.09.11 15:26
 */
@Structure
public class ProfileDoubleKeyStructure {

    @Resize(TypeSize.UINT32)
    public Long longId;

    public String stringId;

    public ProfileDoubleKeyStructure() {
    }

    public ProfileDoubleKeyStructure(Long longId, String stringId) {
        this.longId = longId;
        this.stringId = stringId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ProfileDoubleKeyStructure");
        sb.append("{longId=").append(longId);
        sb.append(", stringId='").append(stringId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
