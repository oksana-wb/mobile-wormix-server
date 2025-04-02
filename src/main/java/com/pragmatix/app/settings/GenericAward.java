package com.pragmatix.app.settings;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.model.AnyMoneyAddition;
import com.pragmatix.app.services.ProfileBonusService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.Null;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Бин для указания чего и сколько выдавать игроку за то или иное событие
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.09.12 10:43
 * @see ProfileBonusService#awardProfile(GenericAward, com.pragmatix.app.model.UserProfile, com.pragmatix.app.common.AwardTypeEnum, String)
 */
public class GenericAward extends GenericAwardProducer implements Cloneable, AnyMoneyAddition {

    public static final GenericAward EMPTY = new GenericAward(); 
    
    private String name = "";
    /**
     * добавить денег
     */
    private int money;
    private int moneyFrom;
    private int moneyTo;
    /**
     * учитывать ускоритель при добавлении денег
     */
    private boolean useBooster;
    /**
     * добавить в награды опыт равный 1/4 от полученных фузов или рубинов*100
     * фузы расчитываюся с учетом ускорителей
     * игроки 30-го уровня опыт не получают
     */
    private boolean addExperience;
    /**
     * добавить реалов
     */
    private int realMoney;
    private int realMoneyFrom;
    private int realMoneyTo;
    /**
     * добавить кол-ва боёв
     */
    private int battlesCount;
    /**
     * установить кол-во боёв
     */
    private int exactBattlesCount;
    /**
     * добавить скорости реакции
     */
    private int reactionRate;
    private int reactionRateFrom;
    private int reactionRateTo;
    /**
     * выстрелы к оружию или временная шапка
     */
    private List<AwardBackpackItem> awardItems = new ArrayList<>();
    private String awardItemsStr = "";

    private boolean setItem;
    /**
     * выдавать выстрелы к оружию не дольше указанного количества
     */
    private int maxWeaponShotCount;
    /**
     * вероятности выпадения реагентов
     */
    private int[][] reagentsMass = new int[][]{};
    private String reagentsMassStr = "";
    /**
     * количество выдаваемых реагентов
     */
    private int reagentsCount;

    private int singleReagentCount;
    private int singleReagentCountFrom;
    private int singleReagentCountTo;

    /**
     * если нужно выдать определенные реагенты <Id реагента, Количество>
     */
    private Map<Byte, Integer> reagents = new HashMap<>();

    private Race race;

    private int[] skins = ArrayUtils.EMPTY_INT_ARRAY;

    private int experience;

    // выдаем seasonWeapons ящиков сезонного оружия в количестве count
    // формат 'seasonWeapons:count seasonWeapons:count'
    private String seasonWeapons;

    // альтернативная награда если игрок достиг максимального уровня
    private GenericAward levelMaxAward;

    // ставочный наградной билет
    private int wagerWinAwardToken;

    // боссовый наградной билет
    private int bossWinAwardToken;

    private int rename;

    public int getRename() {
        return rename;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getRealMoney() {
        return realMoney;
    }

    public void setRealMoney(int realMoney) {
        this.realMoney = realMoney;
    }

    public int getBattlesCount() {
        return battlesCount;
    }

    public void setBattlesCount(int battlesCount) {
        this.battlesCount = battlesCount;
    }

    public int getReactionRate() {
        return reactionRate;
    }

    public void setReactionRate(int reactionRate) {
        this.reactionRate = reactionRate;
    }

    public List<AwardBackpackItem> getAwardItems() {
        return awardItems;
    }

    public void setAwardItemsStr(String awardItemsStr) {
        this.awardItemsStr = awardItemsStr;
    }

    public int[][] getReagentsMass() {
        return reagentsMass;
    }

    public String getReagentsMassStr() {
        return reagentsMassStr;
    }

    public void setReagentsMass(int[][] reagentsMass) {
        this.reagentsMass = reagentsMass;
    }

    public void setReagentsMassStr(String reagentsMassStr) {
        this.reagentsMassStr = reagentsMassStr;
    }

    public int getReagentsCount() {
        return reagentsCount;
    }

    public void setReagentsCount(int reagentsCount) {
        this.reagentsCount = reagentsCount;
    }

    public Map<Byte, Integer> getReagents() {
        return reagents;
    }

    public void setReagents(Map<Byte, Integer> reagents) {
        this.reagents = reagents;
    }

    public void setReagentsStr(String reagents) {
        if(StringUtils.isBlank(reagents)) {
            return;
        }
        this.reagents = Arrays.stream(reagents.split(" ")).map(s -> s.split(":")).collect(Collectors.toMap(ss -> Byte.valueOf(ss[0]), ss -> Integer.valueOf(ss[1])));
    }

    public int getMaxWeaponShotCount() {
        return maxWeaponShotCount;
    }

    public void setMaxWeaponShotCount(int maxWeaponShotCount) {
        this.maxWeaponShotCount = maxWeaponShotCount;
    }

    public int getExactBattlesCount() {
        return exactBattlesCount;
    }

    public void setExactBattlesCount(int exactBattlesCount) {
        this.exactBattlesCount = exactBattlesCount;
    }

    public String getAwardItemsStr() {
        return awardItemsStr;
    }

    public boolean isSetItem() {
        return setItem;
    }

    public void setSetItem(boolean setItem) {
        this.setItem = setItem;
    }

    public Race getRace() {
        return race;
    }

    public void setRace(@Null Race race) {
        this.race = race;
    }

    public int getMoneyFrom() {
        return moneyFrom;
    }

    public void setMoneyFrom(int moneyFrom) {
        this.moneyFrom = moneyFrom;
    }

    public int getMoneyTo() {
        return moneyTo;
    }

    public void setMoneyTo(int moneyTo) {
        this.moneyTo = moneyTo;
    }

    public int getRealMoneyFrom() {
        return realMoneyFrom;
    }

    public void setRealMoneyFrom(int realMoneyFrom) {
        this.realMoneyFrom = realMoneyFrom;
    }

    public int getRealMoneyTo() {
        return realMoneyTo;
    }

    public void setRealMoneyTo(int realMoneyTo) {
        this.realMoneyTo = realMoneyTo;
    }

    public int getReactionRateFrom() {
        return reactionRateFrom;
    }

    public void setReactionRateFrom(int reactionRateFrom) {
        this.reactionRateFrom = reactionRateFrom;
    }

    public int getReactionRateTo() {
        return reactionRateTo;
    }

    public void setReactionRateTo(int reactionRateTo) {
        this.reactionRateTo = reactionRateTo;
    }

    public int getSingleReagentCount() {
        return singleReagentCount;
    }

    public void setSingleReagentCount(int singleReagentCount) {
        this.singleReagentCount = singleReagentCount;
    }

    @Override
    public String toString() {
        return "GenericAward(" + (!name.isEmpty() ? name : key) + "){" +
                (money > 0 ? "money=" + money : "") +
                (moneyFrom > 0 ? "money=" + moneyFrom + "-" + moneyTo : "") +
                (realMoney > 0 ? ", realMoney=" + realMoney : "") +
                (realMoneyFrom > 0 ? ", realMoney=" + realMoneyFrom + "-" + realMoneyTo : "") +
                (battlesCount > 0 ? ", battlesCount=" + battlesCount : "") +
                (exactBattlesCount > 0 ? ", exactBattlesCount=" + exactBattlesCount : "") +
                (reactionRate > 0 ? ", reactionRate=" + reactionRate : "") +
                (reactionRateFrom > 0 ? "money=" + reactionRateFrom + "-" + reactionRateTo : "") +
                (!StringUtils.isEmpty(awardItemsStr) ? ", awardItemsStr='" + awardItemsStr + '\'' : "") +
//                ", awardItems=" + awardItems +
                (setItem ? ", setItem=true" : "") +
                (maxWeaponShotCount > 0 ? ", maxWeaponShotCount=" + maxWeaponShotCount : "") +
                (!reagents.isEmpty() ? ", reagents=" + reagents : "") +
                (!StringUtils.isEmpty(reagentsMassStr) ? ", reagentsMassStr='" + reagentsMassStr + '\'' : "") +
//                ", reagentsMass=" + Arrays.toString(reagentsMass) +
                (reagentsCount > 0 ? ", reagentsCount=" + reagentsCount : "") +
                (singleReagentCount > 0 ? ", singleReagentCount=" + singleReagentCount : "") +
                (singleReagentCountFrom > 0 ? ", singleReagentCount=" + singleReagentCountFrom + "-" + singleReagentCountTo : "") +
                (!StringUtils.isEmpty(seasonWeapons) ? ", seasonWeapons='" + seasonWeapons + '\'' : "") +
                (experience > 0 ? ", experience=" + experience : "") +
                (race != null ? ", race=" + race : "") +
                (skins.length > 0 ? ", skins=" + Arrays.toString(skins) : "") +
                (wagerWinAwardToken > 0 ? ", wagerWinAwardToken=" + wagerWinAwardToken : "") +
                (bossWinAwardToken > 0 ? ", bossWinAwardToken=" + bossWinAwardToken : "") +
                '}';
    }

    public GenericAward addWeapon(int weaponId, int weaponCount) {
        AwardBackpackItem awardBackpackItem = new AwardBackpackItem();
        awardBackpackItem.setWeaponId(weaponId);
        awardBackpackItem.setCount(weaponCount);
        awardItems.add(awardBackpackItem);
        return this;
    }

    public GenericAward addStuff(short stuffId) {
        AwardBackpackItem awardBackpackItem = new AwardBackpackItem();
        awardBackpackItem.setStuffId(stuffId);
        awardItems.add(awardBackpackItem);
        return this;
    }

    public GenericAward addStuffForHours(short stuffId, int expireHours) {
        AwardBackpackItem awardBackpackItem = new AwardBackpackItem();
        awardBackpackItem.setStuffId(stuffId);
        awardBackpackItem.setExpireHours(expireHours);
        awardItems.add(awardBackpackItem);
        return this;
    }

    public GenericAward addSeasonStuff(short stuffId) {
        AwardBackpackItem awardBackpackItem = new AwardBackpackItem();
        awardBackpackItem.setStuffId(stuffId);
        awardItems.add(awardBackpackItem);
        return this;
    }

    public GenericAward addStuffUntilTime(short stuffId, int expireTimeInSeconds) {
        AwardBackpackItem awardBackpackItem = new AwardBackpackItem();
        awardBackpackItem.setStuffId(stuffId);
        awardBackpackItem.setExpireTimeInSeconds(expireTimeInSeconds);
        awardItems.add(awardBackpackItem);
        return this;
    }

    public GenericAward addBattles(int battles) {
        battlesCount += battles;
        return this;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public GenericAward getGenericAward() {
        return this;
    }

    public static class Builder {
        private final GenericAward award = new GenericAward();

        public GenericAward build() {
            return award;
        }

        public Builder addRename(int rename) {
            award.rename += rename;
            return this;
        }

        public Builder addMoney(int money) {
            award.money += money;
            return this;
        }

        public Builder addRealMoney(int realMoney) {
            award.realMoney += realMoney;
            return this;
        }

        public Builder addBattlesCount(int battlesCount) {
            award.battlesCount += battlesCount;
            return this;
        }

        public Builder addReactionRate(int reactionRate) {
            award.reactionRate += reactionRate;
            return this;
        }

        public Builder addWeapon(int weaponId, int weaponCount) {
            award.addWeapon(weaponId, weaponCount);
            return this;
        }

        public Builder addSeasonStuff(int stuffId) {
            award.addSeasonStuff((short)stuffId);
            return this;
        }

        public Builder addReagent(int reagentId, int count) {
            return addReagent((byte) reagentId, count);
        }

        public Builder addReagent(byte reagentId, int count) {
            if(award.reagents.containsKey(reagentId)) {
                award.reagents.put(reagentId, award.reagents.get(reagentId) + count);
            } else {
                award.reagents.put(reagentId, count);
            }
            return this;
        }

        public Builder setRace(Race race) {
            award.race = race;
            return this;
        }

        public Builder addExperience(int exp) {
            award.experience += exp;
            return this;
        }

        public Builder useBooster() {
            award.useBooster = true;
            return this;
        }

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSingleReagentCountFrom() {
        return singleReagentCountFrom;
    }

    public void setSingleReagentCountFrom(int singleReagentCountFrom) {
        this.singleReagentCountFrom = singleReagentCountFrom;
    }

    public int getSingleReagentCountTo() {
        return singleReagentCountTo;
    }

    public void setSingleReagentCountTo(int singleReagentCountTo) {
        this.singleReagentCountTo = singleReagentCountTo;
    }

    public boolean isUseBooster() {
        return useBooster || addExperience;
    }

    public void setUseBooster(boolean useBooster) {
        this.useBooster = useBooster;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public boolean isAddExperience() {
        return addExperience;
    }

    public void setAddExperience(boolean addExperience) {
        this.addExperience = addExperience;
    }

    public String getSeasonWeapons() {
        return seasonWeapons;
    }

    public int[] getSkins() {
        return skins;
    }

    public void setSkins(List<String> skins) {
        this.skins = skins.stream().mapToInt(Integer::parseInt).toArray();
    }

    //8:4
    //6:4 2:3
    private static final Pattern seasonWeaponsPattern = Pattern.compile("(\\d+\\:\\d+)( \\d+\\:\\d+)*");

    public void setSeasonWeapons(String seasonWeapons) {
        if(!seasonWeapons.isEmpty() && !seasonWeaponsPattern.matcher(seasonWeapons).matches())
            throw new IllegalStateException("GenericAward: Не корректный формат для награждения сезонным оружием! [" + seasonWeapons + "]");
        this.seasonWeapons = seasonWeapons;
    }

    public GenericAward getLevelMaxAward() {
        return levelMaxAward;
    }

    public void setLevelMaxAward(GenericAward levelMaxAward) {
        this.levelMaxAward = levelMaxAward;
    }

    public int getWagerWinAwardToken() {
        return wagerWinAwardToken;
    }

    public void setWagerWinAwardToken(int wagerWinAwardToken) {
        this.wagerWinAwardToken = wagerWinAwardToken;
    }

    public int getBossWinAwardToken() {
        return bossWinAwardToken;
    }

    public void setBossWinAwardToken(int bossWinAwardToken) {
        this.bossWinAwardToken = bossWinAwardToken;
    }

    @Override
    public final GenericAward clone() {
        try {
            GenericAward result = (GenericAward) super.clone();
            result.awardItems = awardItems.stream().map(AwardBackpackItem::clone).collect(Collectors.toList());
            result.reagentsMass = new int[reagentsMass.length][];
            for(int i = 0; i < result.reagentsMass.length; i++) {
                result.reagentsMass[i] = ArrayUtils.clone(reagentsMass[i]);
            }
            result.reagents = new HashMap<>(reagents);
            return result;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
