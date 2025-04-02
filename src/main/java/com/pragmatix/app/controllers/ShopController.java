package com.pragmatix.app.controllers;

import com.pragmatix.app.common.*;
import com.pragmatix.app.messages.client.*;
import com.pragmatix.app.messages.server.*;
import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.app.messages.structures.BundleStructure;
import com.pragmatix.app.messages.structures.ShopItemStructure;
import com.pragmatix.app.messages.structures.TemporalStuffStructure;
import com.pragmatix.app.model.*;
import com.pragmatix.app.services.*;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.settings.*;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.sessions.Sessions;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.pragmatix.app.model.Weapon.WeaponType.*;
import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.PURCHASE;

/**
 * Контроллер который будет принимать с клиента
 * запросы на покупку или продажу вещей
 * <p/>
 * User: denis
 * Date: 07.11.2009
 * Time: 16:06:18
 */
@Controller
public class ShopController {

    private static final Logger logger = LoggerFactory.getLogger(ShopController.class);

    @Resource
    private WeaponService weaponService;

    @Resource(name = "battlePriceSettings")
    private ItemRequirements battlePriceSettings;

    @Resource(name = "bulkBattlesPriceSettings")
    private ItemRequirements bulkBattlesPriceSettings;

    @Resource
    private ReactionRateService reactionRateService;

    @Resource(name = "unlockMissionPriceSettings")
    private ItemRequirements unlockMissionPriceSettings;

    @Resource
    private StatisticService statisticService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private StuffService stuffService;

    @Resource
    private CheatersCheckerService cheatersCheckerService;

    @Resource
    private ShopService shopService;

    @Resource
    private SkinService skinService;

    @Resource
    private BundleService bundleService;

    @Resource
    private ProfileService profileService;

    @Value("#{battleAwardSettings.awardSettingsMap}")
    private Map<Short, SimpleBattleSettings> awardSettingsMap;

    @Value("#{reactionRatePriceSettings}")
    private ItemRequirements reactionRatePriceSettings;

    @Resource
    private RacePriceSettings racePriceSettings;

    private static final int Money = 0;
    private static final int RealMoney = 1;

    /**
     * обработка команды покупки вещей в магазине
     *
     * @param buyShopItems команда которую послал нам клиент
     * @param profile      профайл клиента от которого пришла команда
     * @return результат продажи для отправки на клиент
     * @throws Exception при ошиби БД
     */
    @OnMessage
    public ShopResult onBuyShopItems(BuyShopItems buyShopItems, final UserProfile profile) throws Exception {
        try {
            int[] need = {0, 0};
            // проверяем минимальные требования
            for(ShopItemStructure itemStructure : buyShopItems.items) {
                IItem item;
                //если id < 1000, то производется покупка оружия
                if(ItemCheck.isWeapon(itemStructure.id)) {
                    Weapon weapon = weaponService.getWeapon(itemStructure.id);
                    if(weapon != null) {
                        if(weapon.isType(INFINITE)) {
                            itemStructure.count = -1;
                        } else if(weapon.isType(COMPLEX)) {
                            itemStructure.count = Math.max(1, itemStructure.count);
                        } else if(weapon.isType(CONSUMABLE)) {
                            if(itemStructure.count <= 0) {
                                logger.error("try to buy consumable weapon as infinite one!");
                                itemStructure.count = 1;
                            }
                        } else {
                            logger.error("weapon not for sale! {}", weapon);
                            return new ShopResult(ShopResultEnum.ERROR);
                        }
                    }
                    item = weapon;
                } else {
                    item = stuffService.getStuff(itemStructure.id);
                    itemStructure.count = 1;
                }

                //проверяем не читер ли
                if(cheatersCheckerService.checkItemCount(profile, itemStructure.count)) {
                    logger.error("banned for trying to hack shop, item: {}", itemStructure);
                    return new ShopResult(ShopResultEnum.ERROR);
                }

                if(item != null) {
                    int itemCount = shopService.alignItemCount(itemStructure.count);
                    int cost;

                    if(itemStructure.isRealMoneyType()) {
                        cost = item.needRealMoney() * itemCount;
                        need[RealMoney] += cost;
                    } else {
                        if(profile.getLevel() >= item.needLevel()) {
                            cost = item.needMoney() * itemCount;
                            need[Money] += cost;
                        } else {
                            logger.error("minimal requirements violated for weaponId: {}", itemStructure.id);
                            return new ShopResult(ShopResultEnum.MIN_REQUIREMENTS_ERROR);
                        }
                    }
                    itemStructure.cost = cost;
                } else {
                    logger.error("can't get item by id: {}, from cache", itemStructure.id);
                    return new ShopResult(ShopResultEnum.ERROR);
                }
            }
            // если недостаточно денег для совершения покупки
            if(profile.getRealMoney() < need[RealMoney]) {
                logger.error("can't buy {} not enough realMoney {} need {}", Arrays.toString(buyShopItems.items), need[RealMoney] - profile.getRealMoney(), need[RealMoney]);
                return new ShopResult(ShopResultEnum.NOT_ENOUGH_MONEY);
            } else if(profile.getMoney() < need[Money]) {
                logger.error("can't buy {} not enough money {} need {}", Arrays.toString(buyShopItems.items), need[Money] - profile.getMoney(), need[Money]);
                return new ShopResult(ShopResultEnum.NOT_ENOUGH_MONEY);
            } else {
                List<Short> boughtStuffList = new ArrayList<>();
                List<TemporalStuffStructure> boughtTemporalStuffList = new ArrayList<>();
                List<BackpackItemStructure> boughtBackpackList = new ArrayList<>();

                // проверяем какие предметы необходимо просто создать а каким обновить количество
                for(final ShopItemStructure itemStructure : buyShopItems.items) {
                    if(ItemCheck.isWeapon(itemStructure.id)) {
                        int addedWeaponCount = weaponService.addOrUpdateWeaponReturnCount(profile, itemStructure.id, itemStructure.count);
                        if(addedWeaponCount != 0) {
                            boughtBackpackList.add(new BackpackItemStructure(itemStructure.id, addedWeaponCount));

                            firePurchaseWeapon(profile, itemStructure, addedWeaponCount);
                            //логируем покупку оружия
                            statisticService.buyShopItemStatistic(profile.getId(), itemStructure.id, itemStructure.moneyType, addedWeaponCount, profile.getLevel());
                            if(itemStructure.count > 0 && addedWeaponCount > 0 && addedWeaponCount < itemStructure.count){
                                // не всё оружие, по каким то причинам, не попало в рюкзак
                                revertWeaponCountCost(need, itemStructure, itemStructure.count - addedWeaponCount);
                            }
                        } else {
                            // оружие, по каким то причинам, не попало в рюкзак
                            int weaponCount = shopService.alignItemCount(itemStructure.count);
                            revertWeaponCountCost(need, itemStructure, weaponCount);
                        }
                    } else {
                        short stuffId = (short) itemStructure.id;
                        Stuff stuff = stuffService.getStuff(stuffId);
                        boolean addResult = false;
                        if(!stuffService.isExist(profile, stuffId)) {
                            if(stuff.isTemporal()) {
                                // бустеры снимаем с продажи
                                if(!stuff.isBoost()) {
                                    addResult = stuffService.addStuff(profile, stuffId, stuff.getExpireTime(), Stuff.EXPIRE_TIME_UNIT, true);
                                }
                            } else {
                                addResult = stuffService.addStuff(profile, stuffId);
                            }
                        }
                        if(addResult) {
                            if(stuff.isTemporal()) {
                                int expireDate = (int) ((System.currentTimeMillis() + Stuff.EXPIRE_TIME_UNIT.toMillis(stuff.getExpireTime())) / 1000);
                                boughtTemporalStuffList.add(new TemporalStuffStructure(stuffId, expireDate));
                                firePurchaseTemporalStuff(profile, itemStructure);
                            } else {
                                boughtStuffList.add(stuffId);
                                firePurchaseStuff(profile, itemStructure);
                            }

                            //логируем покупку предмета
                            statisticService.buyShopItemStatistic(profile.getId(), itemStructure.id, itemStructure.moneyType, itemStructure.count, profile.getLevel());
                        } else {
                            // предмет уже есть
                            logger.error("try add alredy purchased stuff {}", stuffId);
                            Stuff item = stuffService.getStuff(itemStructure.id);

                            // не списываем за него деньги
                            if(itemStructure.moneyType == MoneyType.REAL_MONEY.getType()) {
                                need[RealMoney] -= item.needRealMoney();
                            } else {
                                need[Money] -= item.needMoney();
                            }
                        }
                    }
                }

                // снимаем деньги только сейчас, а то вдруг исключение было
                profile.setMoney(profile.getMoney() - Math.abs(need[Money]));
                profile.setRealMoney(profile.getRealMoney() - Math.abs(need[RealMoney]));

                profileService.updateSync(profile);

                return new ShopResult(boughtBackpackList, boughtStuffList, boughtTemporalStuffList);
            }
        } catch (Exception ex) {
            logger.error("shop item ERROR: " + ex.toString(), ex);
            // отправляем на клиент инфу о том что произошла ошибка
            return new ShopResult(ShopResultEnum.ERROR);
        }
    }

    private void revertWeaponCountCost(int[] need, ShopItemStructure itemStructure, int weaponCount) {
        Weapon weapon = weaponService.getWeapon(itemStructure.id);
        logger.error("failure add bought weapon in backpack {} count={}", weapon, weaponCount);

        // не списываем за него деньги
        if(itemStructure.moneyType == MoneyType.REAL_MONEY.getType()) {
            need[RealMoney] -= weapon.needRealMoney() * weaponCount;
        } else {
            need[Money] -= weapon.needMoney() * weaponCount;
        }
    }

    private void firePurchaseWeapon(UserProfile profile, ShopItemStructure itemStructure, int count) {
        Weapon weapon = weaponService.getWeapon(itemStructure.id);
        int cost = itemStructure.isRealMoneyType() ? weapon.needRealMoney() * count : weapon.needMoney() * count;
        profileEventsService.fireProfileEventAsync(PURCHASE, profile,
                Param.eventType, ItemType.WEAPON.name(),
                Param.itemId, itemStructure.id,
                Param.itemCount, count,
                itemStructure.isRealMoneyType() ? Param.realMoney : Param.money, -cost,
                Param.profile_backpack, profileEventsService.fillBackpackTrimmed(profile)
        );
    }

    private void firePurchaseStuff(UserProfile profile, ShopItemStructure itemStructure) {
        profileEventsService.fireProfileEventAsync(PURCHASE, profile,
                Param.eventType, ItemType.STUFF.name(),
                Param.itemId, itemStructure.id,
                Param.itemCount, 1,
                itemStructure.isRealMoneyType() ? Param.realMoney : Param.money, -itemStructure.cost,
                Param.profile_stuff, profile.getStuff()
        );
    }

    private void firePurchaseTemporalStuff(UserProfile profile, ShopItemStructure itemStructure) {
        profileEventsService.fireProfileEventAsync(PURCHASE, profile,
                Param.eventType, ItemType.STUFF.name(),
                Param.itemId, itemStructure.id,
                Param.itemCount, 1,
                itemStructure.isRealMoneyType() ? Param.realMoney : Param.money, -itemStructure.cost,
                Param.profile_temporalStuff, ArrayUtils.isNotEmpty(profile.getTemporalStuff()) ? TemporalStuffService.toStringTemporalStuff(profile.getTemporalStuff()) : null
        );
    }

    /**
     * Покупка боя
     *
     * @param msg     сообщение от клиента
     * @param profile профайл пользователя от которого пришло сообщение
     * @return BuyBattleResult результат покупки
     * @throws Exception в случаи ошибки
     */
    @OnMessage
    public BuyBattleResult onBuyBattle(BuyBattle msg, UserProfile profile) throws Exception {
        try {
            ItemRequirements battlePrice = msg.bulk ? bulkBattlesPriceSettings : battlePriceSettings;
            int battlesCount = msg.bulk ? BuyBattle.BULK_BATTLES_COUNT : 1;
            ShopResultEnum shopResult = shopService.tryBuyItem(profile, battlePrice, ItemType.BATTLE, (byte) msg.moneyType, 1, 0,
                    Param.battles, battlesCount
            );
            if(!shopResult.isSuccess()) {
                return new BuyBattleResult(shopResult);
            }
            profile.setBattlesCount(profile.getBattlesCount() + battlesCount);

            profileService.updateSync(profile);

            return new BuyBattleResult(ShopResultEnum.SUCCESS);
        } catch (Exception ex) {
            logger.error("BuyBattle ERROR: ", ex);
            return new BuyBattleResult(ShopResultEnum.ERROR);
        }
    }

    @OnMessage
    public BuyRaceResponse onBuyRace(BuyRace msg, UserProfile profile) throws Exception {
        PurchaseResult purchaseResult = PurchaseResult.ERROR;
        short races;
        if(AppParams.IS_NOT_MOBILE()) {
            purchaseResult = shopService.buyRace(msg.race, msg.moneyType, profile);
            races = profile.getRaces();
        } else {
            // пока мобилки не перейдут на новый механизм покупки рас
            int raceId = msg.race.getType();
            if(profile.getRace() != raceId) {
                IItemRequirements itemRequirements = racePriceSettings.getPriceMap().get(raceId);
                if(itemRequirements != null) {
                    purchaseResult = shopService.tryBuyItemReturnCost(profile, itemRequirements, ItemType.RACE, msg.moneyType, 1, raceId,
                            "oldRace", Race.valueOf(profile.getRace())
                    );
                    if(purchaseResult.isSuccess()) {
                        profile.setRace(msg.race);

                        profileService.updateSync(profile);
                    }
                }
            } else {
                logger.error("ошибка покупки расы. [{}] это текущая раса игрока", Race.valueOf(profile.getRace()));
            }
            races = profile.getRace();
        }
        return new BuyRaceResponse(purchaseResult, msg.race, races, Sessions.getKey(profile));
    }

    @OnMessage
    public BuySkinResponse onBuySkin(BuySkin msg, UserProfile profile) throws Exception {
        PurchaseResult purchaseResult = skinService.buySkin(msg.skinId, msg.moneyType, profile);
        return new BuySkinResponse(purchaseResult, msg.skinId, profile.getSkins(), Sessions.getKey(profile));
    }

    @OnMessage
    public BuyReactionRateResult onBuyReactionRate(BuyReactionRate msg, UserProfile profile) throws Exception {
        ShopResultEnum result;
        if(AppParams.IS_NOT_MOBILE()) {
            result = reactionRateService.buyReactionRate(msg.level, profile);
        } else {
            if(reactionRatePriceSettings.isEmpty())
                throw new IllegalStateException("не указана цена покупки реакции!");
            result = tryBuyReactionRate(profile, reactionRatePriceSettings, ItemType.REACTION_RATE, MoneyType.REAL_MONEY.getType(), msg.reactionRateCount, 0);
            if(result.isSuccess()) {
                profile.setReactionRate(profile.getReactionRate() + shopService.alignItemCount(msg.reactionRateCount));
            }
        }
        if(result.isSuccess()) {
            profileService.updateSync(profile);

            return new BuyReactionRateResult(result, msg.level, profile.getReactionRate());
        } else {
            return new BuyReactionRateResult(result);
        }
    }

    public ShopResultEnum tryBuyReactionRate(UserProfile profile, IItemRequirements itemRequirements, ItemType itemType, int moneyType, int itemCount, int itemId) {
        itemCount = shopService.alignItemCount(itemCount);

        //проверяем не читер ли
        if(cheatersCheckerService.checkItemCount(profile, itemCount)) {
            logger.error("banned for trying to hack shop: reactionRate.count = {}", itemCount);
            return ShopResultEnum.ERROR;
        }
        // количество реакции за один рубин
        int ratesByRyby = 3;
        // реакция покупается только за рубины
        int needRealMoney = (int) Math.ceil((float) itemRequirements.needRealMoney() / (float) ratesByRyby * (float) itemCount);
        // если недостаточно денег для совершения покупки
        if(profile.getRealMoney() < needRealMoney) {
            return ShopResultEnum.NOT_ENOUGH_MONEY;
        } else {
            profile.setRealMoney(profile.getRealMoney() - needRealMoney);
            //сохроняем статистику покупки
            // внимание! логгируем не истинное количество а количество деленное на 3
            statisticService.buyItemStatistic(profile.getId(), moneyType, itemRequirements.needRealMoney(), itemType, itemCount / ratesByRyby, itemId, profile.getLevel());
        }
        return ShopResultEnum.SUCCESS;
    }

    @OnMessage
    public BuyUnlockMissionResult onBuyUnlockMission(BuyUnlockMission msg, UserProfile profile) throws Exception {
        try {
            // id мисии больше нуля и мисиия с таким id прописана в конфиге
            SimpleBattleSettings battleSettings = awardSettingsMap.get(msg.missionId);
            if(msg.missionId <= 0 || battleSettings == null || !battleSettings.isBossBattle()) {
                logger.error("try buy unlock for wrong mission id [{}]", msg.missionId);
                return new BuyUnlockMissionResult(ShopResultEnum.ERROR);
            }

            short skippedMissionId = (short) (msg.missionId - 1);
            int unlockMissionCount = battleSettings.isNewBossBattle() ?
                    skippedMissionId - Math.max(100, profile.getCurrentNewMission()) :// id совместных миссий начинаются со 101
                    skippedMissionId - Math.max(profile.getCurrentMission(), 0);

            // не пытаемся разблокировать текущую мисиию
            if(unlockMissionCount == 0) {
                logger.error("try buy unlock for next mission id [{}]", msg.missionId);
                return new BuyUnlockMissionResult(ShopResultEnum.ERROR);
            }

            ShopResultEnum shopResult = shopService.tryBuyItem(profile, unlockMissionPriceSettings, ItemType.UNLOCK_MISSION, MoneyType.REAL_MONEY.getType(), unlockMissionCount, msg.missionId);
            if(!shopResult.isSuccess()) {
                return new BuyUnlockMissionResult(shopResult);
            }

            if(battleSettings.isNewBossBattle()) {
                profile.setCurrentNewMission(skippedMissionId);
            } else {
                profile.setCurrentMission(skippedMissionId);
            }
            profileService.updateSync(profile);

            return new BuyUnlockMissionResult(ShopResultEnum.SUCCESS);
        } catch (Exception ex) {
            logger.error("BuyUnlockMission ERROR: " + ex.toString(), ex);
            return new BuyUnlockMissionResult(ShopResultEnum.ERROR);
        }
    }

    @OnMessage
    public GetBundlesResult onGetBundles(GetBundles msg, UserProfile profile) {
        List<BundleStructure> bundles = bundleService.getValidBundles();
        for(BundleStructure bundle : bundles) {
            if(bundle.finish != null)
                // жестяная жесть конечно, но клиент не хочет (или не умеет) парится с вычислением времени
                bundle.expireInSeconds = (int) ((bundle.finish.getTime() - TimeUnit.MINUTES.toMillis(bundleService.PurchaseTimeoutInMinutes) - System.currentTimeMillis()) / 1000L);
        }
        bundles.sort(Comparator.comparingInt(BundleStructure::getOrder));
        return new GetBundlesResult(bundles);
    }

    @OnMessage
    public BuySelectRaceResponse onBuySelectRace(BuySelectRace msg, UserProfile profile) {
        PurchaseResult purchaseResult = shopService.buySelectRace(msg.race, profile);
        if(purchaseResult.isSuccess()) {
            skinService.setActiveSkin(profile, msg.race, msg.skinId);

            profileService.updateSync(profile);
        }
        return new BuySelectRaceResponse(purchaseResult, msg.race, skinService.getSkin(profile), Sessions.getKey(profile));
    }

    @OnMessage
    public BuyRenameResult onBuyRename(BuyRename msg, UserProfile profile) {
        ShopResultEnum resultEnum = shopService.buyRename(msg.teamMemberId, msg.name, msg.moneyType, profile);
        return new BuyRenameResult(resultEnum, msg.teamMemberId, msg.name, Sessions.getKey());
    }

    @OnMessage
    public BuyGroupSlotResult onBuyGroupSlot(BuyGroupSlot msg, UserProfile profile) {
        ShopResultEnum shopResult = shopService.buyGroupSlot(msg.newSlotIndex, msg.moneyType, profile);
        return new BuyGroupSlotResult(shopResult, msg.newSlotIndex, Sessions.getKey());
    }
}
