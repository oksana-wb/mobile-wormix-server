package com.pragmatix.app.achieve;

import com.pragmatix.achieve.common.BonusItem;
import com.pragmatix.achieve.common.GrantAwardResultEnum;
import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.common.ItemCheck;
import com.pragmatix.app.messages.structures.AchieveAwardStructure;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.app.model.IItem;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.common.utils.VarInt;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.07.11 11:52
 */
@Component
public class AchieveAwardService {

    private Logger log = LoggerFactory.getLogger(AchieveAwardService.class);

    @Resource
    private WeaponService weaponService;

    @Resource
    private StuffService stuffService;

    @Resource
    private StatisticService statisticService;

    @Resource
    private ProfileService profileService;

    @Resource
    private ProfileBonusService profileBonusService;

    @Value("#{achieveBonusItemsMap}")
    private Map<Integer, BonusItem> achieveBonusItems;

    public int countBonusItemInBackpack(UserProfile profile) {
        final VarInt profilesBonusItemsCount = new VarInt();
        for(BackpackItem backpackItem : profile.getBackpack()) {
            BonusItem bonusItem = achieveBonusItems.get(backpackItem.getWeaponId());
            if(bonusItem != null && weaponService.isPresentInfinitely(backpackItem)) {
                profilesBonusItemsCount.value++;
                while (bonusItem != null && bonusItem.replaces > 0) {
                    profilesBonusItemsCount.value++;
                    bonusItem = achieveBonusItems.get(bonusItem.replaces);
                }
            }
        }
        for(short staffId : profile.getStuff()) {
            BonusItem bonusItem = achieveBonusItems.get((int) staffId);
            if(bonusItem != null) {
                profilesBonusItemsCount.value++;
                while (bonusItem != null && bonusItem.replaces > 0) {
                    profilesBonusItemsCount.value++;
                    bonusItem = achieveBonusItems.get(bonusItem.replaces);
                }
            }
        }
        return profilesBonusItemsCount.value;
    }

    public Tuple2<GrantAwardResultEnum, List<GenericAwardStructure>> grantAwards(UserProfile profile, List<AchieveAwardStructure> rawAwards, long timeSequence) {
        List<GenericAwardStructure> awardsResult = new ArrayList<>();
        try {
            int[] grantedAchieveAwards = profile.getGrantedAchieveAwards();
            List<AchieveAwardStructure> awards = new ArrayList<>();
            for(AchieveAwardStructure award : rawAwards) {
                // проверяем не выдавался ли уже приз за взятие этой ачивки
                if(!ArrayUtils.contains(grantedAchieveAwards, award.awardType)) {
                    awards.add(award);
                } else {
                    log.error("[{}] награда за достижение [{}] была выдана ранее!", profile, award.awardType);
                }
            }
            // призы за эти ачивки уже были выданы ранее
            if(awards.size() == 0) {
                return Tuple.of(GrantAwardResultEnum.ERROR, awardsResult);
            }
            // валидируем выдаваемые предметы
            for(AchieveAwardStructure award : awards) {
                // проверяем id оружия или шапки
                if(award.itemId > 0) {
                    if(!checkItemId(award.itemId))
                        return Tuple.of(GrantAwardResultEnum.ITEM_NOT_FOUND, awardsResult);
                    BonusItem bonusItem = achieveBonusItems.get(award.itemId);
                    if(bonusItem == null)
                        return Tuple.of(GrantAwardResultEnum.ITEM_NOT_FOUND, awardsResult);
                    if(bonusItem.requires > 0 && !isItemPresent(profile, bonusItem.requires)) {
                        return Tuple.of(GrantAwardResultEnum.MIN_REQUIREMENTS_ERROR, awardsResult);
                    }

                    for(BonusItem item : achieveBonusItems.values()) {
                        // попытка выбора предмета, который был замещен ранее
                        if(item.replaces == award.itemId && isItemPresent(profile, item.itemId)) {
                            log.error("[{}] попытка выбора предмета [{}], который был замещен ранее! {}", profile, award.itemId, item);
                            return Tuple.of(GrantAwardResultEnum.ERROR, awardsResult);
                        }
                    }
                }
            }

            boolean needUpdate = false;
            for(AchieveAwardStructure award : awards) {
                int replacedItem = 0;
                GenericAward genericAward = new GenericAward();
                // выдаём деньги или реакцию
                genericAward.setMoney(award.money);
                genericAward.setRealMoney(award.realmoney);
                genericAward.setReactionRate(award.reaction);

                // выдать оружие или шапку
                int itemId = award.itemId;
                if(itemId > 0) {
                    int replaces = achieveBonusItems.get(itemId).replaces;
                    if(ItemCheck.isWeapon(itemId)) {
                        genericAward.addWeapon(itemId, -1);
                        if(replaces > 0 && weaponService.isPresentInfinitely(profile, replaces)) {
                            weaponService.removeWeapon(profile, replaces);
                            replacedItem = replaces;
                        }
                    } else {
                        genericAward.addStuff((short) itemId).setSetItem(true);
                        if(replaces > 0) {
                            stuffService.removeStuff(profile, (short) replaces);
                            replacedItem = replaces;
                        }
                    }
                }

                // запоминаем выдычу приза за данную ачивку
                if(grantedAchieveAwards == null) {
                    profile.setGrantedAchieveAwards(new int[]{award.awardType});
                } else {
                    profile.setGrantedAchieveAwards(ArrayUtils.add(grantedAchieveAwards, award.awardType));
                }

                awardsResult.addAll(profileBonusService.awardProfile(genericAward, profile, AwardTypeEnum.ACHIEVE,
                        "achieveAwardType", award.awardType,
                        "timeSequence", timeSequence,
                        "achievementIndex", award.achievementIndex == -1 ? "" : WormixAchievements.AchievementName.valueOf(award.achievementIndex).toString(),
                        "achievementProgress", award.achievementProgress,
                        "boolAchieveIndex", award.boolAchieveIndex == -1 ? "" : "" + award.boolAchieveIndex,
                        "replacedItem", replacedItem
                ));

                //логируем в базу выдачу рубина или оружия
                if(award.realmoney > 0 || award.itemId > 0) {
                    statisticService.awardStatistic(profile.getId(), award.money, award.realmoney, award.itemId, award.awardType, "");
                    // сохраним профиль в базе, чтобы минимизировать расхождение с сервером достижений в случае сбоя
                    needUpdate = true;
                }
            }

            if(!profile.isOnline() || needUpdate) {
                profileService.updateAsync(profile);
            }
            return Tuple.of(GrantAwardResultEnum.OK, awardsResult);
        } catch (Exception e) {
            log.error("[" + profile + "] " + e.toString(), e);
            return Tuple.of(GrantAwardResultEnum.ERROR, awardsResult);
        }
    }

    private boolean isItemPresent(UserProfile profile, int itemId) {
        if(ItemCheck.isWeapon(itemId)) return weaponService.isPresentInfinitely(profile, itemId);
        else return stuffService.isExistPermanent(profile, (short) itemId);
    }

    private boolean checkItemId(int itemId) {
        IItem item;
        //если id < 1000, то производется покупка оружия
        if(ItemCheck.isWeapon(itemId)) {
            item = weaponService.getWeapon(itemId);
        } else {
            //приводим к short тк в кеше предметы храним по ключу объекта типа Short
            item = stuffService.getStuff(itemId);
        }

        if(item == null) {
            log.error("can't get item by id: {}, from cache", itemId);
            return false;
        }
        return true;
    }

}
