package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.serialization.annotations.Command;

import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

/**
 * @see com.pragmatix.app.controllers.ProfileController#onGetDailyBonus(com.pragmatix.app.messages.client.GetDailyBonus, com.pragmatix.app.model.UserProfile)
 */
@Command(10107)
public class GetDailyBonusResult {

    public SimpleResultEnum result;

    public List<GenericAwardStructure> awards;

    public GetDailyBonusResult() {
    }

    public GetDailyBonusResult(SimpleResultEnum result, List<GenericAwardStructure> awards) {
        this.result = result;
        this.awards = awards;
    }

    @Override
    public String toString() {
        return "GetDailyBonusResult{" +
                "result=" + result +
                (isNotEmpty(awards) ? ", awards="+awards : "") +
                '}';
    }

}
