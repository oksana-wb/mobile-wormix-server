package com.pragmatix.app.messages.structures;

import com.pragmatix.achieve.common.GrantAwardResultEnum;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.11.11 13:44
 */
@Structure
public class GrantAwardsResultStructure {

    public String profileId;

    public int[] awardTypes;

    public int result;

    public GrantAwardsResultStructure() {
    }

    public GrantAwardsResultStructure(String profileId, int[] awardTypes, int result) {
        this.profileId = profileId;
        this.awardTypes = awardTypes;
        this.result = result;
    }

    @Override
    public String toString() {
        return "GrantAwardsResultStructure{" +
                "profileId='" + profileId + '\'' +
                ", awardTypes=" + Arrays.toString(awardTypes) +
                ", result=" + GrantAwardResultEnum.valueOf(result) + "(" + result + ")" +
                '}';
    }
}
