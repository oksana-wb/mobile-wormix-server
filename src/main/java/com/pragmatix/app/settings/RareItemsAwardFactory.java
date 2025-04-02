package com.pragmatix.app.settings;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.init.StuffCreator;
import com.pragmatix.app.messages.structures.BundleStructure;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.craft.domain.Reagent;
import com.pragmatix.craft.services.CraftService;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 22.12.2015 17:11
 */
public class RareItemsAwardFactory extends BundleStructure {

    @Resource
    private StuffCreator stuffCreator;

    @Resource
    private CraftService craftService;

    private List<CraftBoxItem> craftBoxItems;

    private int[][] resultAwardMassMap;

    public RareItemsAwardFactory(String code, float votes, List<CraftBoxItem> craftBoxItems) {
        this.code = code;
        this.votes = votes;
        this.craftBoxItems = craftBoxItems;

        int allAwardsMass = 0;
        resultAwardMassMap = new int[craftBoxItems.size() + 1][2];
        for(int i = 0; i < craftBoxItems.size(); i++) {
            resultAwardMassMap[i + 1][0] = i;
            int mass = craftBoxItems.get(i).chance;
            resultAwardMassMap[i + 1][1] = mass;
            allAwardsMass += mass;
        }
        // по нулевому индексу сумму всех весов
        resultAwardMassMap[0][1] = allAwardsMass;
    }

    @Override
    public GenericAwardStructure[] getItems(UserProfile profile) {
        boolean rollDice = stuffCreator.getStuffs().stream()
                        .filter(Stuff::isCraftBase)
                        .map(Stuff::getStuffId)
                        .anyMatch(rareItemId -> craftService.isHaveItemOrCraftBasedOn(rareItemId, profile));

        if(rollDice){
            CraftBoxItem craftBoxItem = craftBoxItems.get(CraftService.rollDice(resultAwardMassMap));
            if(craftBoxItem instanceof CraftBoxItem_RareItem){
                return getRareAwards(profile, craftBoxItem.count);
            } else {
              return new GenericAwardStructure[] {craftBoxItem};
            }
        } else {
            Integer rareItemsCount = craftBoxItems.stream().filter(item -> item instanceof CraftBoxItem_RareItem).findFirst().map(item -> item.count).orElse(1);
            return getRareAwards(profile, rareItemsCount);
        }
    }

    private GenericAwardStructure[] getRareAwards(UserProfile profile, int count) {
        final Set<Short> granted = new HashSet<>();
        return IntStream.range(0, count).mapToObj(i -> getRareAward(profile, granted)).toArray(GenericAwardStructure[]::new);
    }

    private GenericAwardStructure getRareAward(UserProfile profile, Set<Short> granted) {
        int[] absentRareItems = stuffCreator.getStuffs().stream()
                .filter(Stuff::isCraftBase)
                .map(Stuff::getStuffId)
                .filter(rareItemId -> !craftService.isHaveItemOrCraftBasedOn(rareItemId, profile) && !granted.contains(rareItemId))
                .mapToInt(Integer::valueOf)
                .toArray();
        GenericAwardStructure award;
        if(absentRareItems.length == 0) {
            award = new GenericAwardStructure(AwardKindEnum.REAL_MONEY, craftService.rubyCountInsteadOfExistsRareItem);
        } else {
            int rareItemId = absentRareItems[ThreadLocalRandom.current().nextInt(absentRareItems.length)];
            award = new GenericAwardStructure(AwardKindEnum.STUFF, 1, rareItemId);
            granted.add((short) rareItemId);
        }
        return award;
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    public static class CraftBoxItem extends GenericAwardStructure {

        public final int chance;

        public CraftBoxItem(int chance, AwardKindEnum awardKind, int count, int itemId) {
            super(awardKind, count, itemId);
            this.chance = chance;
        }
    }

    public static class CraftBoxItem_RareItem extends CraftBoxItem {
        public CraftBoxItem_RareItem(int chance, int count) {
            super(chance, AwardKindEnum.NONE, count, -1);
        }
    }

    public static class CraftBoxItem_Money extends CraftBoxItem {
        public CraftBoxItem_Money(int chance, int count) {
            super(chance, AwardKindEnum.MONEY, count, -1);
        }
    }

    public static class CraftBoxItem_RealMoney extends CraftBoxItem {
        public CraftBoxItem_RealMoney(int chance, int count) {
            super(chance, AwardKindEnum.REAL_MONEY, count, -1);
        }
    }

    public static class CraftBoxItem_Medal extends CraftBoxItem {
        public CraftBoxItem_Medal(int chance, int count) {
            super(chance, AwardKindEnum.REAGENT, count, Reagent.medal.getIndex());
        }
    }

}
