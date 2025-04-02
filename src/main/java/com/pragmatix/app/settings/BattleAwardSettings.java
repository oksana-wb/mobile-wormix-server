package com.pragmatix.app.settings;

import com.google.gson.Gson;
import com.pragmatix.app.services.ProfileBonusService;
import com.pragmatix.app.services.Store;
import com.pragmatix.craft.services.CraftService;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.commons.io.FileUtils;
import org.postgresql.jdbc2.optional.SimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 11.02.13 11:32
 */
public class BattleAwardSettings {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private CraftService craftService;

    @Resource
    private Store store;

    private Map<Short, SimpleBattleSettings> awardSettingsMap;

    private Map<String, Integer> heroicMissionLevels;

    private Map<String, HeroicMission> heroicMissions;

    private Map<Integer, Integer> heroicMissionHistoryDeep;

    private Map<Integer, BossBattleWinAward> heroicMissionAwards;

    private final static String LevelsStoreKey = "BattleAwardSettings.heroicMissionLevels";

    public void init() {
        for(SimpleBattleSettings battleSettings : awardSettingsMap.values()) {
            if(battleSettings instanceof BossBattleSettings) {
                proceed(((BossBattleSettings) battleSettings).getFirstWinBattleAward());
                proceed(((BossBattleSettings) battleSettings).getNextWinBattleAward());
            }
        }

        for(BossBattleWinAward award : heroicMissionAwards.values()) {
            proceed(award);
        }

        Map<String, Double> levelsFromDB = store.load(LevelsStoreKey, Map.class);
        if(levelsFromDB != null && levelsFromDB.size() > 0) {
            Map<String, Integer> heroicMissionLevels = new HashMap<>();
            for(Map.Entry<String, Double> entry : levelsFromDB.entrySet()) {
                heroicMissionLevels.put(entry.getKey(), entry.getValue().intValue());
            }
            // сливаем конфиг из БД и xml
            for(Map.Entry<String, Integer> heroicMissionLevel : this.heroicMissionLevels.entrySet()) {
                heroicMissionLevels.putIfAbsent(heroicMissionLevel.getKey(), heroicMissionLevel.getValue());
            }
            this.heroicMissionLevels = heroicMissionLevels;
        }
        persistLevels();

        for(HeroicMission heroicMission : new ArrayList<>(heroicMissions.values())) {
            if(!heroicMissionLevels.containsKey(heroicMission.getKey())) {
                heroicMissions.remove(heroicMission.getKey());
                log.error("Difficulty level not set for heroic boss key {}", heroicMission.getKey());
            }
        }
    }

    public void persistLevels() {
        store.save(LevelsStoreKey, heroicMissionLevels);
    }

    public BossBattleWinAward getAward(int level) {
        BossBattleWinAward award = heroicMissionAwards.get(level);
        if(award == null)
            throw new IllegalArgumentException("award not found by heroic boss difficulty level [" + level + "]");

        return award;
    }

    public int getLevel(String heroicBossKey) {
        HeroicMission heroicMission = heroicMissions.get(heroicBossKey);
        if(heroicMission == null)
            throw new IllegalArgumentException("HeroicMission not found by key [" + heroicBossKey + "]");

        Integer level = heroicMissionLevels.get(heroicBossKey);
        if(level == null)
            throw new IllegalArgumentException("Difficulty level not found by heroic boss key [" + heroicBossKey + "]");

        return level;
    }

    private void proceed(BossBattleWinAward winAward) {
        String awardItemsAsString = winAward.getAwardItemsStr();
        List<AwardBackpackItem> awardItems = new ArrayList<>();
        winAward.setAwardItems(awardItems);

        profileBonusService.setAwardItems(awardItemsAsString, awardItems);

        winAward.setRareAwardMassMap(craftService.parseAwardMassString(winAward.getRareAwardMass()));
    }

    public Map<Short, SimpleBattleSettings> getAwardSettingsMap() {
        return awardSettingsMap;
    }

    public void setAwardSettingsMap(Map<Short, SimpleBattleSettings> awardSettingsMap) {
        this.awardSettingsMap = awardSettingsMap;
    }

    public Map<String, HeroicMission> getHeroicMissions() {
        return heroicMissions;
    }

    @Autowired
    public void findHeroicMissions(Collection<HeroicMission> heroicMissions) {
        this.heroicMissions = new HashMap<>();
        for(HeroicMission heroicMission : heroicMissions) {
            this.heroicMissions.put(heroicMission.getKey(), heroicMission);
        }
    }

    public Map<Integer, Integer> getHeroicMissionHistoryDeep() {
        return heroicMissionHistoryDeep;
    }

    public void setHeroicMissionHistoryDeep(Map<Integer, Integer> heroicMissionHistoryDeep) {
        this.heroicMissionHistoryDeep = heroicMissionHistoryDeep;
    }

    public Map<Integer, BossBattleWinAward> getHeroicMissionAwards() {
        return heroicMissionAwards;
    }

    public void setHeroicMissionAwards(Map<Integer, BossBattleWinAward> heroicMissionAwards) {
        this.heroicMissionAwards = heroicMissionAwards;
    }

    public Map<String, Integer> getHeroicMissionLevels() {
        return heroicMissionLevels;
    }

    public void setHeroicMissionLevels(Map<String, Integer> heroicMissionLevels) {
        this.heroicMissionLevels = heroicMissionLevels;
    }

    public static void main(String[] args) throws IOException, SQLException {
        System.out.print("enter password: ");
        Scanner scanner = new Scanner(System.in);
        String password = scanner.nextLine();
        SimpleDataSource dataSource = new SimpleDataSource();
        dataSource.setServerName("tesla.rmart.ru");
        dataSource.setDatabaseName("wormswar");
        dataSource.setUser("postgres");
        dataSource.setPassword(password);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String heroicMissionLevels = jdbcTemplate.queryForObject("SELECT value FROM wormswar.store WHERE key = ?", String.class, LevelsStoreKey);

//        String heroicMissionLevels = "{\"10_7\":3,\"10_6\":3,\"18_101\":1,\"10_2\":2,\"10_1\":2,\"18_7\":2,\"18_6\":2,\"18_4\":2,\"18_3\":0,\"18_2\":2,\"10_8\":3,\"17_14\":2,\"17_15\":2,\"17_16\":2,\"3_104\":1,\"4_10\":3,\"17_10\":3,\"17_11\":1,\"17_12\":1,\"4_14\":2,\"17_13\":1,\"16_104\":3,\"16_101\":2,\"3_101\":2,\"11_101\":3,\"3_6\":2,\"7_2\":1,\"3_7\":1,\"3_8\":1,\"17_18\":1,\"11_104\":3,\"17_19\":3,\"7_13\":0,\"7_14\":2,\"12_11\":2,\"18_9\":1,\"12_10\":2,\"1_104\":2,\"1_101\":2,\"13_4\":1,\"17_8\":2,\"6_104\":3,\"2_14\":2,\"17_7\":1,\"17_6\":2,\"13_8\":1,\"17_4\":2,\"16_18\":2,\"17_3\":1,\"6_101\":3,\"2_19\":3,\"13_6\":2,\"17_2\":1,\"17_1\":0,\"19_16\":3,\"19_18\":3,\"19_13\":3,\"11_10\":3,\"19_12\":3,\"19_15\":3,\"19_14\":3,\"2_13\":1,\"19_11\":3,\"19_10\":3,\"12_101\":2,\"12_104\":3,\"4_104\":3,\"2_1\":0,\"2_3\":0,\"6_1\":1,\"11_14\":2,\"2_6\":2,\"2_8\":2,\"11_13\":1,\"6_7\":2,\"6_8\":3,\"4_101\":3,\"17_9\":1,\"16_1\":1,\"12_4\":2,\"12_3\":1,\"12_2\":1,\"12_1\":1,\"16_9\":2,\"16_8\":3,\"17_101\":1,\"16_7\":2,\"10_14\":2,\"16_6\":3,\"10_13\":2,\"12_9\":1,\"17_104\":3,\"16_4\":3,\"12_8\":2,\"10_15\":3,\"16_3\":2,\"12_7\":2,\"16_2\":2,\"12_6\":3,\"8_14\":3,\"9_104\":3,\"104_2\":3,\"9_101\":1,\"104_7\":2,\"1_3\":0,\"104_8\":3,\"1_7\":1,\"1_8\":1,\"9_1\":0,\"13_14\":1,\"9_2\":1,\"9_3\":0,\"9_4\":2,\"9_6\":2,\"16_13\":1,\"9_7\":1,\"16_14\":2,\"9_8\":2,\"16_15\":2,\"3_10\":2,\"16_10\":3,\"16_11\":3,\"3_13\":0,\"19_101\":3,\"16_12\":3,\"3_14\":1,\"15_2\":2,\"11_6\":2,\"13_104\":2,\"15_1\":1,\"11_4\":1,\"11_3\":1,\"11_2\":1,\"11_1\":0,\"19_6\":3,\"15_9\":1,\"101_14\":2,\"15_8\":2,\"12_13\":1,\"19_4\":3,\"15_7\":3,\"12_14\":2,\"19_3\":3,\"15_6\":3,\"11_9\":1,\"19_1\":3,\"15_4\":3,\"11_8\":3,\"15_3\":1,\"11_7\":2,\"15_12\":3,\"15_13\":1,\"15_14\":2,\"101_10\":3,\"6_14\":2,\"101_13\":2,\"15_11\":1,\"101_8\":3,\"101_7\":2,\"15_101\":2,\"4_1\":1,\"1_14\":1,\"4_2\":2,\"1_13\":0,\"4_3\":2,\"104_19\":3,\"4_6\":2,\"101_2\":3,\"4_7\":2,\"4_8\":2,\"15_18\":2,\"15_104\":3,\"101_104\":3,\"8_7\":2,\"9_14\":1,\"104_10\":3,\"9_13\":0,\"18_12\":2,\"19_9\":3,\"18_14\":1,\"19_8\":3,\"18_13\":0,\"19_7\":3,\"104_14\":2,\"18_10\":3,\"9_10\":2}";

        Map<String, Integer> heroicMissionLevelsMap = ((Map<String, Double>) new Gson().fromJson(heroicMissionLevels, Map.class)).entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().intValue()));

        XStream xStream = new XStream();
        xStream.processAnnotations(new Class[]{Entry.class, Entries.class});
        List<Entry> entries = ((Entries) xStream.fromXML(FileUtils.readFileToString(new File("1\\superbosses_new.xml"), "UTF-8"))).items;

        entries.forEach(e -> heroicMissionLevelsMap.put(e.key, e.value));

        IntStream.rangeClosed(0, 3).forEach(level -> {
            Comparator<String> comparator = Comparator.comparing((String k) -> Integer.parseInt(k.split("_")[0])).thenComparing(k -> Integer.parseInt(k.split("_")[1]));

            TreeMap<String, Integer> map = new TreeMap<>(comparator);
            map. putAll(heroicMissionLevelsMap.entrySet().stream()
                    .filter(e -> e.getValue() == level)
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
                    //                <entry key="2_14"     value="2" />

            map.entrySet().forEach(e -> {
                char[] space = new char[7 - e.getKey().length()];
                Arrays.fill(space, ' ');
                System.out.println("                <entry key=\"" + e.getKey() + "\""+ new String(space)+ " value=\"" + e.getValue() + "\" />");
            });
            System.out.println();
        });
    }

    @XStreamAlias("entry")
    public static class Entry {
        @XStreamAsAttribute
        String key;
        @XStreamAsAttribute
        Integer value;
    }

    @XStreamAlias("map")
    public static class Entries {
        @XStreamImplicit
        List<Entry> items;
    }

}
