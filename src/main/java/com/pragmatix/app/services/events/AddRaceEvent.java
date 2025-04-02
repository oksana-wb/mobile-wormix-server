package com.pragmatix.app.services.events;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.common.Race;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;

/**
 * Created: 29.07.15 16:52
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class AddRaceEvent implements IProfileEvent<GenericAwardStructure> {

    private Race race;

    private boolean setRace;

    public AddRaceEvent(Race race) {
        this.race = race;
    }

    public AddRaceEvent setRace(boolean value){
        setRace = value;
        return this;
    }

    @Override
    public GenericAwardStructure runEvent(UserProfile profile) {
        if(profile != null) {
            if(!Race.hasRace(profile.getRaces(), race)) {
                profile.setRaces(Race.addRace(profile.getRaces(), race));
            }
            if(setRace) {
                profile.setRace(race);
            }
        }
        return new GenericAwardStructure(AwardKindEnum.RACE, 1, race.getByteType());
    }

}