package com.pragmatix.clanserver.domain;

import com.pragmatix.sessions.IUser;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Date;

/**
 * Author: Vladimir
 * Date: 04.04.13 10:45
 */
public class ClanMember implements Cloneable, IUser {
    private static final int FLAG_NEW = 0x01;
    private static final int FLAG_DIRTY = 0x02;
    private static final int FLAG_ONLINE = 0x04;

    public final short socialId = 0;

    public int profileId;

    public String socialProfileId;

    public volatile Clan clan;

    public volatile Rank rank;

    public volatile String name;

    public volatile Date joinDate;

    //идентификатор пригласившего игрока
    public volatile int hostProfileId;

    public volatile Date loginDate;

    public volatile Date logoutDate;

    public volatile int flags;

    public volatile Invite[] invites = Invite.EMPTY_ARR;

    public volatile int rating;

    public volatile int seasonRating;

    /**
     * позиция в ТОП-е клана сутки назад
     */
    public volatile int oldPlace;

    // в секундах
    public volatile int lastLoginTime;

    // общий донат со скидкой
    public volatile int donation;

    // донат с учетом скидки (для списывания из казны в качестве компенсации при исключении)
    public volatile int donationCurrSeason;
    public volatile int donationPrevSeason;

    public volatile int cashedMedals;

    // донат без скидки (для возврата игроку)
    public volatile int donationCurrSeasonComeback;
    public volatile int donationPrevSeasonComeback;

    public long lastChatMessageTime;

    // возможность включить/отключит офицерам выгонять участников
    public boolean expelPermit = true;
    // возможность включить/отключить молчанку для игрока
    public boolean muteMode = false;

    // рейтинг по дням в формате (дата(в секундах), значение)
    public int[] dailyRating = ArrayUtils.EMPTY_INT_ARRAY;

    public int trimInvites(int lifeTime) {
        if(invites.length > 0) {
            long trimTime = System.currentTimeMillis() - 1000 * lifeTime;
            int i = 0;

            for(Invite invite : invites) {
                if(invite.inviteDate.getTime() > trimTime) {
                    invites[i++] = invite;
                }
            }

            if(i == 0) {
                invites = Invite.EMPTY_ARR;
            } else if(i < invites.length) {
                invites = Arrays.copyOf(invites, i);
            }
        }

        return invites.length;
    }

    public int addInvite(Invite invite) {
        int i = invites.length;
        invites = Arrays.copyOf(invites, i + 1);
        invites[i] = invite;

        return invites.length;
    }

    public Invite removeInvite(short socialId, int profileId) {
        int i = 0;
        Invite res = null;
        for(Invite invite : invites) {
            if(invite.socialId == socialId && invite.profileId == profileId) {
                res = invite;
            } else {
                invites[i++] = invite;
            }
        }
        if(res != null) {
            invites = Arrays.copyOf(invites, i);
        }
        return res;
    }

    @Override
    public String toString() {
        return String.format("clanId=%s %s", clan != null ? clan.id : 0, profileId);
    }

    public String print() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ClanMember");
        sb.append("{profileId=").append(profileId);
        sb.append(", socialId=").append(socialId);
        sb.append(", clanId=").append(clan != null ? clan.id : null);
        sb.append(", rank=").append(rank);
        sb.append(", name='").append(name).append('\'');
        sb.append(", socialProfileId='").append(socialProfileId).append('\'');
        sb.append(", joinDate=").append(joinDate);
        sb.append(", loginDate=").append(loginDate);
        sb.append(", logoutDate=").append(logoutDate);
        sb.append(", flags=").append(Integer.toBinaryString(flags));
        sb.append(", rating=").append(rating);
        sb.append(", seasonRating=").append(seasonRating);
        sb.append(", oldPlace=").append(oldPlace);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;

        if(!(o instanceof ClanMember)) return false;

        ClanMember clanMember = (ClanMember) o;

        return getId().equals(clanMember.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public ClanMember clone() {
        try {
            return (ClanMember) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    private Long id;

    @Override
    public Object getId() {
        long id = profileId;
        if(this.id == null || this.id != id) {
            this.id = id;
        }
        return this.id;
    }

    public Long getClanMemberId(){
        return (Long) getId();
    }

    public static long getId(short socialId, int profileId) {
        return profileId;
    }

    public Integer getClanId() {
        return clan != null ? clan.id : null;
    }

    @Override
    public byte getSocialId() {
        return (byte) socialId;
    }

    public boolean isNew() {
        return isFlag(FLAG_NEW);
    }

    public void setNew(boolean value) {
        setFlag(FLAG_NEW, value);
    }

    public boolean isDirty() {
        return isFlag(FLAG_DIRTY);
    }

    public void setDirty(boolean value) {
        setFlag(FLAG_DIRTY, value);
    }

    public boolean isOnline() {
        return isFlag(FLAG_ONLINE);
    }

    public void setOnline(boolean value) {
        setFlag(FLAG_ONLINE, value);
    }

    private boolean isFlag(int flag) {
        return (flags & flag) != 0;
    }

    private void setFlag(int flag, boolean value) {
        if(value) {
            flags |= flag;
        } else {
            flags &= ~flag;
        }
    }

    public boolean isSelf(short socialId, int profileId) {
        return this.socialId == socialId && this.profileId == profileId;
    }
}
