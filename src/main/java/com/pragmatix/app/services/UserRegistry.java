package com.pragmatix.app.services;

import com.pragmatix.app.init.LevelCreator;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.persist.ProfileGlobalStructureMapKeeper;
import com.pragmatix.common.utils.VarInt;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Глобальный, постоянный кешь необходимой информации о большинстве игроков (уровнем > 1)
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.09.12 10:58
 */
@Component
public class UserRegistry implements UserRegistryI {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Map<Long, ProfileGlobalStructure> store = new ConcurrentHashMap<>();

    @Resource
    private LevelCreator levelCreator;

    //== Кэш структур
    private ProfileGlobalStructure[] abadondedStructureCache;
    private ProfileGlobalStructure[] nonabadondedStructureCache;
    //==

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Value("${comebackBonusSettings.absetDays}")
    private int absentDays;

    /**
     * сколько дней должно пройти после последнего возврата, чтобы игрока снова можно было вернуть
     */
    @Value("${comebackBonusSettings.noCallbackDays}")
    private int noCallbackDays;

    @Resource
    private BanService banService;

    @Resource
    private PersistenceService persistenceService;

    public static final String keepFileName = "UserRegistry.store";

    private Date lastRun;

    private boolean initialized = false;

    public void init() {
        abadondedStructureCache = new ProfileGlobalStructure[levelCreator.getMaxLevel()];
        nonabadondedStructureCache = new ProfileGlobalStructure[levelCreator.getMaxLevel()];

        initStructureInstanceCache();
        restore();
        initialized = true;
    }

    private void restore() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -absentDays);
        lastRun = cal.getTime();

        Map restoredMap = persistenceService.restoreObjectFromFile(Map.class, keepFileName, new ProfileGlobalStructureMapKeeper(this));
        if(restoredMap == null || restoredMap.isEmpty()) {
            final Map<Long, ProfileGlobalStructure> store = new ConcurrentHashMap<>();
            long start = System.currentTimeMillis();
            final VarInt abandondedCount = new VarInt();
            Server.sysLog.info("fill UserRegistry from DB: select profiles whose level > 1  ...");
            jdbcTemplate.query("select u.id, level, (" +
                    "last_login_time < now() - interval '" + absentDays + " days' and (last_comebacked_time < now() - interval '" + noCallbackDays + " days' or last_comebacked_time is null)" +
                    ") as isAbandonded from wormswar.user_profile u left join wormswar.user_profile_meta um on (u.id = um.profile_id) where level > 1", new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet resultSet) throws SQLException {
                        long profileId = resultSet.getLong("id");
                        boolean isAbandonded = resultSet.getBoolean("isAbandonded") && !banService.isBanned(profileId);
                        if(isAbandonded) abandondedCount.value++;

                        ProfileGlobalStructure profileGlobalStructure = getStructureInstanceForParams(resultSet.getInt("level"), isAbandonded);
                        store.put(profileId, profileGlobalStructure);
                }
            });
            this.store = store;
            Server.sysLog.info("done in {} sec.", (double) (System.currentTimeMillis() - start) / (double) 1000);
            Server.sysLog.info("processed {} profiles; abandonded is {} ", store.size(), abandondedCount);
        } else {
            store = restoredMap;
            int abandondedCount = 0;
            for(ProfileGlobalStructure profileGlobalStructure : store.values()) {
                if(profileGlobalStructure.isAbandonded)
                    abandondedCount++;
            }
            Server.sysLog.info("processed {} profiles; abandonded is {} ", store.size(), abandondedCount);
            // "поля" размером в сутки, чтобы не потерять при следующем инкрементальном обновлении тех игроков, которые по времени стали abandoned ПОСЛЕ последнего инкрементального обновления, т.е. в UserRegistry эта информация ещё не попала
            cal.add(Calendar.DAY_OF_YEAR, -1);
            lastRun = cal.getTime();
        }
        persistenceService.rename(keepFileName);
    }

    public void persistToDisk() {
        if(initialized)
            persistenceService.persistObjectToFile(store, keepFileName, new ProfileGlobalStructureMapKeeper(this));
    }

    public void incrumentUpdateFromDB() {
        if(initialized) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            final VarInt abandondedCount = new VarInt();

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -absentDays);
            Calendar lastCallBack = Calendar.getInstance();
            lastCallBack.add(Calendar.DAY_OF_YEAR, -noCallbackDays);

            Server.sysLog.info("increment update UserRegistry from DB: select profiles whose level > 1 and last_login_time in ({} - {}) and lastCallBack in [-∞ - {}) ...", sdf.format(lastRun), sdf.format(cal.getTime()), sdf.format(lastCallBack.getTime()));
            jdbcTemplate.query("SELECT u.id, level FROM wormswar.user_profile u LEFT JOIN wormswar.user_profile_meta um ON (u.id = um.profile_id) WHERE level > 1 AND last_login_time > ? AND last_login_time < ? AND (last_comebacked_time < ? or last_comebacked_time is null)", new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet resultSet) throws SQLException {
                        long profileId = resultSet.getLong("id");
                        abandondedCount.value++;

                        ProfileGlobalStructure profileGlobalStructure = getStructureInstanceForParams(resultSet.getInt("level"), true);
                        store.put(profileId, profileGlobalStructure);
                }
            }, lastRun, cal.getTime(), lastCallBack.getTime());
            Server.sysLog.info("processed {} abandonded profiles ", abandondedCount);
            lastRun = cal.getTime();
        }
    }

    @Override
    public void updateLevelAndSetAbandondedFlag(UserProfile userProfile, boolean value) {
        // храним данные только игроков с уровнем > 1
        if(userProfile.getLevel() > 1) {
            store.put(userProfile.getId(), getStructureInstanceForParams(userProfile.getLevel(), value));
        }
    }

    @Override
    public void updateLevel(UserProfile userProfile) {
        // храним данные только игроков с уровнем > 1
        if(userProfile.getLevel() > 1) {
            ProfileGlobalStructure structure = store.get(userProfile.getId());
            if(structure == null) {
                store.put(userProfile.getId(), getStructureInstanceForParams(userProfile.getLevel(), false));
            } else {
                store.put(userProfile.getId(), getStructureInstanceForParams(userProfile.getLevel(), structure.isAbandonded));
            }
        } else {
            // если уровень == 1 - удаляем из хранилища
            store.remove(userProfile.getId());
        }
    }

    @Override
    public void setAbandondedFlag(UserProfile userProfile, boolean value) {
        // храним данные игроков только с уровнем > 1
        if(userProfile.getLevel() > 1) {
            store.put(userProfile.getId(), getStructureInstanceForParams(userProfile.getLevel(), value));
        }
    }

    @Override
    public int getProfileLevel(Long profileId) {
        ProfileGlobalStructure structure = store.get(profileId);
        return structure != null ? structure.level : 1;
    }

    @Override
    public boolean isProfileAbandonded(Long profileId) {
        // если игрок так и остался на 1-ом уровне, нам он не интересен
        ProfileGlobalStructure structure = store.get(profileId);
        return structure != null && structure.isAbandonded;
    }


    public static class ProfileGlobalStructure {
        public final int level;
        public final boolean isAbandonded;

        private ProfileGlobalStructure(int level, boolean abandonded) {
            this.level = level;
            isAbandonded = abandonded;
        }

        @Override
        public String toString() {
            return "{" +
                    "level=" + level +
                    ", isAbandonded=" + isAbandonded +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;

            ProfileGlobalStructure that = (ProfileGlobalStructure) o;

            if(isAbandonded != that.isAbandonded) return false;
            if(level != that.level) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = level;
            result = 31 * result + (isAbandonded ? 1 : 0);
            return result;
        }
    }

    //== Кэш ==
    private void initStructureInstanceCache() {
        for(int i = 0; i < levelCreator.getMaxLevel(); i++) {
            abadondedStructureCache[i] = new ProfileGlobalStructure(i + 1, true);
            nonabadondedStructureCache[i] = new ProfileGlobalStructure(i + 1, false);
        }
    }

    public ProfileGlobalStructure getStructureInstanceForParams(int level, boolean isAbadonded) {
        level = Math.min(levelCreator.getMaxLevel(), level);
        level = Math.max(1, level);
        if(isAbadonded) {
            return abadondedStructureCache[level - 1];
        } else {
            return nonabadondedStructureCache[level - 1];
        }
    }

    //====================== Getters and Setters =================================================================================================================================================

    public Map<Long, ProfileGlobalStructure> getStore() {
        return store;
    }

    // только для тестов
    public void setLastRun(Date lastRun) {
        this.lastRun = lastRun;
    }
}
