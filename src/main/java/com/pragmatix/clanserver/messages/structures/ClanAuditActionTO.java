package com.pragmatix.clanserver.messages.structures;

import com.pragmatix.clanserver.common.ClanActionEnum;
import com.pragmatix.clanserver.utils.Utils;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.serialization.annotations.Structure;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.06.2015 16:03
 */
@Structure
public class ClanAuditActionTO {

    public int date;

    public short action;

    public int publisherId;

    public int memberId;

    public int param;

    @Override
    public String toString() {
        return "{" +
                "date=" + AppUtils.formatDateInSeconds(date) +
                ", action=" + ClanActionEnum.valueOf(action) +
                ", publisherId=" + publisherId +
                ", memberId=" + memberId +
                ", param=" + param +
                '}';
    }
}
