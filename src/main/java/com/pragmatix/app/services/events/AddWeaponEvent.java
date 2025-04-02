package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.WeaponService;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 02.10.12 16:51
 */
public class AddWeaponEvent implements IProfileEvent<GenericAwardStructure> {

    private int weaponId;

    private WeaponService weaponService;

    public AddWeaponEvent(int weaponId, WeaponService weaponService) {
        this.weaponId = weaponId;
        this.weaponService = weaponService;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        int count = weaponService.getWeapon(weaponId).getInfiniteCount();
        if(profile != null) {
            count = weaponService.addOrUpdateWeaponReturnCount(profile, weaponId, count);
        }
        return new GenericAwardStructure(AwardKindEnum.WEAPON, count, weaponId);
    }

}
