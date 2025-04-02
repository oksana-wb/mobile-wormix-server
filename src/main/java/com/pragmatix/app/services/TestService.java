package com.pragmatix.app.services;

import com.pragmatix.achieve.common.BonusItem;
import com.pragmatix.app.common.ItemCheck;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.dao.BackpackItemDao;
import com.pragmatix.app.domain.BackpackItemEntity;
import com.pragmatix.app.domain.WipeStatisticEntity;
import com.pragmatix.app.init.UserProfileCreator;
import com.pragmatix.app.init.WeaponsCreator;
import com.pragmatix.app.messages.client.WipeProfile;
import com.pragmatix.app.messages.server.WipeProfileResult;
import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.rating.SeasonService;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.intercom.service.AchieveServerAPI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 * Возможность тестеру управлять своим профилем
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 13.12.2016 11:04
 */
@Service
public class TestService {

    @Resource
    private UserRegistry userRegistry;

    @Resource
    private SearchTheHouseService searchTheHouseService;

    @Resource
    private UserProfileCreator userProfileCreator;

    @Resource
    private WeaponsCreator weaponsCreator;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private WeaponService weaponService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private BackpackItemDao backpackItemDao;

    @Resource
    private Optional<SeasonService> seasonService;

    @Resource
    private StuffService stuffService;

    @Resource
    private StatisticService statisticService;

    @Resource
    private AchieveServerAPI achieveServerAPI;

    @Resource
    private ProfileService profileService;

    @Resource
    private CloneProfileService cloneProfileService;

    @Value("#{achieveBonusItemsMap}")
    private Map<Integer, BonusItem> achieveBonusItems;

    public WipeProfileResult applyTestWipeCode(WipeProfile msg, UserProfile profile) {
        String[] ss = msg.confirmCode.split(" ");
        if(ss.length >= 3){
            if(ss[0].equalsIgnoreCase("clone")){
                boolean banSourceProfile = ss.length > 3 ? Boolean.valueOf(ss[3]) : false;
                boolean result = cloneProfileService.cloneProfile(profile, SocialServiceEnum.valueOf(Integer.valueOf(ss[1])), Long.parseLong(ss[2]), banSourceProfile, cloneProfileService.masterSecureToken);
                return new WipeProfileResult(result ? ShopResultEnum.SUCCESS : ShopResultEnum.ERROR);
            }
            return new WipeProfileResult(ShopResultEnum.ERROR);
        }
        if(msg.confirmCode.length() != 4) {
            return new WipeProfileResult(ShopResultEnum.ERROR);
        }
        try {
            short stuffId = Short.parseShort(msg.confirmCode);
            Stuff stuff = stuffService.getStuff(stuffId);
            if(stuff != null) {
                if(stuff.isTemporal()) {
                    stuffService.addStuff(profile, stuffId, 3, TimeUnit.HOURS, true);
                } else {
                    stuffService.addStuff(profile, stuffId);
                }
                return new WipeProfileResult(ShopResultEnum.SUCCESS);
            }
        } catch (NumberFormatException e) {
        }

        String cmd = msg.confirmCode.substring(0, 2);
        int param = Integer.parseInt(msg.confirmCode.substring(2, 4));

        WipeStatisticEntity wipeStatisticEntity = statisticService.getWipeStatisticEntity(profile, msg.confirmCode, null);

        if(cmd.equals("00")) {
            if(param > 0 && param <= 30) {
                Connections.get().getStore().put("wipeProfile", "1");
                // Обнуление с базовыми параметрами и заданным уровнем
                wipeProfileWithLevel(profile, param);
            } else if(param == 41) {
                // Забрать все шапки
                cleanHats(profile);
            } else if(param == 42) {
                // Забрать все артефакты
                cleanKits(profile);
            } else if(param == 43) {
                // Обнулить оружие и апгрейды, выдав базовое
                cleanBackpack(profile);
            } else if(param == 44) {
                // Сбросить все апгреды
                profile.setRecipes(new short[0]);
            } else if(param == 45) {
                // Обнулить прохождение всех боссов
                profile.setCurrentMission((short) 0);
                profile.setCurrentNewMission((short) 0);
                dailyRegistry.clearMission(profile.getId());
            } else {
                return new WipeProfileResult(ShopResultEnum.ERROR);
            }
        } else if(cmd.equals("01") && param > 0 && param <= 30) {
            // Обнуление с тестовыми параметрами и заданным уровнем
            wipeProfileWithLevel(profile, param);
        } else if(cmd.equals("90")) {
            if(param > 0 && param <= 30) {
                // Установить уровень
                setLevel(profile, param);
            } else if(param == 55) {
                // Выдать все оружие, которое можно купить в магазине, кроме разового
                giveAllWeapons(profile);
            } else if(param == 56) {
                // Выдать все сезонное оружие текущего сезона
                giveCurrentSeasonWeapons(profile);
            } else if(param == 57) {
                // Выдать все сезонное оружие следующего сезона
                giveNextSeasonWeapons(profile);
            } else if(param == 58) {
                // Выдать все сезонное прошедшего следующего сезона
                givePrevSeasonWeapons(profile);
            } else if(param == 77) {
                // Выдать все награды за достижения
                giveBonusAchieveItems(profile);
            } else if(param == 90) {
                // Назначить 9000 рейтинга
                profile.setRating(9000);
            } else {
                return new WipeProfileResult(ShopResultEnum.ERROR);
            }
        } else {
            return new WipeProfileResult(ShopResultEnum.ERROR);
        }

        statisticService.insertWipeStatisticEntity(wipeStatisticEntity);

        return new WipeProfileResult(ShopResultEnum.SUCCESS);
    }

    private void cleanHats(UserProfile profile) {
        Set<Short> stuffSet = getStuffSet(profile);

        for(Short stuffId : stuffSet) {
            Stuff stuff = stuffService.getStuff(stuffId);
            if(stuff != null && stuff.isHat()) {
                stuffService.removeStuff(profile, stuffId);
            }
        }
    }

    private void cleanKits(UserProfile profile) {
        Set<Short> stuffSet = getStuffSet(profile);

        for(Short stuffId : stuffSet) {
            Stuff stuff = stuffService.getStuff(stuffId);
            if(stuff != null && stuff.isKit()) {
                stuffService.removeStuff(profile, stuffId);
            }
        }
    }

    private Set<Short> getStuffSet(UserProfile profile) {
        Set<Short> stuffSet = new HashSet<>();
        for(short stuffId : profile.getStuff()) {
            stuffSet.add(stuffId);
        }

        byte[] temporalStuff = profile.getTemporalStuff();
        for(int i = 0; i < temporalStuff.length; i += TemporalStuffService.TEMP_STUF_SIZE) {
            short stuffId = TemporalStuffService.readStuffId(temporalStuff, i);
            stuffSet.add(stuffId);
        }
        return stuffSet;
    }

    public void cleanBackpack(final UserProfile profile) {
        profile.setRecipes(new short[0]);
        weaponService.wipeBackpackConf(profile);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                backpackItemDao.deleteBackpack(profile.getId());
            }
        });

        // заполняем оружием выдаваеиым на старте
        List<BackpackItemEntity> backpack = userProfileCreator.createDefaultBackpack(profile.getId());
        profile.setBackpack(userProfileCreator.initBackpack(backpack));
        profile.setUserProfileStructure(null);
    }

    private void wipeProfileWithLevel(UserProfile profile, int level) {
        // на тесте получаем уровень игрока из диалога подтверждения
        Connections.get().getStore().put("wipeProfileLevel", level);
        achieveServerAPI.wipeAchievements(profileService.getProfileSocialId(profile));
        userProfileCreator.wipeUserProfile(profile);
    }

    private void giveAllWeapons(UserProfile profile) {
        weaponsCreator.getWeapons().stream()
                .filter(weapon -> weapon.isInfiniteOrComplex() && ItemCheck.hasRealPrice(weapon) && ItemCheck.hasPrice(weapon))
                .forEach(weapon -> awardItem(profile, weapon.getWeaponId()));
    }

    public void giveCurrentSeasonWeapons(UserProfile profile) {
        seasonService.ifPresent(seasonService ->
                seasonService.getCurrentSeasonWeapons().forEach(weaponId -> weaponService.addOrUpdateWeapon(profile, weaponId, 25))
        );
    }

    private void givePrevSeasonWeapons(UserProfile profile) {
        seasonService.ifPresent(seasonService -> {
            Set<Integer> seasonWeapons = seasonService.getPrevSeasonWeapons();
            giveSeasonWeapons(profile, seasonWeapons);
        });
    }

    private void giveNextSeasonWeapons(UserProfile profile) {
        seasonService.ifPresent(seasonService -> {
            Set<Integer> seasonWeapons = seasonService.getNextSeasonWeapons();
            giveSeasonWeapons(profile, seasonWeapons);
        });
    }

    private void giveSeasonWeapons(UserProfile profile, Set<Integer> seasonWeapons) {
        seasonWeapons.forEach(weaponId -> {
            BackpackItem item = profile.getBackpackItemByWeaponId(weaponId);
            // если данный выстрел уже есть, то обновляем его
            if(item != null) {
                profile.setBackpackItemCount(weaponId, 25);
            } else {
                // иначе создаем новое оружие
                profile.addBackpackItem(new BackpackItem(weaponId, 25, true));
            }
        });
    }

    private void giveBonusAchieveItems(UserProfile profile) {
        for(Integer itemId : achieveBonusItems.keySet()) {
            // выдать оружие или шапку
            awardItem(profile, itemId);
        }
    }

    protected void setLevel(UserProfile profile, int level) {
        profile.setLevel(level);
        profile.setArmor(level);
        profile.setAttack(level);
        profile.setExperience(0);
        //обновляем уровень игрока в глобальном кеше уровней
        userRegistry.updateLevel(profile);
        // даем возможность заработать на обыске, если достигнут "зачетный" уровень
        searchTheHouseService.fireLevelUp(profile);
    }

    private void awardItem(UserProfile profile, int itemId) {
        if(itemId > 0) {
            if(ItemCheck.isWeapon(itemId)) {
                weaponService.addOrUpdateWeapon(profile, itemId, -1);
            } else {
                stuffService.addStuff(profile, (short) itemId);
            }
        }
    }


}
