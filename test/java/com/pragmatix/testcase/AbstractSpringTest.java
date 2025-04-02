package com.pragmatix.testcase;

import com.pragmatix.achieve.services.AchieveCommandService;
import com.pragmatix.app.controllers.LoginController;
import com.pragmatix.app.init.UserProfileCreator;
import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.client.Login;
import com.pragmatix.app.messages.server.EnterAccount;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.gameapp.IGameApp;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.gameapp.sessions.Connection;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.gameapp.threads.Execution;
import com.pragmatix.gameapp.threads.ExecutionContext;
import com.pragmatix.performance.statictics.StatCollector;
import com.pragmatix.quest.QuestService;
import com.pragmatix.serialization.AppBinarySerializer;
import com.pragmatix.testcase.handlers.TestcaseSimpleMessageHandler;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import com.pragmatix.testcase.SimpleTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Random;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/beans.xml"})
public class AbstractSpringTest extends AbstractTest {

    protected String host = "127.0.0.1";

    @Value("${connection.main.port}")
    protected int portMain;

    @Resource
    protected SoftCache softCache;

    @Resource
    protected UserProfileCreator userProfileCreator;

    @Resource
    protected ProfileService profileService;

    @Resource
    protected DailyRegistry dailyRegistry;

    @Resource
    protected IGameApp gameApp;

    @Resource
    protected LoginController loginController;

    @Resource
    protected AppBinarySerializer binarySerializer;

    @Resource
    protected DaoService daoService;

    @Resource
    protected StatCollector statCollector;

    @Resource
    protected WeaponService weaponService;

    @Resource
    protected StuffService stuffService;

    @Resource
    protected AppParams appParams;

    @Resource
    protected TransactionTemplate transactionTemplate;

    @Resource
    protected JdbcTemplate jdbcTemplate;

    @Resource
    protected CraftService craftService;

    @Resource
    protected TestService testService;

    @Resource
    protected AchieveCommandService achieveService;

    @Resource
    protected QuestService questService;

    @Resource
    protected BattleService battleService;

    protected EnterAccount enterAccount;

    protected String sessionId;

    protected boolean startServer = true;

    protected SocketClientConnection mainConnection;

    @Before
    public void init() throws InterruptedException {
        if(gameApp.getSessionService() == null && startServer) {
            // запускаем приложение
            gameApp.start();

            System.out.println("Server started...");
        }
    }

    // создаем нового игрока
    protected UserProfile createUserProfile() {
        long id = AppUtils.generateRandom(100000);
        UserProfile userProfile = softCache.get(UserProfile.class, id);

        // если это первый вход данного игрока, то создаем новый профайл
        if(userProfile == null) {
            userProfile = userProfileCreator.createUserProfile(id);
            //кешируем объект
            softCache.put(UserProfile.class, id, userProfile);
        }
        userProfile.setMoney(Integer.MAX_VALUE);
        return userProfile;
    }

    protected UserProfile getProfile(int profileId) {
        return getProfile((long) profileId);
    }

    protected UserProfile getProfile(Long profileId) {
        UserProfile userProfile = softCache.get(UserProfile.class, profileId);
        // если это первый вход данного игрока, то создаем новый профайл
        if(userProfile == null) {
            userProfile = userProfileCreator.createUserProfile(profileId);
            //кешируем объект
            softCache.put(UserProfile.class, profileId, userProfile);
        }
        return userProfile;
    }

    public void sendMain(Object message) throws InterruptedException {
        mainConnection.send(message);
    }

    public void sendMain(Object message, long delay) throws InterruptedException {
        mainConnection.send(message);
        Thread.sleep(delay);
    }

    public <T> T requestMain(Object message, Class<T> cmdClass) throws InterruptedException {
        return requestMain(message, cmdClass, 300);
    }

    public <T> T requestMain(Object message, Class<T> cmdClass, long delay) throws InterruptedException {
        mainConnection.send(message);
        return mainConnection.receive(cmdClass, delay);
    }

    public <T> T receiveMain(Class<T> cmdClass, long delay) throws InterruptedException {
        return mainConnection.receive(cmdClass, delay);
    }

    public <T> T receiveMain(Class<T> cmdClass) throws InterruptedException {
        return mainConnection.receive(cmdClass, 300);
    }

    public void disconnectMain() throws InterruptedException {
        mainConnection.disconnect();
        Thread.sleep(100);
    }

    public void loginMain(long profileId, Long... ids) throws Exception {
        connectMain();

        Login message = new Login();
        message.socialNet = SocialServiceEnum.vkontakte;
        message.id = profileId;
        message.ids = Arrays.asList(ids);
        message.authKey = SimpleTest.MASTER_AUTH_KEY();
        message.version = appParams.getVersion();
        message.params = new String[]{
                ILogin.FLASH_VERSION_PARAM_NAME, "" + new Random().nextInt(100),
                ILogin.REFERRER_PARAM_NAME, "1" + new Random().nextInt(10)
        };

        mainConnection.send(message);

        enterAccount = mainConnection.receive(EnterAccount.class, 1500);
        sessionId = enterAccount.sessionKey;
    }

    protected void connectMain() throws Exception {
        mainConnection = new SocketClientConnection(binarySerializer).connect(host, portMain);
    }

    public void loginMain() throws Exception {
        loginMain(testerProfileId, testerProfileId - 1, testerProfileId - 2, testerProfileId - 3, testerProfileId - 4);
    }

    protected void setExecutionContext(final TestcaseSimpleMessageHandler msgHandler) {
        ExecutionContext context = new ExecutionContext(gameApp) {
            @Override
            public void sendMessage(Object message, Connection connection) {
                msgHandler.messageReceived(message);
            }
        };
        Execution.EXECUTION.set(context);
    }

    protected void inTransaction(Runnable r){
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
               r.run();
            }
        });
    }
}
