package com.pragmatix.app.controllers;

import com.pragmatix.app.messages.client.GetRating;
import com.pragmatix.app.messages.server.GetRatingResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.rating.RatingService;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;

import javax.annotation.Resource;

/**
 * Контроллер обрабатывает команды от клиента связаные с рейтингом
 * <p/>
 * User: denis
 * Date: 21.04.2010
 * Time: 1:22:36
 */
@Controller
public class RatingController {

    @Resource
    private RatingService ratingService;

    @OnMessage
    public GetRatingResult onGetRating(GetRating msg, UserProfile profile) {
        return ratingService.getTop(msg.ratingType, msg.battleWager, profile);
    }

}
