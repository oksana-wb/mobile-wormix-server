package com.pragmatix.achieve.controllers;

import com.pragmatix.achieve.dao.AchieveDao;
import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.achieve.messages.client.BuyResetBonusItems;
import com.pragmatix.achieve.messages.client.ChooseBonusItem;
import com.pragmatix.achieve.messages.client.GetAchievements;
import com.pragmatix.achieve.messages.client.IncreaseAchievements;
import com.pragmatix.achieve.messages.server.BuyResetBonusItemsResult;
import com.pragmatix.achieve.messages.server.ChooseBonusItemResult;
import com.pragmatix.achieve.messages.server.GetAchievementsResult;
import com.pragmatix.achieve.messages.server.IncreaseAchievementsResult;
import com.pragmatix.achieve.services.AchieveCommandService;
import com.pragmatix.app.common.Connection;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnCloseConnection;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.security.annotations.Filter;
import com.pragmatix.gameapp.security.annotations.InMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * Контроллер событий и входящих сообщений
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 21.07.11 15:51
 */
@Controller
@Filter
public class AchieveController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private AchieveDao achievementDao;

    @Resource
    private AchieveCommandService achieveService;

    @Resource
    private ProfileService profileService;

    @OnMessage
    public GetAchievementsResult onGetAchievements(final GetAchievements msg, final UserProfile profile) {
        String profileAchieveId = profileService.getProfileAchieveId(profile.getId());
        if (!profileAchieveId.equals(msg.profileId)) {
            // TODO Это не ошибка, а запрос ачивок другого игрока
//            log.error("сервер не корректно определил profileAchieveId profileAchieveId: {} != msg.profileId: {}", profileAchieveId, msg.profileId);
        }
        ProfileAchievements profileAchievements = achieveService.getProfileAchievementsOrCreateNew(profileAchieveId, WormixAchievements.class);
        int userProfileId = profile.getId().intValue();
        if (profileAchievements.userProfileId != userProfileId) {
            profileAchievements.userProfileId = userProfileId;
            profileAchievements.setDirty(true);
        }
        return achieveService.getAchievements(msg, profileAchievements);
    }

    @InMessage(IncreaseAchievements.class)
    public boolean checkMessage(IncreaseAchievements msg, final UserProfile profile) {
        ProfileAchievements profileAchievements = achieveService.getProfileAchievements(profileService.getProfileAchieveId(profile.getId()));
        if (msg.timeSequence < profileAchievements.getTimeSequence()) {
            // проверяем чтобы в каждом сообщении увеличивался параметр timeSequence
            log.error("IncreaseAchievements in not valid cause wrong timeSequence {} <= {}", msg.timeSequence, profileAchievements.getTimeSequence());
            return false;
        }
        return true;
    }

    /**
     * Обработка обрыва соединения
     *
     * @param profileAchievements аккаунт который был отсоединён
     */
    @OnCloseConnection(connections = {Connection.ACHIEVE})
    public void onDisconnect(final ProfileAchievements profileAchievements) {
        int achievePoints = achieveService.getService(profileAchievements).countAchievePoints(profileAchievements);

        achievementDao.persist(profileAchievements, achievePoints);

        if (log.isDebugEnabled()) {
            log.debug("disconnected");
        }
    }

    @OnMessage
    public IncreaseAchievementsResult onIncreaseAchievement(final IncreaseAchievements msg, final UserProfile profile) {
        ProfileAchievements profileAchievements = achieveService.getProfileAchievements(profileService.getProfileAchieveId(profile.getId()));
        IncreaseAchievementsResult result = null;
        // по каким то причинам клиент не получил ответ, и повторно увеличивает те же достижения
        if (msg.timeSequence == profileAchievements.getTimeSequence()) {
            IncreaseAchievementsResult lastIncreaseAchievementResult = profileAchievements.getLastIncreaseAchievementsResult();
            if (lastIncreaseAchievementResult != null && lastIncreaseAchievementResult.timeSequence == msg.timeSequence) {
                result = lastIncreaseAchievementResult;
            }
        } else if (msg.timeSequence == profileAchievements.getTimeSequence() + 1) {
            result = achieveService.increaseAchievements(msg, profileAchievements);
            if (result != null)
                profileAchievements.setTimeSequence(profileAchievements.getTimeSequence() + 1);
        }
        return result;
    }

    @OnMessage
    public ChooseBonusItemResult onChooseBonusItem(final ChooseBonusItem msg, final UserProfile profile) {
        ProfileAchievements profileAchievements = achieveService.getProfileAchievements(profileService.getProfileAchieveId(profile.getId()));
        ShopResultEnum shopResultEnum = achieveService.giveBonusItem(msg, profileAchievements);
        return new ChooseBonusItemResult(msg.itemId, shopResultEnum);
    }

    @OnMessage
    public BuyResetBonusItemsResult onBuyResetBonusItems(final BuyResetBonusItems msg, final UserProfile profile) {
        ProfileAchievements profileAchievements = achieveService.getProfileAchievements(profileService.getProfileAchieveId(profile.getId()));
        ShopResultEnum shopResultEnum = achieveService.buyResetBonusItems(msg, profileAchievements);
        return new BuyResetBonusItemsResult(shopResultEnum, msg.requestNum);
    }

}
