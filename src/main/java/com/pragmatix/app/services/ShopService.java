package com.pragmatix.app.services;

import com.pragmatix.app.common.*;
import com.pragmatix.app.model.PurchaseResult;
import com.pragmatix.app.model.RestrictionItem;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.settings.IItemRequirements;
import com.pragmatix.app.settings.ItemRequirements;
import com.pragmatix.app.settings.RacePriceSettings;
import com.pragmatix.app.settings.RenameRequirements;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.craft.domain.Reagent;
import com.pragmatix.craft.domain.ReagentsEntity;
import com.pragmatix.craft.services.CraftService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.05.13 11:20
 */
@Service
public class ShopService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private StatisticService statisticService;

    @Resource
    private ProfileService profileService;

    @Resource
    private ItemRequirements selectRacePriceSettings;

    @Resource
    private RenameRequirements renamePriceSettings;

    @Resource
    private RacePriceSettings racePriceSettings;

    @Resource
    private List<ItemRequirements> extraGroupSlotPriceSettings;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private CraftService craftService;

    @Resource
    private BattleService battleService;

    @Resource
    private RestrictionService restrictionService;

    public int alignItemCount(int itemCount) {
        // itemCount может быть только положительным
        return Math.abs(itemCount);
    }

    public ShopResultEnum checkPossibilityOfBuyingItem(UserProfile profile, IItemRequirements itemRequirements, MoneyType moneyType, int itemCount) {
        itemCount = alignItemCount(itemCount);

        if(moneyType == MoneyType.REAL_MONEY) {
            if(!ItemCheck.hasPrice(itemRequirements.needRealMoney()))
                return ShopResultEnum.NOT_FOR_SALE;
            int needRealMoney = itemRequirements.needRealMoney() * itemCount;
            // если недостаточно денег для совершения покупки
            if(profile.getRealMoney() < needRealMoney) {
                return ShopResultEnum.NOT_ENOUGH_MONEY;
            }
        } else if(moneyType == MoneyType.MONEY) {
            if(!ItemCheck.hasPrice(itemRequirements.needMoney()))
                return ShopResultEnum.NOT_FOR_SALE;
            int needMoney = itemRequirements.needMoney() * itemCount;

            if(profile.getLevel() < itemRequirements.needLevel()) {
                // если уровень игрока меньше необходимого
                return ShopResultEnum.MIN_REQUIREMENTS_ERROR;
            } else if(profile.getMoney() < needMoney) {
                // если недостаточно денег для совершения покупки
                return ShopResultEnum.NOT_ENOUGH_MONEY;
            }
        } else if(moneyType == MoneyType.BATTLES) {
            if(!ItemCheck.hasPrice(itemRequirements.needBattles()))
                return ShopResultEnum.NOT_FOR_SALE;
            int needBattles = itemRequirements.needBattles() * itemCount;
            if(profile.getBattlesCount() < needBattles) {
                return ShopResultEnum.NOT_ENOUGH_BATTLES;
            }
        } else if(moneyType == MoneyType.REAGENTS) {
            if(itemRequirements.needReagents().values().stream().mapToInt(i -> i).sum() <= 0)
                return ShopResultEnum.NOT_FOR_SALE;
            if(!craftService.isReagentsEnough(profile, itemRequirements.needReagents()))
                return ShopResultEnum.NOT_ENOUGH_REAGENTS;
        }
        return ShopResultEnum.SUCCESS;
    }

    public PurchaseResult tryBuyItemReturnCost(UserProfile profile, IItemRequirements itemRequirements, ItemType itemType, MoneyType moneyType, int itemCount, int itemId, Object... extraParams) {
        ShopResultEnum shopResultEnum = checkPossibilityOfBuyingItem(profile, itemRequirements, moneyType, itemCount);
        if(shopResultEnum == ShopResultEnum.SUCCESS) {
            List<CostStructure> cost = Collections.emptyList();
            int price = 0;
            Param eventParam = null;
            Map<Reagent, Integer> removedReagents = Collections.EMPTY_MAP;

            if(moneyType == MoneyType.REAL_MONEY) {
                price = itemRequirements.needRealMoney();
                eventParam = Param.realMoney;

                profile.setRealMoney(profile.getRealMoney() - price * itemCount);
            } else if(moneyType == MoneyType.MONEY) {
                price = itemRequirements.needMoney();
                eventParam = Param.money;

                profile.setMoney(profile.getMoney() - price * itemCount);
            } else if(moneyType == MoneyType.BATTLES) {
                price = itemRequirements.needBattles();
                eventParam = Param.battles;

                battleService.decBattleCount(profile, price * itemCount);
            } else if(moneyType == MoneyType.REAGENTS) {
                removedReagents = ReagentsEntity.getReagentValues(craftService.withdrawReagents(profile, itemRequirements.needReagents()));
            }

            if(price > 0) {
                cost = Collections.singletonList(new CostStructure(moneyType, price * itemCount));
            } else if(removedReagents.size() > 0) {
                cost = removedReagents.entrySet().stream().map(e -> new CostStructure(MoneyType.REAGENTS, e.getKey().getIndex(), -e.getValue())).collect(Collectors.toList());
            }

            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PURCHASE, profile,
                    Param.eventType, itemType,
                    Param.itemId, itemId,
                    Param.itemCount, itemCount,
                    eventParam, -(price * itemCount),
                    Param.reagents, removedReagents,
                    Param.extraParams, extraParams
            );
            //сохроняем статистику покупки
            statisticService.buyItemStatistic(profile.getId(), moneyType.getType(), price, itemType, itemCount, itemId, profile.getLevel());

            return new PurchaseResult(ShopResultEnum.SUCCESS, cost);
        } else {
            return new PurchaseResult(shopResultEnum, Collections.emptyList());
        }
    }

    public ShopResultEnum tryBuyItem(UserProfile profile, IItemRequirements itemRequirements, ItemType itemType, int moneyType, int itemCount, int itemId, Object... extraParams) {
        ShopResultEnum shopResultEnum = checkPossibilityOfBuyingItem(profile, itemRequirements, MoneyType.valueOf(moneyType), itemCount);
        if(shopResultEnum == ShopResultEnum.SUCCESS) {
            int price;
            Param eventParam;

            if(moneyType == MoneyType.REAL_MONEY.getType()) {
                price = itemRequirements.needRealMoney();
                eventParam = Param.realMoney;

                profile.setRealMoney(profile.getRealMoney() - price * itemCount);
            } else {
                price = itemRequirements.needMoney();
                eventParam = Param.money;

                profile.setMoney(profile.getMoney() - price * itemCount);
            }

            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PURCHASE, profile,
                    Param.eventType, itemType,
                    Param.itemId, itemId,
                    Param.itemCount, itemCount,
                    eventParam, -(price * itemCount),
                    Param.extraParams, extraParams
            );
            //сохроняем статистику покупки
            statisticService.buyItemStatistic(profile.getId(), moneyType, price, itemType, itemCount, itemId, profile.getLevel());
        }
        return shopResultEnum;
    }

    public PurchaseResult buySelectRace(Race race, UserProfile profile) {
        if(!Race.hasRace(profile.getRaces(), race)) {
            log.error("раса [{}] у игрока отсутствует! в наличии: {}", race, Race.toList(profile.getRaces()));
            return PurchaseResult.ERROR;
        }
        if(profileService.getSelectRaceTimeLeft(profile) <= 0 || profileService.isVipActive(profile)) {
            log.error("рассу игрок может сменить бесплатно");
            return PurchaseResult.ERROR;
        }
        if(profile.getRace() == race.getType()) {
            log.error("игрок пытается купить смену расы на текущую");
            return PurchaseResult.ERROR;
        }

        PurchaseResult purchaseResult = tryBuyItemReturnCost(profile, selectRacePriceSettings, ItemType.SELECT_RACE, MoneyType.REAL_MONEY, 1, race.getType());
        if(!purchaseResult.isSuccess()) {
            return purchaseResult;
        }

        profile.setRace(race);
        profile.setSelectRaceTime(AppUtils.currentTimeSeconds());

        return purchaseResult;
    }

    public PurchaseResult buyRace(Race race, MoneyType moneyType, UserProfile profile) {
        IItemRequirements itemRequirements = racePriceSettings.getPriceMap().get(race.getType());
        if(itemRequirements == null) {
            log.error("Раса {} не доступна для покупки", race);
            return PurchaseResult.NOT_FOR_SALE;
        }
        if(Race.hasRace(profile.getRaces(), race)) {
            log.warn("Раса {} уже в наличии", race);
            return PurchaseResult.ERROR;
        }
        PurchaseResult shopResult = tryBuyItemReturnCost(profile, itemRequirements, ItemType.RACE, moneyType, 1, race.getType());
        if(!shopResult.isSuccess()) {
            return shopResult;
        }

        profile.setRace(race);
        profile.setRaces(Race.addRace(profile.getRaces(), race));

        profileService.updateSync(profile);

        return shopResult;
    }

    public ShopResultEnum buyRename(int teamMemberId, @NotNull String name, MoneyType moneyType, UserProfile profile) {
        if(restrictionService.isRestricted(profile.getProfileId(), RestrictionItem.BlockFlag.RENAME)){
            log.error("на переименование наложен запрет");
            return ShopResultEnum.ERROR;
        }

        RenameRequirements requirements = renamePriceSettings;
        String newName = name.trim();

        if(newName.length() < requirements.getMinLength() || newName.length() > requirements.getMaxLength()) {
            log.error("некорректная длина имени: {}", newName.length());
            return ShopResultEnum.ERROR;
        }

        if(!profileService.validateRename(teamMemberId, newName, profile)) {
            return ShopResultEnum.ERROR;
        }

        if(profileService.isVipActive(profile)) {
            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.EXTRA, profile,
                    ProfileEventsService.Param.eventType, "renameByVip",
                    "teamMemberId", teamMemberId,
                    "oldName", profile.getName(),
                    "newName", newName
            );
        } else {
            ShopResultEnum shopResult = tryBuyItem(profile, requirements, ItemType.RENAME, moneyType.getType(), 1, 0,
                    "teamMemberId", teamMemberId,
                    "oldName", profile.getName(),
                    "newName", newName
            );
            if(!shopResult.isSuccess()) {
                return shopResult;
            }
        }

        if(teamMemberId == profile.getProfileId()) {
            // переименовываем себя
            profileService.setName(profile, newName);
        } else {
            // переименовываем члена команды
            profileService.setTeamMemberName(teamMemberId, newName, profile);
        }
        profileService.updateSync(profile);

        return ShopResultEnum.SUCCESS;
    }

    /**
     * Покупка нового слота
     *
     * @param newSlotIndex - индекс слота начиная с 1 (равен количеству доп.слотов после успешной покупки)
     */
    public ShopResultEnum buyGroupSlot(byte newSlotIndex, MoneyType moneyType, UserProfile profile) {
        byte alreadyHasExtraSlots = profile.getExtraGroupSlotsCount();

        if(newSlotIndex != alreadyHasExtraSlots + 1) {
            log.error("попытка купить слот [{}], а уже имеет [{}]", newSlotIndex, alreadyHasExtraSlots);
        }
        if(newSlotIndex > GroupService.MAX_EXTRA_GROUP_SLOTS) {
            log.error("игрок уже имеет {} доп.слотов - предельное количество", alreadyHasExtraSlots);
            return ShopResultEnum.ERROR;
        }

        IItemRequirements requirements = getExtraGroupSlotRequirements(newSlotIndex);
        ShopResultEnum shopResult = tryBuyItem(profile, requirements, ItemType.EXTRA_GROUP_SLOT, moneyType.getType(), 1, newSlotIndex); // логгируем номер слота как itemId

        if(!shopResult.isSuccess()) {
            return shopResult;
        }
        profile.setExtraGroupSlotsCount(newSlotIndex);

        profileService.updateSync(profile);

        return ShopResultEnum.SUCCESS;
    }

    public IItemRequirements getExtraGroupSlotRequirements(byte newSlotIndex) {
        return extraGroupSlotPriceSettings.get(newSlotIndex - 1);
    }
}
