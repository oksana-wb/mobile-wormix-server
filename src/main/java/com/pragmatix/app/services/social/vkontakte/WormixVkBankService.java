package com.pragmatix.app.services.social.vkontakte;

import com.pragmatix.app.services.BankServiceI;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.gameapp.social.SocialServiceId;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 31.10.12 12:30
 */
public class WormixVkBankService implements BankServiceI {

    private Map<Integer, Integer> rubyPriceInVotes = new HashMap<Integer, Integer>();

    @Override
    public SocialServiceId getSocialId() {
        return SocialServiceEnum.vkontakte;
    }

    @Override
    public int getRealMoneByVoites(int voites) {
        if(!rubyPriceInVotes.containsKey(voites)){
            throw new IllegalArgumentException("WormixVkBankService: не зарегистрированное количество голосов ["+voites+"]");
        }
        return rubyPriceInVotes.get(voites);
    }

    @Override
    public int getMoneByVoites(int voites) {
        return getRealMoneByVoites(voites) * 100;
    }

    public void setRubyPriceInVotes(Map<Integer, Integer> rubyPriceInVotes) {
        this.rubyPriceInVotes = rubyPriceInVotes;
    }

}
