package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.WeaponService;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 02.10.12 16:51
 */
public class AddWeaponShotEvent implements IProfileEvent<GenericAwardStructure> {

    private int weaponId;

    private int shotsCount;

    private int maxShotCount;

    private WeaponService weaponService;

    public AddWeaponShotEvent(int weaponId, int shotsCount, int maxShotCount, WeaponService weaponService) {
        this.weaponId = weaponId;
        this.shotsCount = shotsCount;
        this.maxShotCount = maxShotCount;
        this.weaponService = weaponService;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        int shotsCount = this.shotsCount;
        if(profile != null) {
            shotsCount = weaponService.addOrUpdateWeaponReturnCount(profile, weaponId, shotsCount, maxShotCount);
        }
        return new GenericAwardStructure(AwardKindEnum.WEAPON_SHOT, shotsCount, weaponId);
    }

}
