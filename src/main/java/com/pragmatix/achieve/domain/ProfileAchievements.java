package com.pragmatix.achieve.domain;

import com.pragmatix.achieve.common.AchieveUtils;
import com.pragmatix.achieve.messages.server.IncreaseAchievementsResult;
import com.pragmatix.sessions.IUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.07.11 15:22
 */
public abstract class ProfileAchievements implements IUser {

    private static final Logger log = LoggerFactory.getLogger(ProfileAchievements.class);

    public static final int STAT_FIRST_INDEX = 50;

    protected final String profileId;

    public int userProfileId;
    /**
     * сколько баллов игрок уже потратил
     */
    protected byte investedAwardPoints;

    protected short[] achievements;

    protected int[] statistics;

    protected byte[] boolAchievements;

    private volatile boolean dirty = false;

    private volatile boolean newly = false;

    private byte socialId;

    protected int timeSequence;
    /**
     * результат крайнего увеличения достижений
     */
    private IncreaseAchievementsResult lastIncreaseAchievementsResult;

    public final AtomicLong remoteServerRequestNum = new AtomicLong(0);

    public final List<RemoteServerRequestMeta> remoteServerRequestQueue = new CopyOnWriteArrayList<>();

    protected ProfileAchievements(String profileId) {
        this.profileId = profileId;
        achievements = new short[getMaxAchievementIndex() + 1];
        statistics = new int[getMaxStatIndex() + 1];
        boolAchievements = new byte[(int) Math.ceil((double) (getMaxBoolAchievementIndex() + 1) / 8d)];
    }

    @Override
    public Object getId() {
        return profileId;
    }

    public boolean haveBoolAchievement(int achievementIndex) {
        return AchieveUtils.haveBoolAchievement(boolAchievements, achievementIndex);
    }

    public void setBoolAchievement(int achievementIndex, boolean achievementValue) {
        dirty = true;
        AchieveUtils.setBoolAchievement(boolAchievements, achievementIndex, achievementValue);
    }

    public abstract IAchievementName getAchievementNameByIndex(int achievementIndex);

    public abstract int getMaxAchievementIndex();

    public abstract int getMaxStatIndex();

    public abstract int getMaxBoolAchievementIndex();

    public int getAchievement(int achievementIndex) {
        IAchievementName achievementNameByIndex = getAchievementNameByIndex(achievementIndex);
        if(achievementNameByIndex == null) {
            log.error("achievementName not found by index {}", achievementIndex);
        }
        return getAchievement(achievementNameByIndex);
    }

    private int getAchievement(IAchievementName achievementName) {
        return achievementName.isStat() ? statistics[achievementName.getIndex() - STAT_FIRST_INDEX] : AchieveUtils.shortToInt(achievements[achievementName.getIndex()]);
    }

    public void setAchievement(int achievementIndex, int achievementValue) {
        dirty |= getAchievement(achievementIndex) != achievementValue;
        setAchievement(getAchievementNameByIndex(achievementIndex), achievementValue);
    }

    private void setAchievement(IAchievementName achievementName, int achievementValue) {
        if(achievementName.isStat()) {
            statistics[achievementName.getIndex() - STAT_FIRST_INDEX] = achievementValue;
        } else {
            achievements[achievementName.getIndex()] = AchieveUtils.intToShort(achievementValue);
        }
    }

    public String getProfileId() {
        return profileId;
    }

    public byte[] getBoolAchievements() {
        return boolAchievements;
    }

    public void setBoolAchievements(byte[] boolAchievements) {
        if(boolAchievements != null) {
            if(boolAchievements.length < this.boolAchievements.length) {
                this.boolAchievements = Arrays.copyOf(boolAchievements, this.boolAchievements.length);
            } else {
                this.boolAchievements = boolAchievements;
            }
        }
    }

    public short[] getAchievements() {
        return achievements;
    }

    public void setAchievements(short[] achievements) {
        this.achievements = achievements;
    }

    public void setStatistics(int[] statistics) {
        this.statistics = statistics;
    }

    public int[] getStatistics() {
        return statistics;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isNewly() {
        return newly;
    }

    public void setNewly(boolean newly) {
        this.newly = newly;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public byte getSocialId() {
        return socialId;
    }

    public void setSocialId(byte socialId) {
        this.socialId = socialId;
    }

    public byte getInvestedAwardPoints() {
        return investedAwardPoints;
    }

    public void setInvestedAwardPoints(byte investedAwardPoints) {
        dirty = true;
        this.investedAwardPoints = investedAwardPoints;
    }

    public int getTimeSequence() {
        return timeSequence;
    }

    public void setTimeSequence(int timeSequence) {
        dirty = true;
        this.timeSequence = timeSequence;
    }

    public List<Integer> getTrueFlags(){
        return IntStream.rangeClosed(0, getMaxBoolAchievementIndex())
                .filter(this::haveBoolAchievement)
                .boxed()
                .collect(Collectors.toList());
    }

    public void setUserProfileId(int userProfileId) {
        this.userProfileId = userProfileId;
    }

    @Override
    public String toString() {
        return "achieve:" + profileId;
    }

    public IncreaseAchievementsResult getLastIncreaseAchievementsResult() {
        return lastIncreaseAchievementsResult;
    }

    public void setLastIncreaseAchievementsResult(IncreaseAchievementsResult lastIncreaseAchievementsResult) {
        this.lastIncreaseAchievementsResult = lastIncreaseAchievementsResult;
    }
}
