package com.pragmatix.app.init;

import com.pragmatix.app.settings.AppParams;
import com.pragmatix.gameapp.social.service.VkontakteService;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.01.2015 18:46
 */
public class InitVkontakteServiceInProduction {

    @Resource
    private VkontakteService vkontakteService;

    @Resource
    private AppParams appParams;

    public void init(){
        vkontakteService.setAuthSecret(appParams.getVkAuthSecret());
    }

}
