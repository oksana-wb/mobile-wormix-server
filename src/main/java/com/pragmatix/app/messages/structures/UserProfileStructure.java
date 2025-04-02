package com.pragmatix.app.messages.structures;

import com.pragmatix.app.common.BackpackUtils;
import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.app.model.RentedItems;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.TemporalStuffService;
import com.pragmatix.clan.structures.ClanMemberStructure;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Resize;
import com.pragmatix.serialization.annotations.Serialize;
import com.pragmatix.serialization.annotations.Structure;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

/**
 * Структура для передачи между клиентом и сервером профайла пользователя
 * User: denis
 * Date: 10.11.2009
 * Time: 23:38:25
 */
@Structure
public class UserProfileStructure {
    /**
     * id профайла на сервере
     */
    @Resize(TypeSize.UINT32)
    public long id;
    /**
     * Количество игровых денег
     */
    public int money;
    /**
     * Количество реалов
     */
    public int realMoney;
    /**
     * Рейтинг игрока
     */
    public int rating;
    /**
     * массив доступных id червей
     */
    public WormStructure[] wormsGroup;
    /**
     * массив доступного оружия
     */
    public int[] backpack;
    /**
     * массив шапок, амулетов и тд
     */
    public short[] stuff;
    /**
     * массив предметов имеющих срок действия
     */
    public byte[] temporalStuff;
    /**
     * Скорость реакции игрока
     */
    public int reactionRate;
    /**
     * Строковый ID
     */
    public String profileStringId;

    public short[] recipes;

    public ClanMemberStructure clanMember;
    /**
     * Количество _купленных_ дополнительных слотов для TeamMember'ов (0...3)
     */
    public byte extraGroupSlotsCount;
    /**
     * Арендованное оружие
     */
    @Serialize(ifExpr = "com.pragmatix.app.settings.AppParams.IS_MOBILE()")
    public RentedItems rentedItems;

    public int rankPoints;

    public byte bestRank;

    public UserProfileStructure() {
    }

    public UserProfileStructure(UserProfile userProfile, WormStructure[] wormsGroup, ClanMemberStructure clanMember) {
        this.id = userProfile.getId();
        this.profileStringId = userProfile.getProfileStringId();
        this.money = userProfile.getMoney();
        this.realMoney = userProfile.getRealMoney();
        this.rating = userProfile.getRating();
        this.reactionRate = userProfile.getReactionRate();
        this.stuff = userProfile.getStuff();
        this.temporalStuff = userProfile.getTemporalStuff();
        this.wormsGroup = wormsGroup;
        this.backpack = fillBackpack(userProfile);
        this.recipes = userProfile.getRecipes();
        this.clanMember = clanMember;
        this.extraGroupSlotsCount = userProfile.getExtraGroupSlotsCount();
        this.rankPoints = userProfile.getRankPoints();
        this.bestRank = userProfile.getBestRank();
    }

    public static int[] fillBackpack(UserProfile userProfile) {
        int[] backpack = new int[userProfile.getBackpack().size()];
        int size = userProfile.getBackpack().size();
        for(int i = 0; i < size; i++) {
            BackpackItem item = userProfile.getBackpack().get(i);
            backpack[i] = BackpackUtils.toItem(item);
        }
        return backpack;
    }

    public WormStructure[] wormsGroup() {
        return wormsGroup;
    }

    public int getClanId() {
        return clanMember != null ? clanMember.getClanId() : 0;
    }

    @Override
    public String toString() {
        return "{" +
                id + (StringUtils.isNoneEmpty(profileStringId) ? ":" + profileStringId : "") +
                ", money=" + money +
                ", realMoney=" + realMoney +
                ", rating=" + rating +
                ", rankPoints=" + rankPoints +
                ", maxRank=" + bestRank +
                ", reaction=" + reactionRate +
                (isNotEmpty(temporalStuff) ? ", tempStuff=" + TemporalStuffService.toStringTemporalStuff(temporalStuff) : "") +
                (clanMember != null && clanMember.getClanId() > 0 ? ", clanMember=" + clanMember : "") +
                (rentedItems != null && !rentedItems.isEmpty() ? ", " + rentedItems : "") +
                ", wormsGroup=" + Arrays.toString(wormsGroup()) +
                ", backpack(" + backpack.length + ')' +
//                ", recipes=" + Arrays.toString(recipes) +
                '}';
    }


}
