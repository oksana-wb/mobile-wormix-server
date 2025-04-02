package com.pragmatix.app.init;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 28.05.2018 11:51
 */
public class LegacyLevelCreator extends LevelCreator {

    @Override
    public int getMaxAvailablePoints(int level) {
        return level * 2;
    }

}
