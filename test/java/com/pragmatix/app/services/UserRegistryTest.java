package com.pragmatix.app.services;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.util.Calendar;

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.09.12 12:52
 */
public class UserRegistryTest extends AbstractSpringTest {

    @Resource
    private UserRegistry userRegistry;

    @Resource
    private DaoService daoService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private PersistenceService persistenceService;

    @Value("${InitController.persistStateOnExit}")
    private boolean persistStateOnExit;

    @Value("${comebackBonusSettings.absetDays}")
    private int absentDays;

    @Test
    public void testInit() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                UserProfile profile = getProfile(testerProfileId);
                profile.setLevel(2);
                daoService.getUserProfileDao().updateProfile(profile);
            }
        });
        Thread.sleep(1000);

        userRegistry.init();
        assertTrue(userRegistry.getStore().size() > 0);
        int profileLevel = userRegistry.getProfileLevel(testerProfileId);
        assertTrue(profileLevel > 1 && profileLevel < 30);

        final UserProfile profile = getProfile(testerProfileId);
        int newLevel = profileLevel + 1;
        profile.setLevel(newLevel);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                userRegistry.updateLevel(profile);
                daoService.getUserProfileDao().updateProfile(profile);
            }
        });

        fakeDaysElapsed(1);
        userRegistry.incrumentUpdateFromDB();

        assertEquals(newLevel, userRegistry.getProfileLevel(testerProfileId));
    }

    @Test
    public void testUpdateLevelAndSetAbandondedFlag() throws Exception {
        final UserProfile profile = getProfile(testerProfileId);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

                profile.setLevel(1);
                daoService.getUserProfileDao().updateProfile(profile);
            }
        });
        fakeDaysElapsed(1);
        userRegistry.init(); // <- тестим именно init, т.к. при incrumentUpdateFromDB level не пересчитывается

        assertEquals(1, userRegistry.getProfileLevel(testerProfileId));
        assertFalse(userRegistry.isProfileAbandonded(testerProfileId));

        userRegistry.updateLevelAndSetAbandondedFlag(profile, true);

        assertEquals(1, userRegistry.getProfileLevel(testerProfileId));
        assertFalse(userRegistry.isProfileAbandonded(testerProfileId));

        profile.setLevel(2);

        userRegistry.updateLevelAndSetAbandondedFlag(profile, false);

        assertEquals(2, userRegistry.getProfileLevel(testerProfileId));
        assertFalse(userRegistry.isProfileAbandonded(testerProfileId));

    }

    @Test
    public void testIsProfileAbandonded() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                UserProfile profile = getProfile(testerProfileId);
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, -2);
                profile.setLastLoginTime(cal.getTime());
                profile.setDirty(true);
                daoService.getUserProfileDao().updateProfile(profile);
                daoService.getUserProfileDao().clearMeta(profile.getId());
            }
        });
        fakeDaysElapsed(61);
        userRegistry.incrumentUpdateFromDB();

        assertTrue(userRegistry.isProfileAbandonded(testerProfileId));

        userRegistry.setAbandondedFlag(getProfile(testerProfileId), false);

        assertFalse(userRegistry.isProfileAbandonded(testerProfileId));

    }

    @Test
    public void testMissedAbandoned() throws InterruptedException {
        final UserProfile profile = getProfile(testerProfileId);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -absentDays);
        cal.add(Calendar.SECOND, 1);
        profile.setLastLoginTime(cal.getTime());
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                profile.setDirty(true);
                daoService.getUserProfileDao().updateProfile(profile);
                daoService.getUserProfileDao().clearMeta(profile.getId());
            }
        });
        // пусть есть профиль, который пока не abandoned, но станет таковым через секунду (lastLogin был 21 день назад + 1 секунда)
        userRegistry.setAbandondedFlag(profile, false);

        // если сейчас произойдет инкрементальное обновление - то он ещё пока не будет abandoned
        userRegistry.incrumentUpdateFromDB();

        // вот он уже по идее abandoned
        Thread.sleep(1000L);

        // но ещё до следующего инкрементального обновления сервер перезапустился, и кэш восстановился из файла
        String dataDir = "data";
        persistenceService.setDataDir(dataDir);
        userRegistry.persistToDisk();
        try {
            Thread.sleep(1000L);
            userRegistry.init();

            // состояние профиля восстановилось с диска: он не abandoned
            assertFalse("could be still not abandoned, it's OK", userRegistry.isProfileAbandonded(testerProfileId));

            // но при следующем инкрементальном обновлении
            userRegistry.incrumentUpdateFromDB();

            // он всё-таки должен стать abandoned
            assertTrue("must become abandoned", userRegistry.isProfileAbandonded(testerProfileId));

            // потому что иначе он уже никогда не станет abandoned
        } finally {
            if ( ! persistStateOnExit ) { // если это development-сервер
                // подчищаем за собой, чтобы последующие запуски тестов не считывали устаревшую информацию
                new File(dataDir + '/' + UserRegistry.keepFileName).delete();
            }
        }
    }

    // хак, чтобы обойти оптимизацию lastRun, которая смотрит только профили, которые стали abandoned после последнего обновления
    private void fakeDaysElapsed(int days) throws InterruptedException {
        Thread.sleep(1000);
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -(days+1));
        userRegistry.setLastRun(yesterday.getTime());
        Thread.sleep(1000);
    }
}
