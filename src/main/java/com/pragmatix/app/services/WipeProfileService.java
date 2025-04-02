package com.pragmatix.app.services;

import com.pragmatix.app.common.ItemType;
import com.pragmatix.app.init.UserProfileCreator;
import com.pragmatix.app.messages.client.SendWipeConfirmCode;
import com.pragmatix.app.messages.client.WipeProfile;
import com.pragmatix.app.messages.server.SendWipeConfirmCodeResult;
import com.pragmatix.app.messages.server.WipeProfileResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.settings.ItemRequirements;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.services.DailyTaskAvailable;
import com.pragmatix.gameapp.services.IServiceTask;
import com.pragmatix.gameapp.sessions.Connection;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.intercom.service.AchieveServerAPI;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.Set;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 13.07.11 17:04
 */
@Controller
public class WipeProfileService implements DailyTaskAvailable {

    private static final Logger logger = LoggerFactory.getLogger(WipeProfileService.class);

    @Resource
    private UserProfileCreator profileCreator;

    @Resource
    private ProfileService profileService;

    @Resource
    private StatisticService statisticService;

    @Resource
    @Qualifier(value = "wipePriceSettings")
    private ItemRequirements wipePriceSettings;

    @Resource
    private ShopService shopService;

    @Resource
    private AchieveServerAPI achieveServerAPI;

    /**
     * скольким игрокам в день мы можем себе позволить отправить уведомления  с кодом подтверждения
     */
    private int maxNotificationsByDay = 5000;
    /**
     * кому мы сегодня уже отправляли уведомления с кодом
     */
    private Set<Long> wipeNotifications = new ConcurrentHashSet<>();

    public static final String WIPE_PROFILE_CONFIRM_CODE = "wipeProfileConfirmCode";

    private int confirmCodeLength = 4;

    @Value("${social.confirmWipeProfile:}")
    private String confirmMessagePattern;

    @Value("${debug.verifyWiping:true}")
    private boolean debugVerifyWiping = true;

    @Resource
    private SocialService socialService;

    private boolean initialized = false;

    @Override
    public void init() {
        initialized = true;
    }

    // очищаем кэш отосланный нотификаций
    private IServiceTask dailyTask = new IServiceTask() {
        @Override
        public void runServiceTask() {
            wipeNotifications = new ConcurrentHashSet<Long>();
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    };

    public IServiceTask getDailyTask() {
        return dailyTask;
    }

    public SendWipeConfirmCodeResult sendWipeConfirmCode(SendWipeConfirmCode msg, UserProfile profile) {

        if(!debugVerifyWiping) {
            return new SendWipeConfirmCodeResult(SendWipeConfirmCodeResult.Result.OK);
        }

        if(wipeNotifications.size() > maxNotificationsByDay) {
            return new SendWipeConfirmCodeResult(SendWipeConfirmCodeResult.Result.DAILY_LIMIT_EXCEEDED);
        }
        if(wipeNotifications.contains(profile.getId())) {
            return new SendWipeConfirmCodeResult(SendWipeConfirmCodeResult.Result.TODAY_ALREADY_WIPED);
        }

        String code = RandomStringUtils.randomNumeric(confirmCodeLength);
        Connection connection = Connections.get();
        if(connection == null) {
            logger.error("Connection is null!");
            return new SendWipeConfirmCodeResult(SendWipeConfirmCodeResult.Result.ERROR);
        }

        boolean sendResult = socialService.sendMessage(profile, confirmMessagePattern.replaceFirst("#confirmCode#", code));
        if(!sendResult) {
            return new SendWipeConfirmCodeResult(SendWipeConfirmCodeResult.Result.SEND_MESSAGE_ERROR);
        }

        wipeNotifications.add(profile.getId());

        logger.info("Store wipeProfileConfirmCode [{}] in connection's store", code);

        connection.getStore().put(WIPE_PROFILE_CONFIRM_CODE, code);

        return new SendWipeConfirmCodeResult(SendWipeConfirmCodeResult.Result.OK);
    }

    public boolean validateConfirmCode(String confirmCode) {
        Connection connection = Connections.get();
        if(connection == null) {
            logger.error("Connection is null!");
            return false;
        }

        String code = (String) connection.getStore().get(WIPE_PROFILE_CONFIRM_CODE);
        if(confirmCode == null || code == null || code.length() != 4) {
            return false;
        }
        return confirmCode.equals(code);
    }

    public WipeProfileResult validateAndWipe(WipeProfile msg, UserProfile profile) {
        if(!validateConfirmCode(msg.confirmCode)) {
            return new WipeProfileResult(ShopResultEnum.CONFIRM_FAILURE);
        }

        try {
            ShopResultEnum shopResult = shopService.checkPossibilityOfBuyingItem(profile, wipePriceSettings, MoneyType.REAL_MONEY, 1);
            if(!shopResult.isSuccess() && debugVerifyWiping) {
                return new WipeProfileResult(shopResult);
            }

            shopService.tryBuyItem(profile, wipePriceSettings, ItemType.WIPE, MoneyType.REAL_MONEY.getType(), 1, 0);

            statisticService.wipeStatistic(profile, "", null);

            achieveServerAPI.wipeAchievements(profileService.getProfileSocialId(profile));
            profileCreator.wipeUserProfile(profile);

            // клиент при получении данного сообщения должен заставить игрока обновить приложение
            return new WipeProfileResult(ShopResultEnum.SUCCESS);
        } catch (Exception ex) {
            logger.error("WipeProfile ERROR: ", ex);
            return new WipeProfileResult(ShopResultEnum.ERROR);
        }
    }


    public void wipeAndSendResponse(UserProfile profile) {
        try {
            achieveServerAPI.wipeAchievements(profileService.getProfileSocialId(profile));
            statisticService.wipeStatistic(profile, "", null);
            profileCreator.wipeUserProfile(profile);
        } catch (Exception ex) {
            logger.error("WipeProfile ERROR: ", ex);
        } finally {
            Messages.toUser(new WipeProfileResult(ShopResultEnum.SUCCESS), profile);
        }
    }

    //====================== Getters and Setters =================================================================================================================================================

    public void setMaxNotificationsByDay(int maxNotificationsByDay) {
        this.maxNotificationsByDay = maxNotificationsByDay;
    }

    public void setConfirmCodeLength(int confirmCodeLength) {
        this.confirmCodeLength = confirmCodeLength;
    }

    public void setConfirmMessagePattern(String confirmMessagePattern) {
        this.confirmMessagePattern = confirmMessagePattern;
    }

}
