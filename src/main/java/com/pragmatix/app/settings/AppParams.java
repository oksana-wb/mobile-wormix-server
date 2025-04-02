package com.pragmatix.app.settings;

import com.pragmatix.app.common.Locale;
import com.pragmatix.app.domain.AppParamsEntity;
import com.pragmatix.app.services.DaoService;
import com.pragmatix.app.services.ProfileBonusService;
import com.pragmatix.craft.domain.Reagent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс содержит параметры приложения хрянимые в ВД
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 17.05.13 10:24
 */
@Component
public class AppParams {

    public int MAX_REAGENT_IN_BONUS = 20;

    public int MAX_WEAPON_SHOOT_IN_BONUS = 20;

    public int MAX_HOURS_STUFF_IN_BONUS = 7 * 24;

    @Resource
    private DaoService daoService;
    
    @Resource
    private ProfileBonusService profileBonusService;

    public static boolean IS_MOBILE = false;

    public static boolean IS_NOT_MOBILE() {
        return !IS_MOBILE;
    }

    public static boolean IS_MOBILE() {
        return IS_MOBILE;
    }

    public static boolean IS_TEST = false;

    public static boolean IS_NOT_TEST() {
        return !IS_TEST;
    }

    public static boolean IS_TEST() {
        return IS_TEST;
    }

    private volatile List<BonusPeriodSettings> bonusPeriodSettings;

    /**
     * варсия сервера в формате 0.0.1.0 запакованая в int
     */
    private static volatile int version;

    private String vkAuthSecret;

    public static int VERSION() {
        return version;
    }

    public void init() {
        var appParamsDao = daoService.getAppParamsDao();
        AppParamsEntity appParamsEntity = appParamsDao.selectAppParams();
        if (appParamsEntity == null) {
            createAppParamsEntity();
        }
        bonusPeriodSettings = appParamsDao.getAllList().stream()
                .peek(it -> {
                    if (it.getId() == 1) {
                        version = versionFromString(it.getAppVersion());
                        vkAuthSecret = it.getVkAuthSecret();
                    }
                })
                .map(this::fill)
                .toList();
    }

    public void createAppParamsEntity() {
        AppParamsEntity entity = new AppParamsEntity();
        entity.setId(1);

        daoService.doInTransactionWithoutResult(() -> daoService.getAppParamsDao().insert(entity));
    }
    
    public BonusPeriodSettings fill(AppParamsEntity appParams) {
        BonusPeriodSettings bonusPeriodSettings = new BonusPeriodSettings(appParams.getId());

        bonusPeriodSettings.setStartBonusDay(appParams.getStartBonusDay());
        bonusPeriodSettings.setEndBonusDay(appParams.getEndBonusDay());

        bonusPeriodSettings.setLevelMin(appParams.getLevelMin());
        bonusPeriodSettings.setLevelMax(appParams.getLevelMax());

        bonusPeriodSettings.setBonusMessage(Locale.RU, appParams.getMessage());
        bonusPeriodSettings.setBonusMessage(Locale.EN, appParams.getMessageEn());

        bonusPeriodSettings.setMoney(appParams.getBonusMoney());
        bonusPeriodSettings.setRealMoney(appParams.getBonusRealMoney());
        bonusPeriodSettings.setExactBattlesCount(appParams.getBonusBattlesCount());
        bonusPeriodSettings.setWagerWinAwardToken(appParams.getWagerToken());
        bonusPeriodSettings.setBossWinAwardToken(appParams.getBossToken());
        bonusPeriodSettings.setReagentsMassStr(appParams.getBonusReagentsMass());
        bonusPeriodSettings.setReagentsCount(appParams.getBonusReagentsCount());
        if (appParams.getKeysCount() > 0) {
            HashMap<Byte, Integer> reagents = new HashMap<>();
            reagents.put(Reagent.prize_key.getIndex(), appParams.getKeysCount());
            bonusPeriodSettings.setReagents(reagents);
        }
        if (StringUtils.isNotBlank(appParams.getReagents())) {
            Map<Byte, Integer> reagents = new HashMap<>();
            for (String s : StringUtils.split(appParams.getReagents())) {
                String[] item = s.split(":");
                Reagent reagentName = Reagent.valueOf(Integer.parseInt(item[0]));
                byte reagentIndex = reagentName.getIndex();
                reagents.put(reagentIndex, Math.min(MAX_REAGENT_IN_BONUS, Integer.parseInt(item[1])));
            }
            bonusPeriodSettings.setReagents(reagents);
        }
        if (StringUtils.isNotBlank(appParams.getWeaponShoots())) {
            String weaponShoots = Arrays.stream(StringUtils.split(appParams.getWeaponShoots()))
                    .map(s -> s.split(":"))
                    .map(ss -> String.format("%d:%d", Integer.parseInt(ss[0]), Math.min(MAX_WEAPON_SHOOT_IN_BONUS, Integer.parseInt(ss[1]))))
                    .collect(Collectors.joining(" "));

            profileBonusService.setAwardItems(weaponShoots, bonusPeriodSettings.getAwardItems());
            bonusPeriodSettings.setAwardItemsStr(weaponShoots);
        }
        if (StringUtils.isNotBlank(appParams.getTemporalItems())) {
            List<String> temporalItemsView = new ArrayList<>();
            String temporalItems = Arrays.stream(StringUtils.split(appParams.getTemporalItems()))
                    .map(s -> s.split(":"))
                    .map(ss -> {
                        int itemId = Integer.parseInt(ss[0]);
                        String duration = ss[1];
                        int hours;
                        if (duration.endsWith("d")) {
                            hours = Integer.parseInt(duration.replace("d", "")) * 24;
                        } else {
                            hours = Integer.parseInt(duration.replace("h", ""));
                            duration = hours + "h";
                        }
                        if (hours > MAX_HOURS_STUFF_IN_BONUS) {
                            duration = (MAX_HOURS_STUFF_IN_BONUS / 24) + "d";
                            hours = MAX_HOURS_STUFF_IN_BONUS;
                        }
                        temporalItemsView.add(String.format("%d:%s", itemId, duration));

                        return String.format("%d:%d", itemId, hours);
                    })
                    .collect(Collectors.joining(" "));

            profileBonusService.setAwardItems(temporalItems, bonusPeriodSettings.getAwardItems());
            bonusPeriodSettings.temporalItems = String.join(" ", temporalItemsView);
        }
        bonusPeriodSettings.setReactionRate(appParams.getReaction());

        return bonusPeriodSettings;
    }

    /**
     * пакеут версию в int
     *
     * @param appVersion версия в виде 0.0.1.0
     */
    public static int versionFromString(String appVersion) {
        String[] vers = appVersion.split("\\.");
        int result = Byte.parseByte(vers[3]);
        result = result | Byte.parseByte(vers[2]) << 8;
        result = result | Byte.parseByte(vers[1]) << 16;
        result = result | Byte.parseByte(vers[0]) << 24;
        return result;
    }

    /**
     * форматирует запакованную версию в строку вида 0.0.1.0
     *
     * @param appVersion запакованная версия
     */
    public static String versionToString(int appVersion) {
        String result = "" + (appVersion & 0x000000FF);
        result = (appVersion >>> 8 & 0x000000FF) + "." + result;
        result = (appVersion >>> 16 & 0x000000FF) + "." + result;
        result = (appVersion >>> 24 & 0x000000FF) + "." + result;
        return result;
    }

    public List<BonusPeriodSettings> getBonusPeriodSettings() {
        return bonusPeriodSettings;
    }

    public void setBonusPeriodSettings(List<BonusPeriodSettings> bonusPeriodSettings) {
        this.bonusPeriodSettings = bonusPeriodSettings;
    }

    public int getVersion() {
        return version;
    }

    public String getVersionAsString() {
        return versionToString(version);
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setAppVersion(String appVersion) {
        this.version = versionFromString(appVersion);
    }

    public String getVkAuthSecret() {
        return vkAuthSecret;
    }

    public void setVkAuthSecret(String vkAuthSecret) {
        this.vkAuthSecret = vkAuthSecret;
    }
}
