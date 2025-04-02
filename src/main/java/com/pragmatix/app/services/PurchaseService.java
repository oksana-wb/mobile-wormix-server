package com.pragmatix.app.services;

import com.pragmatix.app.common.*;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.settings.GenericAward;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.11.12 14:50
 */
@Component
public class PurchaseService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ProfileService profileService;

    private int resetBonusItemsRealPrice = 10;

    @Value("#{achieveBonusItemsMap.keySet()}")
    private Set<Integer> achieveBonusItems;

    @Resource
    private WeaponService weaponService;

    @Resource
    StuffService stuffService;

    @Resource
    private StatisticService statisticService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private ProfileBonusService profileBonusService;

    private double sellStuffFactor = 0.3;

    public ShopResultEnum buyResetBonusItems(String profileId) {
        try {
            final UserProfile profile = profileService.getUserProfile(profileId);
            if(profile == null) {
                log.warn("UserProfile not found by id {}", profileId);
                return ShopResultEnum.ERROR;
            }

            boolean vipActive = profileService.isVipActive(profile);
            int needRealMoney =  vipActive ? 0 : resetBonusItemsRealPrice;
            // если недостаточно денег для совершения покупки
            if(profile.getRealMoney() < needRealMoney) {
                log.error("can't reset bonus items not enough realmoney: {}", profile.getRealMoney());
                return ShopResultEnum.NOT_ENOUGH_MONEY;
            }

            List<Integer> removedItems = removeBonusItems(profile);

            if(removedItems.size() == 0) {
                log.warn("отсутствуют призовые предметы");
                return ShopResultEnum.ERROR;
            }

            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PURCHASE, profile,
                    Param.eventType, ItemType.RESET_BONUS_ITEMS,
                    Param.realMoney, -needRealMoney,
                    Param.vipExpireDate, vipActive ? profileService.getVipExpireDate(profile) : "",
                    "removedItems", removedItems
            );
            if(needRealMoney > 0) {
                statisticService.buyItemStatistic(profile.getId(), MoneyType.REAL_MONEY.getType(), needRealMoney, ItemType.RESET_BONUS_ITEMS, removedItems.size(), 0, profile.getLevel());
                profile.setRealMoney(profile.getRealMoney() - needRealMoney);
            }

            profile.cleanGrantedAchieveAwards();

            return ShopResultEnum.SUCCESS;
        } catch (Exception e) {
            log.error(e.toString(), e);
            return ShopResultEnum.ERROR;
        }
    }

    public List<Integer> removeBonusItems(UserProfile profile) {
        List<Integer> removedItems = new ArrayList<>();
        for(Integer achieveBonusItemId : achieveBonusItems) {
            if(ItemCheck.isWeapon(achieveBonusItemId)) {
                if(weaponService.isPresentInfinitely(profile, achieveBonusItemId) && weaponService.removeWeapon(profile, achieveBonusItemId)) {
                    removedItems.add(achieveBonusItemId);
                    if(log.isDebugEnabled()) {
                        log.debug("удаляем из рюкзака призовое оружие [{}]", achieveBonusItemId);
                    }
                }
            } else {
                if(stuffService.removeStuff(profile, achieveBonusItemId.shortValue())) {
                    removedItems.add(achieveBonusItemId);
                    if(log.isDebugEnabled()) {
                        log.debug("забираем призовую шапку [{}]", achieveBonusItemId);
                    }
                }
            }
        }
        return removedItems;
    }

    public void setResetBonusItemsRealPrice(int resetBonusItemsRealPrice) {
        this.resetBonusItemsRealPrice = resetBonusItemsRealPrice;
    }

    public List<GenericAwardStructure> sellStuff(UserProfile profile, short stuffId) {
        Stuff stuff = stuffService.getStuff(stuffId, false);
        if(stuff == null) {
            log.error("Предмет для продажи не найден! [{}]", stuffId);
            return Collections.emptyList();
        }
        if(stuff.isTemporal()) {
            log.error("Предмет [{}] для продажи должен быть постоянным!", stuff);
            return Collections.emptyList();
        }
        int priceInMoney = 0;
        if(ItemCheck.hasPrice(stuff)) {
            priceInMoney = stuff.getPrice();
        } else if(ItemCheck.hasRealPrice(stuff)) {
            priceInMoney = stuff.getRealprice() * 100;
        }
        if(priceInMoney == 0) {
            log.error("Предмет [{}] не продается!", stuff);
            return Collections.emptyList();
        }
        if(!stuffService.isExist(profile, stuffId, false)
                || !stuffService.removeStuff(profile, stuffId)) {
            log.error("Предмет для продажи [{}] отсутствует!", stuff);
            return Collections.emptyList();
        }

        int p = (int) Math.round(priceInMoney * sellStuffFactor);
        int mod = p % 100;
        int awardMoney = p / 100 * 100 + (mod < 50 ? 0 : 100);
        GenericAward award = GenericAward.builder().addMoney(awardMoney).build();
        List<GenericAwardStructure> result = profileBonusService.awardProfile(award, profile, AwardTypeEnum.SELL_STUFF,
                Param.itemId, stuffId
        );
        profileService.updateSync(profile);

        return result;
    }
}
