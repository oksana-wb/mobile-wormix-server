package com.pragmatix.craft.services;

import com.pragmatix.achieve.messages.server.IncreaseAchievementsResult;
import com.pragmatix.app.common.*;
import com.pragmatix.app.init.LevelCreator;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.services.events.*;
import com.pragmatix.app.services.rating.SeasonService;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.chat.ChatAction;
import com.pragmatix.chat.GlobalChatService;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.services.ClanService;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.craft.dao.ReagentsDao;
import com.pragmatix.craft.domain.Reagent;
import com.pragmatix.craft.domain.ReagentsEntity;
import com.pragmatix.craft.messages.OpenChestResult;
import com.pragmatix.craft.model.CraftItemResult;
import com.pragmatix.craft.model.CraftResultItem;
import com.pragmatix.craft.model.Recipe;
import com.pragmatix.craft.model.StuffRecipe;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.quest.QuestService;
import com.pragmatix.quest.dao.QuestEntity;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.pragmatix.app.services.ProfileEventsService.Param.reagents;
import static com.pragmatix.craft.services.CraftService.ParamsEnum.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 28.06.12 17:00
 */
@Service
public class CraftService {

    public static final int MONEY_FOR_ONE_RUBY = 100;

    public static final int MAX_REAGENTS_FOR_BATTLE = 3;

    public int medalsCountInsteadOfExistsRareItem = 3;
    public int rubyCountInsteadOfExistsRareItem = 25;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Set<Short> startingRecipes = new HashSet<>();

    private Map<Short, Recipe> recipes;

    private Map<Short, StuffRecipe> stuffRecipes;

    private Map<Reagent, Integer> reagentsPrice;

    // вероятности реагентов по уровням за "простой" бой
    private NavigableMap<Integer, int[][]> simpleBattleReagentsChanceMap = new TreeMap<>();

    // вероятности реагентов по id боссов
    private Map<Integer, int[][]> bossBattleReagentsChanceMap = new HashMap<>();

    // вероятности реагентов по id героик боссов
    private Map<Integer, byte[]> heroicBattleReagentsChanceMap = new HashMap<>();

    // вероятности реагентов по уровням за обыск друга
    private Map<Integer, int[][]> friendSearchReagentsMassMap = new HashMap<>();

    // вероятности реагентов по ставке и потом по уровням
    private Map<Integer, NavigableMap<Integer, int[][]>> pvpBattleReagentsChanceMap = new HashMap<>();

    @Resource
    private ReagentsDao reagentsDao;

    @Resource
    private ProfileService profileService;

    @Resource
    private StatisticService statisticService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private StuffService stuffService;

    @Resource
    private WeaponService weaponService;

    @Resource
    private QuestService questService;

    @Resource
    private GlobalChatService chatService;

    @Resource
    private ClanService clanService;

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private ProfileExperienceService profileExperienceService;

    @Resource
    private BattleService battleService;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private LevelCreator levelCreator;

    @Resource
    Optional<SeasonService> seasonService;

    @Value("#{craftResultItemsMap}")
    private Map<Short, Map<Integer, CraftResultItem>> craftResultItemsMap = new HashMap<>();

    @Value("#{craftServiceParams}")
    private Map<ParamsEnum, Integer> params;

    @Value("#{craftServiceUnluckyThreshold}")
    private Map<Integer, Map<Integer, Integer>> unluckyThresholds;

    public static final int DOWNGRADE_WEAPON_PRICE_IN_RUBY = 5;

    @Value("${CraftService.debugMode:false}")
    private boolean debugMode = false;

    public enum ParamsEnum {
        OpenChestWeaponLimit,
        OpenChestInsteadWeaponMoneyFrom,
        OpenChestInsteadWeaponMoneyTo,
    }

    public void init() {
        // составляем набор рецептов доступных на старте
        for(Recipe recipe : recipes.values()) {
            for(Byte reagentId : recipe.getReagentsMap().keySet()) {
                if(Reagent.valueOf(reagentId) == null) {
                    throw new IllegalArgumentException("Reagent not found by id [" + reagentId + "]! recipeId = [" + recipe.getId() + "]");
                }
            }
            if(recipe.isStarting()) {
                startingRecipes.add(recipe.getId());
            }
            if(recipe.getBaseRecipeId() > 0) {
                Recipe baseRecipe = recipes.get(recipe.getBaseRecipeId());
                if(baseRecipe == null)
                    throw new IllegalArgumentException("base Recipe not found by id [" + recipe.getBaseRecipeId() + "]! recipeId = [" + recipe.getId() + "]");
                if(recipe.getWeaponId() != baseRecipe.getWeaponId())
                    throw new IllegalArgumentException(String.format("recipe [%s]: base recipe [%s] have different weapon id [%s]!", recipe.getId(), baseRecipe.getId(), baseRecipe.getWeaponId()));
                recipe.setBaseRecipe(baseRecipe);
            }
        }

        // инициализируем рецепты предметов
        for(StuffRecipe stuffRecipe : stuffRecipes.values()) {
            stuffRecipe.setResultStuffMassMap(parseResultStuffsMassString(stuffRecipe.getResultStuffMass(), craftResultItemsMap.get(stuffRecipe.getId())));
            // заполняем набор id итоговых предметов
            String[] reagentsMass = stuffRecipe.getResultStuffMass().split(" ");
            for(String reagentsMas : reagentsMass) {
                stuffRecipe.getResultsStuffsSet().add(Short.valueOf(reagentsMas.split(":")[0]));
            }
        }
    }

    public void applyRecipe(UserProfile profile, short recipeId) {
        recipeId = validateRecipeId(recipeId);

        short[] recipes = profile.getRecipes();
        for(short recipe : recipes) {
            if(recipe == recipeId) {
                throw new IllegalArgumentException("apply recipe failure because recipe [" + recipeId + "] already applied! recipes: " + Arrays.toString(recipes));
            }
        }

        profile.setRecipes(ArrayUtils.add(recipes, recipeId));
        profile.setDirty(true);
    }

    public void rollbackRecipe(UserProfile profile, short recipeId, boolean validate) {
        if(validate) {
            recipeId = validateRecipeId(recipeId);
        }

        short[] recipes = profile.getRecipes();
        for(int i = 0; i < recipes.length; i++) {
            short recipe = recipes[i];
            if(recipe == recipeId) {
                profile.setRecipes(ArrayUtils.remove(recipes, i));
                profile.setDirty(true);
                return;
            }
        }
        if(validate) {
            throw new IllegalArgumentException("rollback recipe failure because recipe [" + recipeId + "] not applied! recipes:" + Arrays.toString(recipes));
        }
    }

    private short validateRecipeId(short recipeId) {
        recipeId = (short) Math.abs(recipeId);
        if(!startingRecipes.contains(recipeId)) {
            throw new IllegalArgumentException("apply recipe failure because recipe not found by id [" + recipeId + "]");
        }
        return recipeId;
    }

    // "откатить" все рецепты для данного оружия
    public void rollbackRecipesForWeapon(UserProfile profile, int weaponId) {
        for(Recipe recipe : recipes.values()) {
            if(recipe.getWeaponId() == weaponId) {
                rollbackRecipe(profile, recipe.getId(), false);
            }
        }
    }

    public short getRecipeFor(UserProfile profile, short recipeId) {
        recipeId = (short) Math.abs(recipeId);

        for(short recipe : profile.getRecipes()) {
            if(recipeId == Math.abs(recipe)) {
                return recipe;
            }
        }
        return 0;
    }

    public boolean recipeIsPresentAndNotApplied(UserProfile profile, short recipeId) {
        return startingRecipes.contains(recipeId) && getRecipeFor(profile, recipeId) == 0;
    }

    public boolean recipeIsPresentAndApplied(UserProfile profile, short recipeId) {
        return getRecipeFor(profile, recipeId) > 0;
    }

    // нет примененного рецепта для которого данный рецепт являлся бы базовым
    public boolean theresNotMajorRecipeApplied(UserProfile profile, short recipeId) {
        for(Recipe recipe : recipes.values()) {
            // есть примененный рецепт для которого данный рецепт является базовым
            if(recipe.getBaseRecipeId() == recipeId && recipeIsPresentAndApplied(profile, recipe.getId())) {
                return false;
            }
        }
        return true;
    }

    public ReagentsEntity getReagentsForProfile(long profileId) {
        UserProfile userProfile = profileService.getUserProfile(profileId);
        if(userProfile != null) {
            if(userProfile.getReagents() == null) {
                // Ленивая инициализация
                synchronized (userProfile) {
                    if(userProfile.getReagents() == null) {
                        ReagentsEntity reagentsEntity = reagentsDao.select(userProfile.getId());
                        if(reagentsEntity == null) {
                            reagentsEntity = new ReagentsEntity(profileId);
                        }
                        userProfile.setReagents(reagentsEntity);
                    }
                    return userProfile.getReagents();
                }
            } else {
                return userProfile.getReagents();
            }
        }
        return null;
    }

    public void incrementReagents(UserProfile profile, byte[] collectedReagents) {
        for(byte reagentId : collectedReagents) {
            if(reagentId >= 0) {
                incrementReagent(reagentId, profile.getId());
            }
        }
    }

    public void incrementReagent(byte reagentId, Long profileId) {
        addReagent(reagentId, 1, profileId);
    }

    public void addReagent(byte reagentId, int count, Long profileId) {
        try {
            ReagentsEntity reagentsForProfile = getReagentsForProfile(profileId);
            if(reagentsForProfile != null) {
                reagentsForProfile.addReagentValue(reagentId, count);
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    public void setReagentValue(UserProfile profile, byte reagentId, int value) {
        try {
            ReagentsEntity reagentsForProfile = getReagentsForProfile(profile.getId());
            if(reagentsForProfile != null) {
                reagentsForProfile.setReagentValue(reagentId, value);
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    /**
     * @return массив из 3-х максимум реагентов для боя с ботами согласно заложенной вероятности для каждого из реагентов
     */
    public byte[] getReagentsForSimpleBattle(int level, int extraReagentCount) {
        // реагенты доступны с 4-го уровня, в обучающих мисиях реагенты не предусмотрены
        int[][] reagentsMass = simpleBattleReagentsChanceMap.containsKey(level) ?
                simpleBattleReagentsChanceMap.get(level) :
                simpleBattleReagentsChanceMap.get(simpleBattleReagentsChanceMap.lastKey());
        return getReagentsForBattle(reagentsMass, extraReagentCount);
    }

    /**
     * @return реагент за обыск согласно заложенной вероятности для каждого из реагентов
     */
    public byte getSingleReagentForFriendSearch(int level) {
        return getSingleReagentForBattle(level, friendSearchReagentsMassMap);
    }

    /**
     * @return массив из 3-х максимум реагентов для боя с боссом согласно заложенной вероятности для каждого из реагентов
     */
    public byte[] getReagentsForBossBattle(int missionId, int extraReagentCount) {
        return getReagentsForBattle(bossBattleReagentsChanceMap.get(missionId), extraReagentCount);
    }

    /**
     * @return массив из 3-х максимум реагентов для боя с боссом согласно заложенной вероятности для каждого из реагентов
     * совместные боссы
     */
    public byte[] getReagentsForBossBattle(short[] missionIds, int heroicBossLevel) {
        if(missionIds.length == 1) {
            return getReagentsForBossBattle(missionIds[0], 0);
        } else {
            return heroicBattleReagentsChanceMap.get(heroicBossLevel);
        }
    }

    public byte[] getReagentsForPvpBattle(BattleWager wager, int levelKey) {
        if(wager.questId > 0 || wager == BattleWager.WAGER_20_DUEL)
            return new byte[0];

        int wagerValue = wager.getValue();
        NavigableMap<Integer, int[][]> chanceMap = pvpBattleReagentsChanceMap.get(wagerValue);
        if(chanceMap != null) {
            int[][] reagentsMass = chanceMap.containsKey(levelKey) ?
                    chanceMap.get(levelKey) :
                    chanceMap.get(chanceMap.lastKey());
            return getReagentsForBattle(reagentsMass, 0);
        } else {
            log.error("no reagents's chance map by wager [{}]", wager);
            return new byte[0];
        }
    }

    private byte[] getReagentsForBattle(int[][] reagentsMass, int extraReagentCount) {
        int length = MAX_REAGENTS_FOR_BATTLE + extraReagentCount;
        byte[] result = new byte[length + (AppParams.IS_NOT_MOBILE() ? 1 : 0)];
        Arrays.fill(result, (byte) -1);

        if(reagentsMass == null) {
            return result;
        }
        for(int i = 0; i < length; i++) {
            result[i] = (byte) rollDice(reagentsMass);
        }
        if(AppParams.IS_NOT_MOBILE()) {
            // добавляем новый ресурс: мутаген (id 52)
            // - в обычных миссиях к списку реагентов за бой всегда добавляется 1 единица мутагена
            // - аналогично за боссов: 1 единица добавляется в список наград
            result[length] = Reagent.mutagen.getIndex();
        }
        return result;
    }

    private byte getSingleReagentForBattle(int key, Map<Integer, int[][]> reagentsChanceMap) {
        byte result = -1;

        int[][] reagentsMass = reagentsChanceMap.get(key);
        if(reagentsMass == null) {
            return result;
        }
        return (byte) rollDice(reagentsMass);
    }

    public int rollDice(int[][] itemsMass, int excludedItem) {
        if(debugMode) {
            return excludedItem + 1;
        }
        Random rnd = new Random();
        int item = -1;
        int sumMass = 0;
        // суммируем массу за иселючение массы excludedItem
        for(int i = 1; i < itemsMass.length; i++) {
            sumMass += itemsMass[i][0] == excludedItem ? 0 : itemsMass[i][1];
        }
        int r = rnd.nextInt(sumMass);
        for(int i = 1; i < itemsMass.length; i++) {
            if(itemsMass[i][0] == excludedItem) continue;
            r -= itemsMass[i][1];
            if(r < 0) {
                return itemsMass[i][0];
            }
        }
        return item;
    }

    public static int rollDice(int[][] itemsMass) {
        return CraftService.rollDice(itemsMass, 0L);
    }

    public static int rollDice(int[][] itemsMass, long seed) {
        // [0][1] - сумма масс; [i(>0)][0] - id элемента; [i(>0)][1] - масса элемента
        Random rnd = seed != 0 ? new Random(seed) : new Random();
        int item = -1;
        int r = rnd.nextInt(itemsMass[0][1]);
        for(int i = 1; i < itemsMass.length; i++) {
            r -= itemsMass[i][1];
            if(r < 0) {
                return itemsMass[i][0];
            }
        }
        return item;
    }

    public static int[][] parseReagentsMassString(String reagentsMassString) {
        if(reagentsMassString == null || reagentsMassString.trim().isEmpty()) {
            return new int[][]{};
        }
        String[] reagentsMass = reagentsMassString.trim().split("[ ]{1,3}");
        // первый элемент - процент "молока" (нет ничего)
        float milkPercent = (float) Integer.parseInt(reagentsMass[0]) / (float) 100;
        int allReagentsMass = 0;
        int[][] reagentsMassArray = new int[reagentsMass.length + 1][2];
        for(int i = 1; i < reagentsMass.length; i++) {
            Reagent reagent = Reagent.valueOf(Integer.parseInt(reagentsMass[i].split(":")[0]));
            int reagentMass = Integer.parseInt(reagentsMass[i].split(":")[1]);
            allReagentsMass += reagentMass;
            reagentsMassArray[i + 1][0] = reagent.getIndex();
            reagentsMassArray[i + 1][1] = reagentMass;
        }
        reagentsMassArray[1][0] = -1;// нет ничего
        reagentsMassArray[1][1] = Math.round(((float) allReagentsMass * milkPercent) / (1f - milkPercent));

        // по нулевому индексу сумму всех весов
        reagentsMassArray[0][1] = reagentsMassArray[1][1] + allReagentsMass;
        return reagentsMassArray;
    }

    private Set<Short> allBaseStuffsSet;

    public int[][] parseAwardMassString(String massString) {
        if(massString == null || massString.trim().isEmpty()) {
            return new int[][]{};
        }
        if(allBaseStuffsSet == null) {
            allBaseStuffsSet = stuffRecipes.values().stream().flatMap(r -> r.getBaseStuffsSet().stream()).collect(Collectors.toSet());
        }
        String[] reagentsMass = massString.trim().split("[ ]{1,3}");
        // первый элемент - процент "молока" (нет ничего)
        float milkPercent = (float) Integer.parseInt(reagentsMass[0]) / (float) 100;
        int allReagentsMass = 0;
        int[][] reagentsMassArray = new int[reagentsMass.length + 1][2];
        for(int i = 1; i < reagentsMass.length; i++) {
            Integer itemId = Integer.valueOf(reagentsMass[i].split(":")[0]);
            Stuff stuff = stuffService.getStuff(itemId.shortValue());
            if(stuff == null) {
                throw new IllegalArgumentException("не найден предмет! " + itemId + " в строке конфигурации " + massString);
            }
            if(!stuff.isCraftBase()) {
                throw new IllegalArgumentException("редкий предмет [" + stuff + "] не является основой для крафта! Отсутствует признак craftBase");
            }
            if(!allBaseStuffsSet.contains(stuff.getStuffId())) {
                throw new IllegalArgumentException("редкий предмет [" + stuff + "] не является основой для крафта! Отсутствует рецепт");
            }
            int reagentMass = Integer.parseInt(reagentsMass[i].split(":")[1]);
            allReagentsMass += reagentMass;
            reagentsMassArray[i + 1][0] = itemId;
            reagentsMassArray[i + 1][1] = reagentMass;
        }
        reagentsMassArray[1][0] = -1;// нет ничего
        reagentsMassArray[1][1] = Math.round(((float) allReagentsMass * milkPercent) / (1f - milkPercent));

        // по нулевому индексу сумму всех весов
        reagentsMassArray[0][1] = reagentsMassArray[1][1] + allReagentsMass;
        return reagentsMassArray;
    }

    public int[][] parseResultStuffsMassString(String massString, Map<Integer, CraftResultItem> craftResultItemMapForRecipe) {
        String[] reagentsMass = massString.split(" ");
        int allReagentsMass = 0;
        int[][] reagentsMassArray = new int[reagentsMass.length + 1][2];
        for(int i = 0; i < reagentsMass.length; i++) {
            String s = reagentsMass[i].split(":")[0];
            Integer itemId = Integer.valueOf(s);
            if(craftResultItemMapForRecipe != null) {
                CraftResultItem craftResultItem = craftResultItemMapForRecipe.get(itemId);
//                if(craftResultItem.stuffId > 0) {
//                    Stuff stuff = stuffService.getStuff(craftResultItem.stuffId);
//                    if(stuff == null) {
//                        throw new IllegalArgumentException("не найден предмет рузультат крафта! " + itemId);
//                    }
//                }
            } else {
                Stuff stuff = stuffService.getStuff(itemId.shortValue());
                if(stuff == null) {
                    throw new IllegalArgumentException("не найден предмет рузультат крафта! " + itemId);
                }
            }
            int reagentMass = Integer.parseInt(reagentsMass[i].split(":")[1]);
            allReagentsMass += reagentMass;
            reagentsMassArray[i + 1][0] = itemId;
            reagentsMassArray[i + 1][1] = reagentMass;
        }
        // по нулевому индексу сумму всех весов
        reagentsMassArray[0][1] = allReagentsMass;
        return reagentsMassArray;
    }

    public ShopResultEnum upgradeWeapon(UserProfile profile, short recipeId) {
        if(recipeIsPresentAndNotApplied(profile, recipeId)) {
            Recipe recipe = recipes.get(recipeId);
            if(recipe != null) {
                // проверяем доступен ли апгрейд по уровню
                if(profile.getLevel() < recipe.needLevel()) {
                    log.error("error apply recipe [{}]: profile's level [{}] too small", recipeId, profile.getLevel());
                    return ShopResultEnum.MIN_REQUIREMENTS_ERROR;
                }
                // проверяем наличие базового (бесконечного) оружия в рюкзаке
                BackpackItem backpackItem = profile.getBackpackItemByWeaponId(recipe.getWeaponId());
                if(!weaponService.isPresentInfinitely(backpackItem)) {
                    log.error("error apply recipe [{}]: base weapon [{}] absent in backpack or not infinite(max level) [{}]", recipeId, recipe.getWeaponId(), backpackItem);
                    // не применен родительский рецепт
                    return ShopResultEnum.MIN_REQUIREMENTS_ERROR;
                }

                // проверяем что применены все базовые рецепты (если они есть)
                Recipe baseRecipe = recipe.getBaseRecipe();
                while (baseRecipe != null) {
                    if(!recipeIsPresentAndApplied(profile, baseRecipe.getId())) {
                        log.error("error apply recipe [{}]: does not apply the parent recipe [{}]", recipeId, baseRecipe.getId());
                        // не применен родительский рецепт
                        return ShopResultEnum.MIN_REQUIREMENTS_ERROR;
                    }
                    baseRecipe = baseRecipe.getBaseRecipe();
                }

                // проверяем что уже не применен рецепт одного уровня с этим
                baseRecipe = recipe.getBaseRecipe();
                if(baseRecipe != null) {
                    for(Recipe rec : recipes.values()) {
                        if(!rec.equals(recipe) && rec.getBaseRecipeId() == baseRecipe.getId() && recipeIsPresentAndApplied(profile, rec.getId())) {
                            log.error("error apply recipe [{}]: exist applied recipe for the same level! recipes:{}", recipeId, Arrays.toString(profile.getRecipes()));
                            // уже применен рецепт одного уровня с этим
                            return ShopResultEnum.ERROR;
                        }
                    }
                } else {
                    for(Recipe rec : recipes.values()) {
                        if(!rec.equals(recipe) && rec.getBaseRecipe() == null && rec.getWeaponId() == recipe.getWeaponId() && recipeIsPresentAndApplied(profile, rec.getId())) {
                            log.error("error apply recipe [{}]: exist applied recipe for the zero level! recipes:{}", recipeId, Arrays.toString(profile.getRecipes()));
                            // уже применен рецепт одного уровня с этим
                            return ShopResultEnum.ERROR;
                        }
                    }
                }

                int[] profilesReagents = getReagentsForProfile(profile.getId()).getValues();
                Map<Byte, Integer> recipeReagentsMap = recipe.getReagentsMap();

                // считаем деньги если не хватает ресурсов
                int needRuby = realMoneyForAbsentReagents(profilesReagents, recipeReagentsMap);

                if(profile.getRealMoney() < needRuby) {
                    log.error("error apply recipe [{}]: no enougth ruby [{}]", recipeId, needRuby - profile.getRealMoney());
                    return ShopResultEnum.NOT_ENOUGH_MONEY;
                }

                try {
                    int[] removedReagents = new int[profilesReagents.length];
                    // минусуем реагенты рецепта
                    for(Byte reagentId : recipeReagentsMap.keySet()) {
                        int newValue = Math.max(0, profilesReagents[reagentId] - recipeReagentsMap.get(reagentId));
                        removedReagents[reagentId] = -(profilesReagents[reagentId] - newValue);
                        profilesReagents[reagentId] = newValue;
                    }
                    profile.getReagents().setDirty(true);

                    // применяем рецепт
                    applyRecipe(profile, recipeId);

                    if(needRuby > 0) {
                        // списываем реалы за покупку недостающих реагентов
                        profile.setRealMoney(profile.getRealMoney() - needRuby);
                    }
                    //всегда логируем покупку апгрейда
                    profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PURCHASE, profile,
                            Param.eventType, ItemType.UPGRADE_WEAPON,
                            Param.realMoney, -needRuby,
                            reagents, ReagentsEntity.getReagentValues(removedReagents),
                            Param.recipeId, recipeId,
                            Param.profile_recipes, profile.getRecipes(),
                            Param.profile_reagents, profile.getReagents().getReagentValues()
                    );
                    statisticService.buyItemStatistic(profile.getId(), MoneyType.REAL_MONEY.getType(), 1, ItemType.UPGRADE_WEAPON, needRuby, recipeId, profile.getLevel());

                    return ShopResultEnum.SUCCESS;

                } catch (Exception e) {
                    log.error("error apply recipe [" + recipeId + "]: " + e.toString(), e);
                }
            } else {
                log.error("recipe not found by id [{}]", recipeId);
            }
        } else {
            log.error("error apply recipe [{}]: recipe not present or alredy applied. recipeStatus is [{}]", recipeId, getRecipeFor(profile, recipeId));
        }
        return ShopResultEnum.ERROR;
    }

    public int realMoneyForAbsentReagents(int[] profilesReagents, Map<Byte, Integer> recipeReagentsMap) {
        int needMoney = 0;
        for(Byte reagentId : recipeReagentsMap.keySet()) {
            int needCount = recipeReagentsMap.get(reagentId) - profilesReagents[reagentId];
            if(needCount > 0) {
                Reagent reagent = Reagent.valueOf(reagentId);
                Integer reagentPrice = reagentsPrice.get(reagent);
                if(reagentPrice == null)
                    throw new IllegalArgumentException("Для реагента " + reagent + " не задана цена!");
                needMoney += reagentPrice * needCount;
            }
        }

        int needRuby = 0;
        if(needMoney > 0) {
            // переводим деньги в реалы
            needRuby = (int) Math.ceil((float) needMoney / (float) MONEY_FOR_ONE_RUBY);
        }
        return needRuby;
    }

    public int realMoneyForReagents(Map<Byte, Integer> recipeReagentsMap) {
        int needMoney = 0;
        for(Byte reagentId : recipeReagentsMap.keySet()) {
            int needCount = recipeReagentsMap.get(reagentId);
            if(needCount > 0) {
                Reagent reagent = Reagent.valueOf(reagentId);
                Integer reagentPrice = reagentsPrice.get(reagent);
                needMoney += reagentPrice * needCount;
            }
        }
        int needRuby = 0;
        if(needMoney > 0) {
            // переводим деньги в реалы
            needRuby = (int) Math.ceil((float) needMoney / (float) MONEY_FOR_ONE_RUBY);
        }
        return needRuby;
    }

    public int[] withdrawReagents(UserProfile profile, Map<Reagent, Integer> needReagents) {
        int[] profilesReagents = getReagentsForProfile(profile.getId()).getValues();
        int[] removedReagents = new int[profilesReagents.length];
        // минусуем реагенты
        for(Map.Entry<Reagent, Integer> entry : needReagents.entrySet()) {
            byte reagentId = entry.getKey().getIndex();
            int newValue = Math.max(0, profilesReagents[reagentId] - entry.getValue());
            removedReagents[reagentId] = -(profilesReagents[reagentId] - newValue);
            profilesReagents[reagentId] = newValue;
        }
        profile.getReagents().setDirty(true);

        return removedReagents;
    }

    public boolean isReagentsEnough(UserProfile profile, Map<Reagent, Integer> needReagents) {
        int[] profilesReagents = getReagentsForProfile(profile.getId()).getValues();
        for(Map.Entry<Reagent, Integer> entry : needReagents.entrySet()) {
            byte reagentId = entry.getKey().getIndex();
            if(profilesReagents[reagentId] < entry.getValue())
                return false;
        }
        return true;
    }

    public boolean isReagentsEnough(int[] profilesReagents, Map<Byte, Integer> needReagents) {
        for(Map.Entry<Byte, Integer> entry : needReagents.entrySet()) {
            byte reagentId = entry.getKey();
            if(profilesReagents[reagentId] < entry.getValue())
                return false;
        }
        return true;
    }

    public ShopResultEnum downgradeWeapon(UserProfile profile, short recipeId) {
        if(recipeIsPresentAndApplied(profile, recipeId)) {
            Recipe recipe = recipes.get(recipeId);
            if(recipe != null) {
                if(theresNotMajorRecipeApplied(profile, recipeId)) {
                    int needRuby = AppParams.IS_MOBILE() ? DOWNGRADE_WEAPON_PRICE_IN_RUBY : 0;
                    if(profile.getRealMoney() < needRuby) {
                        log.error("error downgrade weapon by recipeId [{}]: no enougth ruby [{}]", recipeId, needRuby - profile.getRealMoney());
                        return ShopResultEnum.NOT_ENOUGH_MONEY;
                    }
                    try {
                        // возвращаем реагенты рецепта. Не все, только 80%
                        Map<Byte, Integer> recipeReagentsMap = recipe.getReagentsMap();
                        int[] profilesReagents = getReagentsForProfile(profile.getId()).getValues();
                        int[] restoredReagents = new int[profilesReagents.length];
                        for(Byte reagentId : recipeReagentsMap.keySet()) {
                            int value = (int) ((float) recipeReagentsMap.get(reagentId) * 0.8f);
                            profilesReagents[reagentId] = profilesReagents[reagentId] + value;
                            restoredReagents[reagentId] = value;
                        }
                        profile.getReagents().setDirty(true);

                        // откатываем рецепт
                        rollbackRecipe(profile, recipeId, true);

                        // списываем реалы за разборку оружия
                        profile.setRealMoney(profile.getRealMoney() - needRuby);
                        //логируем покупку разборки оружия
                        profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PURCHASE, profile,
                                Param.eventType, ItemType.DOWNGRADE_WEAPON,
                                Param.profile_reagents, profile.getReagents().getReagentValues(),
                                Param.profile_recipes, profile.getRecipes(),
                                Param.realMoney, -needRuby,
                                reagents, ReagentsEntity.getReagentValues(restoredReagents),
                                Param.recipeId, recipeId
                        );
                        if(needRuby > 0)
                            statisticService.buyItemStatistic(profile.getId(), MoneyType.REAL_MONEY.getType(), 1, ItemType.DOWNGRADE_WEAPON, needRuby, recipeId, profile.getLevel());

                        return ShopResultEnum.SUCCESS;
                    } catch (Exception e) {
                        log.error("error downgrade weapon by recipeId [" + recipeId + "]: " + e.toString(), e);
                    }
                } else {
                    log.error("downgrade failure fo recipeId [{}] there's major recipe applied {}", recipeId, Arrays.toString(profile.getRecipes()));
                }
            } else {
                log.error("recipe not found by id [{}]", recipeId);
            }
        }
        return ShopResultEnum.ERROR;
    }

    public void wipeReagents(UserProfile profile) {
        getReagentsForProfile(profile.getId());
        profile.getReagents().clean();
    }

    public CraftItemResult craftItem(@NotNull UserProfile profile, short recipeId, @Null MoneyType moneyType) {
        StuffRecipe recipe = stuffRecipes.get(recipeId);
        if(recipe == null) {
            log.error("stuff recipe not found by id [{}]", recipeId);
            return new CraftItemResult(ShopResultEnum.ERROR);
        }

        boolean recraft = false;
        short prevAssembledStuffId = -1;
        // возможно он уже прокачивал этот предмет ранее
        for(Short resStuffId : recipe.getResultsStuffsSet()) {
            if(stuffService.isExist(profile, resStuffId)) {
                recraft = true;
                prevAssembledStuffId = resStuffId;
                break;
            }
        }

        // проверяем, что ещё есть куда улучшать
        if(recraft && stuffService.getStuff((short) (prevAssembledStuffId + 1), false) == null) {
            log.error("stuff recipe [{}]: profile have max stuff [{}] in recipe", recipeId, prevAssembledStuffId);
            return new CraftItemResult(ShopResultEnum.ERROR);
        }

        // проверяем пройден ли босс
        if(Math.max(profile.getMissionId(), 0) < recipe.getMissionId()) {
            log.error("error apply stuff recipe [{}]: required mission failed [{}]", recipeId, profile.getMissionId());
            return new CraftItemResult(ShopResultEnum.MIN_REQUIREMENTS_ERROR);
        }
        // проверяем доступен ли апгрейд по уровню
        if(profile.getLevel() < recipe.needLevel()) {
            log.error("error apply stuff recipe [{}]: profile's level [{}] too small", recipeId, profile.getLevel());
            return new CraftItemResult(ShopResultEnum.MIN_REQUIREMENTS_ERROR);
        }

        // проверяем наличие базовых предметов
        Set<Short> baseStuffsSet = recraft ? recipe.getRecraftBaseStuffsSet() : recipe.getBaseStuffsSet();
        Set<Short> stuffsToRemove = new TreeSet<>();
        int maxResultItemLevel = recipe.getResultsStuffsSet().stream().max(Short::compareTo).orElse((short) 0) % 10;
        for(Short baseStuffId : baseStuffsSet) {
            if(ItemCheck.isCraftStuff(baseStuffId)) {
                int craftLevel = baseStuffId % 10;
                short stuffToRemove = 0;
                for(int i = craftLevel; i <= maxResultItemLevel; i++) {
                    short craftStuffId = (short) (baseStuffId / 10 * 10 + i);
                    if(stuffService.isExist(profile, craftStuffId, false)) {
                        stuffToRemove = craftStuffId;
                        break;
                    }
                }
                if(stuffToRemove == 0) {
                    log.error("error apply stuff recipe [{}]: no one crafted stuff >= [{}]", recipeId, baseStuffId);
                    // отсутствует необходимый предмет
                    return new CraftItemResult(ShopResultEnum.MIN_REQUIREMENTS_ERROR);
                }
                stuffsToRemove.add(stuffToRemove);
            } else {
                if(!stuffService.isExist(profile, baseStuffId)) {
                    log.error("error apply stuff recipe [{}]: base stuff is absent [{}]", recipeId, baseStuffId);
                    // отсутствует необходимый предмет
                    return new CraftItemResult(ShopResultEnum.MIN_REQUIREMENTS_ERROR);
                }
                stuffsToRemove.add(baseStuffId);
            }
        }

        int[] profilesReagents = getReagentsForProfile(profile.getId()).getValues();
        Map<Byte, Integer> recipeReagentsMap = recraft ? recipe.getRecraftReagentsMap() : recipe.getReagentsMap();

        int needRuby = 0;
        int needMoney = 0;

        // необходимо рубинов если не хватает ресурсов
        int realMoneyForAbsentReagents = realMoneyForAbsentReagents(profilesReagents, recipeReagentsMap);

        if(moneyType == MoneyType.REAL_MONEY) {
            needRuby = (recraft ? recipe.getRecraftRealMoney() : recipe.needRealMoney()) + realMoneyForAbsentReagents;
        } else if(realMoneyForAbsentReagents == 0) {
            needMoney = recraft ? recipe.getRecraftMoney() : recipe.needMoney();
        } else {
            log.error("error apply stuff recipe [{}]: needed reagents is absent", recipeId);
            // отсутствует необходимый предмет
            return new CraftItemResult(ShopResultEnum.MIN_REQUIREMENTS_ERROR);
        }

        if(profile.getRealMoney() < needRuby) {
            log.error("error applying craft recipe [{}]: no enougth ruby [{}]", recipeId, needRuby - profile.getRealMoney());
            return new CraftItemResult(ShopResultEnum.NOT_ENOUGH_MONEY);
        }

        if(profile.getMoney() < needMoney) {
            log.error("error applying craft  recipe [{}]: no enougth money [{}]", recipeId, needMoney - profile.getMoney());
            return new CraftItemResult(ShopResultEnum.NOT_ENOUGH_MONEY);
        }

        try {
            CraftItemResult craftItemResult = new CraftItemResult(ShopResultEnum.SUCCESS);
            // применяем рецепт
            int resultItemId = !recraft ? rollDice(recipe.getResultStuffMassMap()) : rollDice(recipe.getResultStuffMassMap(), prevAssembledStuffId);

            int unluckyCraftCount = 0;
            boolean successRecraft = false;

            QuestEntity questEntity = questService.getQuestEntity(profile);
            Map<Short, Integer> unluckyCraftCounts = questEntity.q3().unluckyCraftCounts;
            if(!recraft) {
                craftItemResult.stuffId = (short) resultItemId;
                stuffService.addStuffPermanentlyOrTemporarily(profile, craftItemResult.stuffId);
                questEntity.dirty = unluckyCraftCounts.remove(recipeId) != null;
            } else {
                successRecraft = resultItemId > prevAssembledStuffId;

                // если не удалось скрафтить след. в иерархии предмет и такой предмет существует
                if(!successRecraft) {
                    unluckyCraftCount = unluckyCraftCounts.getOrDefault(recipeId, 0) + 1;
                    int unluckyThreshold = unluckyThresholds
                            .getOrDefault(maxResultItemLevel, Collections.emptyMap())
                            .getOrDefault(prevAssembledStuffId % 10 + 1, Integer.MAX_VALUE);
                    if(unluckyCraftCount >= unluckyThreshold) {
                        resultItemId = prevAssembledStuffId + 1;
                        successRecraft = true;
                    } else {
                        unluckyCraftCounts.put(recipeId, unluckyCraftCount);
                        questEntity.dirty = true;
                    }
                }

                if(successRecraft) {
                    craftItemResult.stuffId = (short) resultItemId;
                    // снимаем предыдущий предмет
                    stuffService.removeStuff(profile, prevAssembledStuffId);
                    stuffService.addStuffPermanentlyOrTemporarily(profile, craftItemResult.stuffId);
                    // сбрасываем счетчик неудачных крафтов если он был
                    if(unluckyCraftCounts.containsKey(recipeId)) {
                        unluckyCraftCounts.remove(recipeId);
                        questEntity.dirty = true;
                    }
                } else {
                    craftItemResult.stuffId = prevAssembledStuffId;
                }
            }

            int[] removedReagents = new int[profilesReagents.length];
            // минусуем реагенты рецепта
            for(Byte reagentId : recipeReagentsMap.keySet()) {
                int newValue = Math.max(0, profilesReagents[reagentId] - recipeReagentsMap.get(reagentId));
                removedReagents[reagentId] = -(profilesReagents[reagentId] - newValue);
                profilesReagents[reagentId] = newValue;
            }
            profile.getReagents().setDirty(true);

            // забираем базовые предметы
            for(Short baseStuffId : stuffsToRemove) {
                stuffService.removeStuff(profile, baseStuffId);
            }

            ItemType itemType = !recraft ? ItemType.ASSEMBLE_STUFF : ItemType.REASSEMBLE_STUFF;

            if(needMoney > 0) {
                // списываем деньги за апгрейд
                profile.setMoney(profile.getMoney() - needMoney);
                //логируем покупку апгрейда
                statisticService.buyItemStatistic(profile.getId(), MoneyType.MONEY.getType(), needMoney, itemType, 1, resultItemId, profile.getLevel());
            } else if(needRuby > 0) {
                // списываем реалы за апгрейд и покупку недостающих реагентов
                profile.setRealMoney(profile.getRealMoney() - needRuby);
                //логируем покупку апгрейда
                statisticService.buyItemStatistic(profile.getId(), MoneyType.REAL_MONEY.getType(), needRuby, itemType, 1, resultItemId, profile.getLevel());
            }

            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PURCHASE, profile,
                    Param.eventType, itemType,
                    Param.recipeId, recipeId,
                    Param.itemId, craftItemResult.stuffId,
                    "craftedItemId", successRecraft ? "" + prevAssembledStuffId : "",
                    "unluckyCraftCount", unluckyCraftCount,
                    Param.money, -needMoney,
                    Param.realMoney, -needRuby,
                    Param.items, stuffsToRemove.stream().mapToInt(i -> -i).toArray(),
                    Param.reagents, ReagentsEntity.getReagentValues(removedReagents),
                    Param.profile_reagents, profile.getReagents().getReagentValues()
            );

            if(isLegendaryItem(craftItemResult.stuffId)) {
                String params = "" + craftItemResult.stuffId;
                chatService.postToChat(ChatAction.CraftLegendaryItem, params, profile);
                clanService.postToChat(profile.getSocialId(), profile.getId().intValue(), Messages.CRAFT_LEGENDARY_ITEM_CHAT_ACTION, params);
            }

            return craftItemResult;
        } catch (Exception e) {
            log.error("error apply stuff recipe [" + recipeId + "]: " + e.toString(), e);
            return new CraftItemResult(ShopResultEnum.ERROR);
        }
    }

    private boolean isLegendaryItem(short stuffId) {
        return stuffId % 10 >= 6;
    }

    public OpenChestResult openChest(@NotNull UserProfile profile, short recipeId) {
        // частный случай крафта, открытие празничных сундуков

        StuffRecipe recipe = stuffRecipes.get(recipeId);
        if(recipe == null) {
            log.error("stuff recipe not found by id [{}]", recipeId);
            return new OpenChestResult(ShopResultEnum.ERROR);
        }

        int[] profilesReagents = getReagentsForProfile(profile.getId()).getValues();
        Map<Byte, Integer> recipeReagentsMap = recipe.getReagentsMap();

        // необходимо рубинов если не хватает реагентов
        if(!isReagentsEnough(profilesReagents, recipeReagentsMap)) {
            log.error("error apply stuff recipe [{}]: needed reagents is absent! [{}]", recipeId, recipeReagentsMap);
            // отсутствует необходимый реагент
            return new OpenChestResult(ShopResultEnum.MIN_REQUIREMENTS_ERROR);
        }

        try {
            OpenChestResult openChestResult = new OpenChestResult(ShopResultEnum.SUCCESS);
            // применяем рецепт
            int resultItemId = rollDice(recipe.getResultStuffMassMap());
            int itemId = 0;
            int weaponCount = 0;
            int rubyCount = 0;
            int moneyCount = 0;
            int experience = 0;
            int battles = 0;
            int bossToken = 0;
            int wagerToken = 0;

            int[] loggedReagents = new int[profilesReagents.length];

            Map<Integer, CraftResultItem> craftResultItemMapForRecipe = craftResultItemsMap.get(recipeId);
            if(craftResultItemMapForRecipe != null) {
                CraftResultItem craftResultItem = craftResultItemMapForRecipe.get(resultItemId);
                if(craftResultItem.seasonStuff) {
                    openChestResult.stuffId = seasonService.map(SeasonService::getCurrentSeasonStuffArr)
                            .map(stuff -> stuff[new Random().nextInt(stuff.length)])
                            .orElse(0).shortValue();
                    itemId = openChestResult.stuffId;
                    stuffService.addOrExpandTemporalStuff(profile, openChestResult.stuffId);
                } else if(craftResultItem.moneyCount > 0) {
                    openChestResult.moneyCount = craftResultItem.moneyCount;
                    moneyCount = openChestResult.moneyCount;
                    new AddMoneyEvent(moneyCount, 1).runEvent(profile);
                } else if(craftResultItem.rubyCount > 0) {
                    openChestResult.rubyCount = craftResultItem.rubyCount;
                    rubyCount = openChestResult.rubyCount;
                    new AddRealMoneyEvent(rubyCount).runEvent(profile);
                } else if(craftResultItem.weaponCount > 0) {
                    int weaponId = seasonService.map(SeasonService::getCurrentSeasonWeaponsArr)
                            .map(weapons -> weapons[new Random().nextInt(weapons.length)])
                            .orElse(0);
                    if(weaponId > 0) {
                        BackpackItem backpackItem = profile.getBackpackItemByWeaponId(weaponId);
                        if(backpackItem == null || getParamValue(OpenChestWeaponLimit) <= 0 || backpackItem.getCount() < getParamValue(OpenChestWeaponLimit)) {
                            openChestResult.weaponId = weaponId;
                            openChestResult.weaponCount = craftResultItem.weaponCount;
                            itemId = openChestResult.weaponId;
                            weaponCount = openChestResult.weaponCount;
                            weaponService.addOrUpdateWeapon(profile, openChestResult.weaponId, openChestResult.weaponCount);
                            //Достижения на сбор сезонного оружия
                            openChestResult.increaseAchievementsResult = consumeIncreaseSeasonWeapon(profile, weaponId, weaponCount);
                        } else {
                            //Если в ящике из главного меню выпадает орудие, которого у игрока уже больше лимита, выдавать вместо орудия фузы
                            openChestResult.moneyCount = AppUtils.randomInt(getParamValue(OpenChestInsteadWeaponMoneyFrom), getParamValue(OpenChestInsteadWeaponMoneyTo));
                            moneyCount = openChestResult.moneyCount;
                            profile.setMoney(profile.getMoney() + openChestResult.moneyCount);
                        }
                    }
                } else if(craftResultItem.experience > 0) {
                    if(profile.getLevel() == levelCreator.getMaxLevel()) {
                        openChestResult.moneyCount = craftResultItem.experience * 4;
                        moneyCount = openChestResult.moneyCount;
                        new AddMoneyEvent(moneyCount, 1).runEvent(profile);
                    } else {
                        openChestResult.experience = craftResultItem.experience;
                        experience = openChestResult.experience;
                        new AddExperienceEvent(experience, 1, profileExperienceService).runEvent(profile);
                    }
                } else if(craftResultItem.medalCount > 0) {
                    openChestResult.medalCount = craftResultItem.medalCount;
                    new AddReagentEvent(Reagent.medal.getIndex(), craftResultItem.medalCount, this).runEvent(profile);
                    loggedReagents[Reagent.medal.getIndex()] = craftResultItem.medalCount;
                } else if(craftResultItem.mutagenCount > 0) {
                    openChestResult.mutagenCount = craftResultItem.mutagenCount;
                    new AddReagentEvent(Reagent.mutagen.getIndex(), craftResultItem.mutagenCount, this).runEvent(profile);
                    loggedReagents[Reagent.mutagen.getIndex()] = craftResultItem.mutagenCount;
                } else if(craftResultItem.battles > 0) {
                    openChestResult.battles = craftResultItem.battles;
                    battles = openChestResult.battles;
                    new AddBattlesCountEvent(battleService, battles).runEvent(profile);
                } else if(craftResultItem.wagerToken > 0) {
                    openChestResult.wagerToken = craftResultItem.wagerToken;
                    wagerToken = openChestResult.wagerToken;
                    new AddWagerWinAwardTokenEvent(dailyRegistry, wagerToken).runEvent(profile);
                } else if(craftResultItem.bossToken > 0) {
                    openChestResult.bossToken = craftResultItem.bossToken;
                    bossToken = openChestResult.bossToken;
                    new AddBossWinAwardTokenEvent(dailyRegistry, bossToken).runEvent(profile);
                }
            }

            // минусуем реагенты рецепта
            for(Byte reagentId : recipeReagentsMap.keySet()) {
                int newValue = Math.max(0, profilesReagents[reagentId] - recipeReagentsMap.get(reagentId));
                loggedReagents[reagentId] = -(profilesReagents[reagentId] - newValue);
                profilesReagents[reagentId] = newValue;
            }
            profile.getReagents().setDirty(true);


            ItemType itemType = ItemType.OPEN_CHEST;

            //логируем открытие сундука
            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PURCHASE, profile,
                    Param.eventType, itemType,
                    Param.profile_reagents, profile.getReagents().getReagentValues(),
                    reagents, ReagentsEntity.getReagentValues(loggedReagents),
                    Param.money, moneyCount,
                    Param.realMoney, rubyCount,
                    Param.itemId, itemId,
                    Param.itemCount, weaponCount,
                    Param.experience, experience,
                    Param.battles, battles,
                    Param.wagerToken, wagerToken,
                    Param.bossToken, bossToken
            );
            if(rubyCount > 0 || moneyCount > 0) {
                statisticService.awardStatistic(profile.getId(), moneyCount, rubyCount, 0, AwardTypeEnum.OPEN_CHEST.getType(), "");
            }

            return openChestResult;
        } catch (Exception e) {
            log.error("error apply stuff recipe [" + recipeId + "]: " + e.toString(), e);
            return new OpenChestResult(ShopResultEnum.ERROR);
        }
    }

    IncreaseAchievementsResult consumeIncreaseSeasonWeapon(UserProfile profile, int weaponId, int weaponCount) {
        if(!seasonService.isPresent())
            return null;
        String currentSeasonKey = seasonService.get().getCurrentSeasonStartDate().toString();
        QuestEntity questEntity = questService.getQuestEntity(profile);
        Map<Integer, Integer> currentSeasonData = questEntity.q4().progress.merge(currentSeasonKey, new HashMap<>(), (oldValue, value) -> oldValue);
        int currentSeasonalWeaponProgress = currentSeasonData.getOrDefault(weaponId, 0);
        // у достижения 3 ступени: собрать 5-15-50 штук, награда: 100 фузов - 300 фузов - 5 рубинов
        int newValue = currentSeasonalWeaponProgress + weaponCount;

        GenericAward award = null;
        if(currentSeasonalWeaponProgress < 5 && newValue >= 5) {
            award = GenericAward.builder().addMoney(100).build();
        } else if(currentSeasonalWeaponProgress < 15 && newValue >= 15) {
            award = GenericAward.builder().addMoney(300).build();
        } else if(currentSeasonalWeaponProgress < 50 && newValue >= 50) {
            award = GenericAward.builder().addRealMoney(5).build();
        }
        List<GenericAwardStructure> awardStructures = Collections.emptyList();
        List<Integer> thresholdAchievementsIndex = Collections.emptyList();
        List<Integer> thresholdAchievementsOldValues = Collections.emptyList();
        if(award != null) {
            awardStructures = profileBonusService.awardProfile(award, profile, AwardTypeEnum.ACHIEVE,
                    "seasonalWeaponId", weaponId,
                    "oldWeaponCount", currentSeasonalWeaponProgress,
                    "newWeaponCount", newValue
            );
            thresholdAchievementsIndex = Collections.singletonList(weaponId);
            thresholdAchievementsOldValues = Collections.singletonList(currentSeasonalWeaponProgress);
        }
        IncreaseAchievementsResult increaseAchievementsResult = new IncreaseAchievementsResult(
                new int[]{weaponId},
                new int[]{newValue},
                thresholdAchievementsIndex,
                thresholdAchievementsOldValues,
                Collections.emptyList(),
                0L,
                (byte) 0
        );
        increaseAchievementsResult.awards = awardStructures;
        currentSeasonData.put(weaponId, newValue);
        questEntity.dirty = true;
        return increaseAchievementsResult;
    }

    private int randomInt(int from, int to) {
        if(from == to)
            if(from > 0)
                return from;
            else
                throw new IllegalArgumentException("'from' and 'to' both is zero!");
        else if(from > to)
            throw new IllegalArgumentException("from > to");
        else
            return from + new Random().nextInt(to - from + 1);
    }

    public LoginAwardStructure downgradeWeaponsInaccessibleByLevel(UserProfile profile) {
        List<Recipe> recipesToRemove = new ArrayList<>();
        for(short recipeId : profile.getRecipes()) {
            Recipe recipe = recipes.get(recipeId);
            if(recipe != null && recipe.needLevel > profile.getLevel()) {
                recipesToRemove.add(recipe);
            }
        }
        int realMoney = 0;
        for(Recipe recipe : recipesToRemove) {
            rollbackRecipe(profile, recipe.getId(), false);
            realMoney += realMoneyForReagents(recipe.getReagentsMap());
        }
        if(realMoney > 0) {
            LoginAwardStructure result = new LoginAwardStructure();
            result.awardType = AwardTypeEnum.DOWNGRADE_WEAPONS_COMPENSATION;
            result.attach = recipesToRemove.stream().map(r -> "" + r.getId()).collect(Collectors.joining(","));
            result.awards = profileBonusService.awardProfile(GenericAward.builder().addRealMoney(realMoney).build(), profile, result.awardType,
                    "removedRecipes", recipesToRemove.stream().map(r -> "" + r.getId() + "_need_" + r.needLevel).collect(Collectors.joining(",", "[", "]")),
                    Param.profile_recipes, Arrays.toString(profile.getRecipes())
            );
            return result;
        } else {
            return null;
        }
    }

//====================== Getters and Setters =================================================================================================================================================

    public Map<Short, Recipe> getAllRecipesMap() {
        return recipes;
    }

    public Map<Reagent, Integer> getReagentsPrice() {
        return this.reagentsPrice;
    }

    @Value("#{craftReagentsPrice}")
    public void setReagentsPrice(Map<Reagent, Integer> reagentsPrice) {
        this.reagentsPrice = new EnumMap<>(reagentsPrice);
    }

    @Value("#{craftRecipesList}")
    public void setRecipeList(List<Recipe> recipesList) {
        this.recipes = new HashMap<Short, Recipe>();
        for(Recipe recipe : recipesList) {
            this.recipes.put(recipe.getId(), recipe);
        }
    }

    @Value("#{craftStuffRecipesList}")
    public void setStuffRecipeList(List<StuffRecipe> recipesList) {
        this.stuffRecipes = new HashMap<Short, StuffRecipe>();
        for(StuffRecipe recipe : recipesList) {
            this.stuffRecipes.put(recipe.getId(), recipe);
        }
    }

    @Value("#{craftSimpleBattleLevelReagentsMassMap}")
    public void setSimpleBattleReagentsChanceMap(Map<String, String> battleReagentsChanceMap) {
        for(Map.Entry<String, String> entry : battleReagentsChanceMap.entrySet()) {
            int level = Integer.parseInt(entry.getKey().trim());
            int[][] reagentsMassArray = parseReagentsMassString(entry.getValue());

            this.simpleBattleReagentsChanceMap.put(level, reagentsMassArray);
        }
    }

    @Value("#{craftBossBattleReagentsMassMap}")
    public void setBossBattleReagentsMassMap(Map<String, String> battleRagentsChanceMap) {
        for(Map.Entry<String, String> entry : battleRagentsChanceMap.entrySet()) {
            int missionId = Integer.parseInt(entry.getKey().trim());
            int[][] reagentsMassArray = parseReagentsMassString(entry.getValue());

            this.bossBattleReagentsChanceMap.put(missionId, reagentsMassArray);
        }
    }

    @Value("#{craftHeroicBattleReagentsMassMap}")
    public void setHeroicBattleReagentsMassMap(Map<Integer, String> battleReagentsChanceMap) {
        for(Map.Entry<Integer, String> entry : battleReagentsChanceMap.entrySet()) {
            String[] ss = entry.getValue().split(" ");
            byte[] reagents = new byte[ss.length];
            for(int i = 0; i < ss.length; i++) {
                reagents[i] = Byte.valueOf(ss[i]);
            }
            this.heroicBattleReagentsChanceMap.put(entry.getKey(), reagents);
        }
    }

    @Value("#{craftFriendSearchReagentsMassMap}")
    public void setFriendSearchReagentsMassMap(Map<String, String> battlereagentsChanceMap) {
        for(Map.Entry<String, String> entry : battlereagentsChanceMap.entrySet()) {
            int missionId = Integer.parseInt(entry.getKey().trim());
            int[][] reagentsMassArray = parseReagentsMassString(entry.getValue());

            this.friendSearchReagentsMassMap.put(missionId, reagentsMassArray);
        }
    }

    @Value("#{craftPvpBattleReagentsMassMap}")
    public void setPvpBattleReagentsMassMap(Map<Integer, Map<Integer, String>> battleReagentsChanceMap) {
        for(Map.Entry<Integer, Map<Integer, String>> wagerMapEntry : battleReagentsChanceMap.entrySet()) {
            NavigableMap<Integer, int[][]> levelChanceMap = new TreeMap<>();
            for(Map.Entry<Integer, String> entry : wagerMapEntry.getValue().entrySet()) {
                int level = entry.getKey();
                int[][] reagentsMassArray = parseReagentsMassString(entry.getValue());

                levelChanceMap.put(level, reagentsMassArray);
            }
            int wager = wagerMapEntry.getKey();
            this.pvpBattleReagentsChanceMap.put(wager, levelChanceMap);
        }
    }

    public boolean isHatCrafted(short hat) {
        return hat >= 1500;
    }

    public boolean isKitCrafted(short kit) {
        return kit >= 2500;
    }

    /**
     * Для крафтового предмета сбрасываем к минимальному из вариантов крафта
     * <p>
     * Convention: разные предметы идут по десяткам, т.е. {1501, 1502, ...} - это первый предмет, а {1511, 1512, 1513, ...} - следующий и т.д.
     *
     * @param stuff id предмета, который нужно оберзать
     * @return id обрезанного предмета
     */
    public short truncateCraft(short stuff) {
        short truncateTo = 10;
        short baseIndex = 1;
        // округляем до 10 и берём первый - с единицей в последнем разряде
        return (short) ((stuff / truncateTo) * truncateTo + baseIndex);
    }

    public Map<Short, Recipe> getRecipes() {
        return recipes;
    }

    public Map<Short, StuffRecipe> getStuffRecipes() {
        return stuffRecipes;
    }

    public int getParamValue(ParamsEnum param) {
        return params.get(param);
    }

    public Tuple2<Short, Map<Byte, Integer>> tryHitRareItemOrReagentsInstead(UserProfile profile, int[][] rareAwardMassMap) {
        //Из боссов падает ресурс с маленьким шансом. Если он не нужен, есть в инвентаре или есть уже крафт где он используется, то даются 3 медали
        Tuple2<Short, Map<Byte, Integer>> result = Tuple.of((short) 0, Collections.emptyMap());
        if(ArrayUtils.isEmpty(rareAwardMassMap)) {
            return result;
        }
        short rareItemId;
        if(debugMode) {
            rareItemId = (short) rareAwardMassMap[ThreadLocalRandom.current().nextInt(2, rareAwardMassMap.length)][0];
        } else {
            rareItemId = (short) CraftService.rollDice(rareAwardMassMap);
        }
        if(rareItemId > 0) {
            int medals = isHaveItemOrCraftBasedOn(rareItemId, profile) ? medalsCountInsteadOfExistsRareItem : 0;
            if(medals > 0) {
                result = Tuple.of((short) 0, Map.of(Reagent.medal.getIndex(), medals));
            } else {
                result = Tuple.of(rareItemId, Collections.emptyMap());
            }
        }
        return result;
    }

    public boolean isHaveItemOrCraftBasedOn(short rareItemId, UserProfile profile) {
        if(stuffService.isExist(profile, rareItemId)) {
            return true;
        } else {
            StuffRecipe stuffRecipe = getStuffRecipes().values().stream().filter(r -> r.getBaseStuffsSet().contains(rareItemId)).findFirst().orElse(null);
            if(stuffRecipe != null) {
                if(stuffRecipe.getResultsStuffsSet().stream().anyMatch(itemId -> stuffService.isExist(profile, itemId))) {
                    return true;
                }
            } else {
                log.error("редкий предмет [{}] не является основой для крафта", rareItemId);
            }
        }
        return false;
    }

}

