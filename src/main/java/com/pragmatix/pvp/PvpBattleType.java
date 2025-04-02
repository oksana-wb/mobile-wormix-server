package com.pragmatix.pvp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 16.11.12 16:25
 */
public enum PvpBattleType implements TypeableEnum {

    // совместное прохождение
    PvE_FRIEND(1, true, 0, true),// с выбранным другом
    PvE_PARTNER(2, true, 1, true),// с подобранным партнером
    // дружеские бои
    FRIEND_PvP(3, false, 0),// с выбранным другом(друзьями)
    // бои на ставку
    WAGER_PvP_DUEL(4, true, 1),// с подобранным соперником один на один
    WAGER_PvP_3_FOR_ALL(8, true, 2),// с подобранными соперниками
    WAGER_PvP_2x2(6, true, LobbyConf.MAX_NEEDED_PARTICIPANTS),// с подобранным партнером против подобобранных соперников
    ;
    private int type;

    // за бой положена награда
    private boolean rewardBattle;

    // сколько участников небходимо подобрать для боя
    private int needParticipants;

    private boolean pveBattle;

    public static final Map<Integer, PvpBattleType> valuesMap = Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(it -> it.type, it -> it));
    
    @JsonCreator
    public static PvpBattleType valueOf(int type) {
        return valuesMap.get(type);
    }
    
    PvpBattleType(int type, boolean rewardBattle, int needParticipants) {
        this.type = type;
        this.rewardBattle = rewardBattle;
        this.needParticipants = needParticipants;
    }

    PvpBattleType(int type, boolean rewardBattle, int needParticipants, boolean pveBattle) {
        this.type = type;
        this.rewardBattle = rewardBattle;
        this.needParticipants = needParticipants;
        this.pveBattle = pveBattle;
    }

    public int getType() {
        return type;
    }

    public boolean isRewardBattle() {
        return rewardBattle;
    }

    public int getNeedParticipants() {
        return needParticipants;
    }

    public boolean isPveBattle() {
        return pveBattle;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }

}
