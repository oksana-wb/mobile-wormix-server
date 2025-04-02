package com.pragmatix.quest.quest01;

import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.06.2016 12:31
 */
public class Data {

    public Date startDate;
    public Date updateDate;
    public byte[] awardSlots = ArrayUtils.EMPTY_BYTE_ARRAY;

    public boolean isEmpty() {
        return updateDate == null;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        Data data = (Data) o;

        if(startDate != null ? !startDate.equals(data.startDate) : data.startDate != null) return false;
        if(updateDate != null ? !updateDate.equals(data.updateDate) : data.updateDate != null) return false;
        return Arrays.equals(awardSlots, data.awardSlots);

    }

    @Override
    public int hashCode() {
        int result = startDate != null ? startDate.hashCode() : 0;
        result = 31 * result + (updateDate != null ? updateDate.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(awardSlots);
        return result;
    }
}
