package com.pragmatix.pvp.model;

import com.pragmatix.pvp.messages.handshake.client.CreateBattleRequest;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf;
import com.pragmatix.sessions.IAppServer;
import com.pragmatix.sessions.IUser;

import java.util.Arrays;

/**
 * Класс игрока на pvp сервере
 */
public class PvpUser implements IUser {

    /**
     * идентификатор пользователя
     */
    private final long profileId;

    private final String profileStringId;

    private final byte socialNetId;

    private final Long pvpUserId;

    private final IAppServer mainServer;

    private String profileName;

    private volatile long battleId;

    private long lastChatMessageTime;

    private CreateBattleRequest createBattleRequest;

    private final long[] lastMatchmakingOpponents = new long[LobbyConf.MAX_NEEDED_PARTICIPANTS];

    private volatile boolean online;

    private int minDailyRating;

    public int clanId;

    public long connectTime = 0;
    public volatile long disconnectTime = 0;

    public PvpUser(long profileId, String profileStringId, byte socialNetId, IAppServer mainServer) {
        this.profileId = profileId;
        this.profileStringId = profileStringId;
        this.socialNetId = socialNetId;
        this.pvpUserId = PvpService.getPvpUserId(profileId, socialNetId);
        this.mainServer = mainServer;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public void setBattleId(long battleId) {
        this.battleId = battleId;
    }

    public Long getId() {
        return pvpUserId;
    }

    public long getBattleId() {
        return battleId;
    }

    @Override
    public byte getSocialId() {
        return socialNetId;
    }

    public long getLastChatMessageTime() {
        return lastChatMessageTime;
    }

    public void setLastChatMessageTime(long lastChatMessageTime) {
        this.lastChatMessageTime = lastChatMessageTime;
    }

    public IAppServer getMainServer() {
        return mainServer;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        PvpUser pvpUser = (PvpUser) o;

        if(!pvpUserId.equals(pvpUser.pvpUserId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return pvpUserId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("battleId=%s %s:%s", battleId, socialNetId, profileId);
    }

    public String getProfileName() {
        return profileName;
    }

    public long getProfileId() {
        return profileId;
    }

    public byte getSocialNetId() {
        return socialNetId;
    }

    public CreateBattleRequest getCreateBattleRequest() {
        return createBattleRequest;
    }

    public void setCreateBattleRequest(CreateBattleRequest createBattleRequest) {
        this.createBattleRequest = createBattleRequest;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getProfileStringId() {
        return profileStringId;
    }

    public void cleanLastMatchmakingOpponents(){
        Arrays.fill(lastMatchmakingOpponents, 0);
    }

    public long[] getLastMatchmakingOpponents() {
        return lastMatchmakingOpponents;
    }

    public boolean isLastMatchmakingOpponent(long pvpUserId) {
        for(long lastMatchmakingOpponent : lastMatchmakingOpponents) {
            if(lastMatchmakingOpponent == 0) {
                break;
            } else if(lastMatchmakingOpponent == pvpUserId) {
                return true;
            }
        }
        return false;
    }

    public void addLastMatchmakingOpponent(long pvpUserId) {
        for(int i = 0; i < lastMatchmakingOpponents.length; i++) {
            if(lastMatchmakingOpponents[i] == 0) {
                lastMatchmakingOpponents[i] = pvpUserId;
                break;
            }
        }
    }

    public int getMinDailyRating() {
        return minDailyRating;
    }

    public void setMinDailyRating(int minDailyRating) {
        this.minDailyRating = minDailyRating;
    }
}
