package com.pragmatix.app.dao;

import com.pragmatix.app.domain.BossBattleExtraRewardEntity;
import com.pragmatix.app.domain.ReferralLinkEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.04.13 11:30
 */
@Component
public class BossBattleExtraRewardDao extends AbstractDao<BossBattleExtraRewardEntity> {

    public BossBattleExtraRewardDao() {
        super(BossBattleExtraRewardEntity.class);
    }

}
