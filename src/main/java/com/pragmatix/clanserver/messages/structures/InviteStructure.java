package com.pragmatix.clanserver.messages.structures;

import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.DateTime;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Resize;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Date;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 29.04.13 15:05
 */
@Structure
public class InviteStructure {

    public int clanId;

    public String clanName;

    public byte[] clanEmblem;

    /**
     * Идентификатор соцсети пригласившей стороны
     */
    @Ignore
    public short socialId;

    /**
     * Идентификатор профиля пригласившейстороны
     */
    @Resize(TypeSize.UINT32)
    public long profileId;

    /**
     * Идентификатор соц. сети профиля пригласившей стороны
     */
    public String stringProfileId;

    /**
     * Должность в клане пригласившей стороны
     */
    public Rank rank;

    /**
     * Имя пригласившей стороны
     */
    public String name;

    /**
     * Дата приглашения
     */
    @DateTime
    public Date inviteDate;


    @Override
    public String toString() {
        return "{" +
                "clanId=" + clanId +
                ", clanName='" + clanName + '\'' +
                ", profileId=" + profileId +
                ", stringProfileId='" + stringProfileId + '\'' +
                ", rank=" + rank +
                ", name='" + name + '\'' +
                ", inviteDate=" + inviteDate +
                '}';
    }

}
