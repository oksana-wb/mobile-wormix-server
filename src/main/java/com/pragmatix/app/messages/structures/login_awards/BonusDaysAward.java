package com.pragmatix.app.messages.structures.login_awards;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.LoginAwardStructure;

import java.util.List;

/**
 * Награды в бонусный период
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 01.10.12 12:24
 */
public class BonusDaysAward extends LoginAwardStructure {

    public BonusDaysAward(String bonusMessage, List<GenericAwardStructure> awards) {
        this.awardType = AwardTypeEnum.BONUS_DAYS;
        this.attach = bonusMessage;
        this.awards = awards;
    }

}
