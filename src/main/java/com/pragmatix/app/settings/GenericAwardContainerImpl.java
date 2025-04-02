package com.pragmatix.app.settings;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.06.2016 16:17
 */
public class GenericAwardContainerImpl implements GenericAwardContainer {

    public List<GenericAward> awards;

    public void setAwards(List<GenericAward> awards) {
        this.awards = awards;
    }

    @Override
    public Collection<GenericAward> getGenericAwards() {
        return awards;
    }

}
