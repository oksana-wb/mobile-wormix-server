package com.pragmatix.app.messages.structures.login_awards;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.serialization.annotations.Ignore;

import java.util.Date;
import java.util.List;

/**
 * Ежедневные награды
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 01.10.12 12:00
 */
public class DailyBonusAward extends LoginAwardStructure {

    @Ignore
    public boolean firstLogin;

    @Ignore
    public Date lastLogin;

    public DailyBonusAward(LoginAwardStructure loginAwardStructure) {
        super(loginAwardStructure.awardType, loginAwardStructure.awards, loginAwardStructure.attach);
    }

    public DailyBonusAward(int loginSequence, List<GenericAwardStructure> awards) {
        this.awardType = AwardTypeEnum.DAILY_BONUS;
        this.awards = awards;
        this.attach = String.valueOf(loginSequence);
    }

    public int getLoginSequence(){
        return Integer.parseInt(this.attach);
    }

    public AwardKindEnum getDailyBonusType(){
        return this.awards.get(0).awardKind;
    }

    public int getDailyBonusCount(){
        return this.awards.get(0).count;
    }
}
