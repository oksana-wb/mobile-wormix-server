package com.pragmatix.app.services;

import com.pragmatix.app.common.MoneyType;
import com.pragmatix.app.common.Race;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.messages.client.BuySkin;
import com.pragmatix.app.messages.server.BuySkinResponse;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.craft.domain.Reagent;
import com.pragmatix.quest.QuestService;
import com.pragmatix.testcase.AbstractSpringTest;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.08.2016 14:23
 */
public class SkinServiceTest extends AbstractSpringTest {

    @Resource
    QuestService questService;

    @Test
    public void buyAlienSkin() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        loginMain();

        profile.setSkins(ArrayUtils.EMPTY_BYTE_ARRAY);
        profile.setRealMoney(10);
        profile.setRace(Race.ALIEN);
        profile.setRaces(Race.setRaces(Race.ALIEN));

        BuySkinResponse result = requestMain(new BuySkin(101, MoneyType.REAL_MONEY), BuySkinResponse.class);
        Assert.assertEquals(ShopResultEnum.NOT_FOR_SALE, result.result);
        Assert.assertEquals(10, profile.getRealMoney());
        println(result.cost);
    }

    @Test
    public void buySkin() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        loginMain();

        profile.setSkins(ArrayUtils.EMPTY_BYTE_ARRAY);
        craftService.getReagentsForProfile(testerProfileId).setReagentValue(Reagent.mutagen, 100);
        profile.setRealMoney(0);
        profile.setRace(Race.DEMON);
        profile.setRaces(Race.setRaces(Race.DEMON, Race.CAT));
        questService.getQuestEntity(profile).q3().buySkinForReagent = 0;

        BuySkinResponse result = requestMain(new BuySkin(31, MoneyType.REAGENTS), BuySkinResponse.class);
        Assert.assertEquals(ShopResultEnum.SUCCESS, result.result);
        Assert.assertEquals(0, profile.getReagents().getReagentValue(Reagent.mutagen));
        Assert.assertEquals(0, profile.getRealMoney());
        Assert.assertEquals(1, questService.getQuestEntity(profile).q3.buySkinForReagent);
        Assert.assertArrayEquals(new byte[]{-31}, result.skins);
        println(result.cost);

        profile.setRealMoney(1);
        result = requestMain(new BuySkin(32, MoneyType.REAL_MONEY), BuySkinResponse.class);
        Assert.assertEquals(ShopResultEnum.SUCCESS, result.result);
        Assert.assertEquals(0, profile.getReagents().getReagentValue(Reagent.mutagen));
        Assert.assertEquals(0, profile.getRealMoney());
        Assert.assertArrayEquals(new byte[]{31, -32}, result.skins);
        println(result.cost);

        profile.setRace(Race.CAT);
        craftService.getReagentsForProfile(testerProfileId).setReagentValue(Reagent.mutagen, 200);
        result = requestMain(new BuySkin(61, MoneyType.REAGENTS), BuySkinResponse.class);
        Assert.assertEquals(ShopResultEnum.SUCCESS, result.result);
        Assert.assertEquals(0, profile.getReagents().getReagentValue(Reagent.mutagen));
        Assert.assertEquals(0, profile.getRealMoney());
        Assert.assertEquals(2, questService.getQuestEntity(profile).q3.buySkinForReagent);
        Assert.assertArrayEquals(new byte[]{31, -32, -61}, result.skins);
        println(result.cost);

        println("");
        println(questService.questsProgress(profile));
    }

}