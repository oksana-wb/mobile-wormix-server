package com.pragmatix.clanserver.domain;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Author: Vladimir
 * Date: 04.04.13 9:36
 */
public class Clan implements Cloneable {
    private static final int FLAG_DIRTY = 0x02;

    public Integer id;

    public volatile int flags;

    public volatile String name;

    public volatile int level;

    public volatile int size;

    public volatile int rating;

    public volatile int seasonRating;

    public volatile int joinRating = -1;

    public volatile Date createDate;

    public volatile byte[] emblem;

    public volatile String description = "";

    public volatile ReviewState reviewState = ReviewState.NONE;

    /**
     * место занимаемое кланов в предыдущем (закрытом) сезоне
     */
    public int prevSeasonTopPlace;

    public final List<ChatMessage> chat = new CopyOnWriteArrayList<>();

    public final List<News> newsBoard = new CopyOnWriteArrayList<>();

    public final Map<Object, ClanMember> memberDict = new ConcurrentHashMap<>();

    public volatile boolean closed = false;

    // казна
    public volatile int treas;

    public volatile byte medalPrice;

//    public volatile boolean medalPriceChanged;

    public volatile int cashedMedals;

    public ClanMember getLeader() {
        return member(Rank.LEADER);
    }

    public ClanMember getMember(Object memberId) {
        return memberDict.get(memberId);
    }

    public ClanMember getMember(short socialId, int profileId) {
        return getMember(ClanMember.getId(socialId, profileId));
    }

    public Collection<ClanMember> members() {
        return memberDict.values();
    }

    public String normalName() {
        return normalName(name);
    }

    public static String normalName(String name) {
        return NameNormalizer.normalizeName(name);
    }

    public List<ClanMember> members(Rank rank) {
        List<ClanMember> members = new ArrayList<>();
        for (ClanMember member: memberDict.values()) {
            if (member.rank == rank) {
                members.add(member);
            }
        }
        return members;
    }

    public ClanMember member(Rank rank) {
        for (ClanMember member: memberDict.values()) {
            if (member.rank == rank) {
                return member;
            }
        }
        return null;
    }

    public int capacity() {
        return ClanLevel.get(level).capacity;
    }

    public int size(Rank rank) {
        int i = 0;
        for (ClanMember member: members()) {
            if (member.rank == rank) {
                i++;
            }
        }
        return i;
    }

    public int vacancies(Rank rank) {
        if (rank == Rank.SOLDIER) {
            return capacity() - size;
        } else if (rank == Rank.OFFICER) {
            return ClanLevel.get(level).officers - size(Rank.OFFICER);
        } else {
            return 0;
        }
    }

    public void accept(ClanMember member) {
        if (memberDict.containsKey(member.getId())) {
            throw new RuntimeException("Уже в клане " + member);
        }

        member.clan = this;
        memberDict.put(member.getId(), member);

        refreshAggregates();
    }

    public void expel(ClanMember member) {
        memberDict.remove(member.getId());

        member.clan = null;

        refreshAggregates();
    }

    public void refreshAggregates() {
        int size = memberDict.size();
        int rating = 0;
        int seasonRating = 0;
        int membersCashedMedals = 0;

        for (ClanMember member: members()) {
            rating += member.rating;
            seasonRating += member.seasonRating;
            membersCashedMedals += member.cashedMedals;
        }
        seasonRating += Math.max(0, cashedMedals - membersCashedMedals) * 1000;

        if (size != this.size || rating != this.rating || seasonRating != this.seasonRating) {
            this.size = memberDict.size();
            this.rating = rating;
            this.seasonRating = seasonRating;
            setDirty(true);
        }
    }

    public boolean isDirty() {
        return isFlag(FLAG_DIRTY);
    }

    public void setDirty(boolean value) {
        setFlag(FLAG_DIRTY, value);
    }

    public Clan copy() {
        try {
            return (Clan) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Clan");
        sb.append("{id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", level=").append(level);
        sb.append(", size=").append(size);
        sb.append(", rating=").append(rating);
        sb.append(", joinRating=").append(joinRating);
        sb.append(", createDate=").append(createDate);
        sb.append(", emblem=").append(emblem == null ? "null" : "");
        sb.append(", description=").append(description);
        sb.append(", prevSeasonTopPlace=").append(prevSeasonTopPlace);
        sb.append(", reviewState=").append(reviewState);
        for (int i = 0; emblem != null && i < emblem.length; ++i)
            sb.append(i == 0 ? "" : ", ").append(emblem[i]);
        sb.append(", members=").append(members());
        sb.append(", chat=").append(chat);
        sb.append(", newsBoard=").append(newsBoard);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Clan)) return false;

        Clan clan = (Clan) o;

        return id.equals(clan.id);
    }

    @Override
    public int hashCode() {
        return id;
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

    public boolean isMedalPriceChanged() {
        return medalPrice > 0;
    }
}
