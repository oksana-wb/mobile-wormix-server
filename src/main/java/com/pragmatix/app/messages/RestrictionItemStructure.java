package com.pragmatix.app.messages;

import com.pragmatix.app.common.BanType;
import com.pragmatix.app.model.RestrictionItem;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.serialization.annotations.Structure;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.10.2016 9:30
 * @see com.pragmatix.app.model.RestrictionItem
 */
@Structure
public class RestrictionItemStructure {

    public static final RestrictionItemStructure[] EMPTY_ARRAY = new RestrictionItemStructure[0];

    public int endDate;

    public int reason;

    public byte[] blocks;

    public RestrictionItemStructure() {
    }

    public RestrictionItemStructure(int endDate, int reason, byte[] blocks) {
        this.endDate = endDate;
        this.reason = reason;
        this.blocks = blocks;
    }

    @Override
    public String toString() {
        StringBuilder blocksView = new StringBuilder();
        RestrictionItem.BlockFlag[] values = RestrictionItem.BlockFlag.values();
        for(byte block : blocks) {
            blocksView.append(",").append(values[block].name());
        }
        return "{" +
                "endDate=" + AppUtils.formatDateInSeconds(endDate) +
                ", reason=" + BanType.valueOf(reason) +
                ", blocks=[" + blocksView.toString().replaceFirst(",", "") + ']' +
                '}';
    }
}
