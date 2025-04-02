package com.pragmatix.app.services;

import com.pragmatix.app.dao.BossBattleExtraRewardDao;
import com.pragmatix.app.domain.BossBattleExtraRewardEntity;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class BossBattleExtraRewardServiceTest extends AbstractSpringTest {

    @Resource
    BossBattleExtraRewardService extraRewardService;

    @Resource
    private BossBattleExtraRewardDao extraRewardDao;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Test
    public void onEndBattleGrandReward() {
        var profile = new UserProfile(1L);
        profile.setLevel(30);
        var missionId = (short) 1;

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                extraRewardDao.getAllList().forEach(it -> extraRewardDao.delete(it));

                var entity = new BossBattleExtraRewardEntity();
                entity.setMissionId(missionId);
                entity.setChance(100);
                entity.setMoney(10);

                extraRewardDao.insert(entity);

                entity = new BossBattleExtraRewardEntity();
                entity.setChance(100);
                entity.setMissionId(missionId);
                entity.setRealMoney(1);

                extraRewardDao.insert(entity);
            }
        });

        extraRewardService.loadReward();

        var map = extraRewardService.bossBattleExtraReward();
        Assert.assertEquals(1, map.size());

        var award = new ArrayList<GenericAwardStructure>();
        extraRewardService.onEndBattleGrandReward(profile, missionId, null, award);

        System.out.println(award);
        
        //Assert.assertEquals(1, award.size());
    }

}