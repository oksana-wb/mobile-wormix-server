package com.pragmatix.pvp.messages;

import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.serialization.annotations.Serialize;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Serialize;
import com.pragmatix.serialization.annotations.Structure;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.11.12 9:26
 */
@Structure
public class PvpProfileStructure extends UserProfileStructure {
    /**
     * имя в соц. сети
     */
    public String profileName;
    /**
     * id соц. сети
     */
    public byte socialNetId;
    /**
     * индекс игрока в бою
     */
    public byte playerNum;
    /**
     * индекс команды
     */
    public byte playerTeam;

    public int dailyRating;

    public short[] backpackConf;
    /**
     * только члены команды идущие в бой
     */
    public WormStructure[] activeTeamMembers;

    public byte[] seasonsBestRank;

    public PvpProfileStructure() {
        activeTeamMembers = new WormStructure[0];
    }

    public PvpProfileStructure(UserProfileStructure userProfileStructure, String profileStringId, String profileName, int socialNetId, int playerNum, int playerTeam, int dailyRating,
                               short[] backpackConf, WormStructure[] activeTeamMembers, byte[] seasonsBestRank) {
        this.id = userProfileStructure.id;
        this.profileStringId = profileStringId;
        this.money = userProfileStructure.money;
        this.realMoney = userProfileStructure.realMoney;
        this.rating = userProfileStructure.rating;
        this.reactionRate = userProfileStructure.reactionRate;
        this.stuff = userProfileStructure.stuff;
        this.temporalStuff = userProfileStructure.temporalStuff;
        this.wormsGroup = userProfileStructure.wormsGroup();
        this.extraGroupSlotsCount = userProfileStructure.extraGroupSlotsCount;
        this.activeTeamMembers = activeTeamMembers;
        this.backpack = userProfileStructure.backpack;
        this.recipes = userProfileStructure.recipes;
        this.clanMember = userProfileStructure.clanMember;
        this.backpackConf = backpackConf;
        this.rankPoints = userProfileStructure.rankPoints;
        this.bestRank = userProfileStructure.bestRank;
        this.seasonsBestRank = seasonsBestRank;
        this.rentedItems = userProfileStructure.rentedItems;

        this.profileName = profileName;
        this.socialNetId = (byte) socialNetId;
        this.playerNum = (byte) playerNum;
        this.playerTeam = (byte) playerTeam;
        this.dailyRating = dailyRating;
    }

    @Override
    public String toString() {
        return "PvpProfileStructure{" +
//                "socialName=" + socialName +
                "id=" + socialNetId + ":" + id +
                ", race=" + masterWorm().map(w -> w.race).orElse((byte) -1) +
                ", skin=" + masterWorm().map(w -> w.skin).orElse((byte) 0) +
                ", playerNum=" + playerNum +
                ", playerTeam=" + playerTeam +
                (StringUtils.isNoneEmpty(profileStringId) ? ", profileStringId=" + profileStringId  : "") +
                ", level=" + getLevel() +
                ", wormsGroup=" + Arrays.toString(wormsGroup()) +
                ", teamSize=" + activeTeamMembers.length +
                ", rankPoints=" + rankPoints +
                ", bestRank=" + bestRank +
//                ", money=" + money +
//                ", realMoney=" + realMoney +
//                ", rating=" + rating +
//                ", wormsGroup count=" + (wormsGroup == null ? 0 : wormsGroup.length) +
                ", backpack(" + (backpack == null ? 0 : backpack.length) + ")" +
//                ", reactionRate=" + reactionRate +
//                ", recipes=" + Arrays.toString(recipes) +
                ", seasonsBestRank=" + Arrays.toString(seasonsBestRank) +
                '}';
    }

    public String mkString() {
        List<WormStructure> activeTeamMembers = Arrays.stream(this.wormsGroup).filter(w -> w.active).collect(Collectors.toList());
        Optional<WormStructure> masterWorm = activeTeamMembers.stream().filter(s -> id == s.ownerId).findFirst();
        return "PvpProfileStructure{" +
                "id=" + socialNetId + ":" + id +
                ", level=" + masterWorm.map(w -> w.level).orElse((byte) 0) +
                ", race=" + masterWorm.map(w -> w.race).orElse((byte) -1) +
                ", skin=" + masterWorm.map(w -> w.skin).orElse((byte) 0) +
                ", playerNum=" + playerNum +
                ", playerTeam=" + playerTeam +
                (StringUtils.isNoneEmpty(profileStringId) ? ", profileStringId=" + profileStringId  : "") +
                ", wormsGroup=" + activeTeamMembers +
                '}';
    }

    public int getLevel() {
        return masterWorm().map(w -> w.level).orElse((byte) 0);
    }

    public Optional<WormStructure> masterWorm() {
        for(WormStructure wormStructure : wormsGroup()) {
            if(wormStructure.ownerId == id) {
                return Optional.of(wormStructure);
            }
        }
        return Optional.empty();
    }

    @Override
    public WormStructure[] wormsGroup() {
        return activeTeamMembers;
    }

}
