package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.craft.services.CraftService;
import org.apache.commons.lang.math.RandomUtils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 02.10.12 16:51
 */
public class AddReagentEvent implements IProfileEvent<GenericAwardStructure> {

    private int[][] reagentsMass;

    private byte reagentId = -1;
    private int reagentCount;

    private CraftService craftService;

    public AddReagentEvent(int[][] reagentsMass, int reagentCount, CraftService craftService) {
        this.reagentsMass = reagentsMass;
        this.reagentCount = reagentCount;
        this.craftService = craftService;
    }

    public AddReagentEvent(byte reagentId, int reagentCount, CraftService craftService) {
        this.reagentId = reagentId;
        this.reagentCount = reagentCount;
        this.craftService = craftService;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        byte reagentId;
        if(this.reagentId >= 0) {
            // реагент указан явно
            reagentId = this.reagentId;
        } else {
            reagentId = (byte) CraftService.rollDice(reagentsMass);
        }

        if(profile != null && reagentCount > 0) craftService.addReagent(reagentId, reagentCount, profile.getId());
        return new GenericAwardStructure(AwardKindEnum.REAGENT, reagentCount, reagentId);
    }

}
