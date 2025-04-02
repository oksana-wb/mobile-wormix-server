package com.pragmatix.app.controllers;

import com.pragmatix.app.common.ItemType;
import com.pragmatix.app.init.LevelCreator;
import com.pragmatix.app.messages.client.DistributePoints;
import com.pragmatix.app.messages.client.ResetParameters;
import com.pragmatix.app.messages.server.DistributePointsResult;
import com.pragmatix.app.messages.server.ResetParametersResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.settings.ItemRequirements;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.Resource;

import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.EXTRA;
import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.PURCHASE;

/**
 * Контроллер обрабатывает команды
 * относящиеся к прокачке параметров игрока
 *
 * @author denis
 *         Date: 10.01.2010
 *         Time: 19:42:45
 */
@Controller
public class UserParametersController {

    private static final Logger logger = LoggerFactory.getLogger(ShopController.class);

    @Resource
    @Qualifier("userParametersPriceSettings")
    private ItemRequirements parametersPriceSettings;

    @Resource
    private StatisticService statisticService;

    @Resource
    private CheatersCheckerService cheatersCheckerService;

    @Resource
    private ProfileService profileService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private LevelCreator levelCreator;

    private int FREE_RESET_PARAMS_LEVEL = 10;

    @OnMessage
    public DistributePointsResult onDistributePoints(DistributePoints msg, UserProfile profile) {
        try {
            //проверяем не читер ли
            if(cheatersCheckerService.checkDistributePoints(profile, msg)) {
                logger.error("banned for trying to hack user parameters, msg: {}", msg);
                return new DistributePointsResult(DistributePointsResult.ERROR);
            }
            int availablePoints = levelCreator.getMaxAvailablePoints(profile.getLevel()) - profile.getArmor() - profile.getAttack();
            if(Math.abs(msg.attack) + Math.abs(msg.armor) > availablePoints) {
                logger.error("Can't distribute points: not enough points ...");
                return new DistributePointsResult(DistributePointsResult.NOT_ENOUGH_POINTS);
            } else {
                profile.setArmor(profile.getArmor() + Math.abs(msg.armor));
                profile.setAttack(profile.getAttack() + Math.abs(msg.attack));
            }
            return new DistributePointsResult(DistributePointsResult.SUCCESS);
        } catch (Exception ex) {
            logger.error("DistributePoints ERROR: ", ex);
            return new DistributePointsResult(DistributePointsResult.ERROR);
        }
    }

    @OnMessage
    public ResetParametersResult onResetParameters(ResetParameters msg, UserProfile profile) {
        if(profile.getLevel() >= FREE_RESET_PARAMS_LEVEL && !profileService.isVipActive(profile)) {
            if(msg.moneyType == ResetParameters.REAL_MONEY) {
                int needRealMoney = parametersPriceSettings.needRealMoney();
                if(profile.getRealMoney() < needRealMoney) {
                    return new ResetParametersResult(ResetParametersResult.NOT_ENOUGH_MONEY);
                } else {
                    profile.setRealMoney(profile.getRealMoney() - needRealMoney);
                    statisticService.resetParametersStatistic(profile.getId(), msg.moneyType, needRealMoney, profile.getLevel());
                    profileEventsService.fireProfileEventAsync(PURCHASE, profile,
                            Param.eventType, ItemType.PARAMETERS,
                            Param.realMoney, -needRealMoney
                    );
                }
            } else {
                int needMoney = parametersPriceSettings.needMoney();
                if(profile.getMoney() < needMoney) {
                    return new ResetParametersResult(ResetParametersResult.NOT_ENOUGH_MONEY);
                } else {
                    profile.setMoney(profile.getMoney() - needMoney);
                    statisticService.resetParametersStatistic(profile.getId(), msg.moneyType, needMoney, profile.getLevel());
                    profileEventsService.fireProfileEventAsync(PURCHASE, profile,
                            Param.eventType, ItemType.PARAMETERS,
                            Param.realMoney, -needMoney
                    );
                }
            }
        }
        profile.setArmor(0);
        profile.setAttack(0);

        if(profileService.isVipActive(profile)){
            profileEventsService.fireProfileEventAsync(EXTRA, profile,
                    Param.eventType, ItemType.PARAMETERS,
                    Param.vipExpireDate, profileService.getVipExpireDate(profile)
            );
        }
        return new ResetParametersResult(ResetParametersResult.SUCCESS);
    }

}
