package com.pragmatix.app.model;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 10.05.2018 12:33
 */
public class SteamProfileInitParams extends ProfileInitParams {

    @Override
    public int defaultArmorForLevel(int level) {
        return 30;
    }

    @Override
    public int defaultAttackForLevel(int level) {
        return 30;
    }

}
