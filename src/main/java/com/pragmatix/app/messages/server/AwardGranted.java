package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.messages.client.ActivatePromoKey;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.intercom.messages.IntercomAchieveRequest;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.sessions.IAppServer;

import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 14.11.13 11:50
 * @see com.pragmatix.app.services.rating.DailyRatingService#awardForTopPosition(UserProfile, int, BattleWager)
 * @see com.pragmatix.app.services.PromoService.PromoController#onActivatePromoKey(ActivatePromoKey, UserProfile)
 * @see com.pragmatix.intercom.controller.IntercomController#onIntercomAchieveRequest(IntercomAchieveRequest, IAppServer)
 */
@Command(10094)
public class AwardGranted implements SecuredResponse {

    public List<GenericAwardStructure> awards;

    public AwardTypeEnum awardType;

    /**
     * доп. параметр
     */
    public String attach = "";

    public String sessionKey;

    public AwardGranted() {
    }

    public AwardGranted(AwardTypeEnum awardType, List<GenericAwardStructure> awards, String attach, String sessionKey) {
        this.awards = awards;
        this.awardType = awardType;
        this.attach = attach;
        this.sessionKey = sessionKey;
    }

    @Override
    public String toString() {
        return "AwardGranted{" +
                "awards=" + awards +
                ", awardType=" + awardType +
                ", attach='" + attach + '\'' +
                '}';
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }
}
