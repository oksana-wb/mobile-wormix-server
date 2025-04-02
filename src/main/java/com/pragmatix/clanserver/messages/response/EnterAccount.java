package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.LoginBase;
import com.pragmatix.clanserver.messages.structures.ChatMessageTO;
import com.pragmatix.clanserver.messages.structures.ClanMemberTO;
import com.pragmatix.clanserver.messages.structures.ClanTO;
import com.pragmatix.clanserver.messages.structures.NewsTO;
import com.pragmatix.common.utils.ArrayUtils;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.DateTime;
import org.hibernate.annotations.Type;

import java.util.Date;

/**
 * Author: Vladimir
 * Date: 08.04.13 9:18
 */
@Command(value = Messages.LOGIN_RESPONSE)
public class EnterAccount extends CommonResponse<LoginBase> {
    /**
     * Информация о клане
     */
    public ClanTO clan;

    public ChatMessageTO[] chat = ChatMessageTO.EMPTY_ARR;

    public NewsTO[] newsBoard = NewsTO.EMPTY_ARR;

    @DateTime
    public Date startSeasonDate;

    @DateTime
    public Date finishSeasonDate;

    public EnterAccount() {
    }

    public EnterAccount(LoginBase request) {
        super(request);
    }

    public ClanMemberTO getMember(short socialId, int profileId) {
        for (ClanMemberTO member: clan.members) {
            if (member.profileId == profileId && member.socialId == socialId) {
                return member;
            }
        }
        return null;
    }

    @Override
    protected StringBuilder propertiesString() {
        StringBuilder sb = super.propertiesString()
                .append(", clan=").append(clan)
                ;
//        ArrayUtils.append(sb.append(",\n\tchat="), chat);
//        ArrayUtils.append(sb.append(",\n\tnewsBoard="), newsBoard);
        return sb;
    }
}
