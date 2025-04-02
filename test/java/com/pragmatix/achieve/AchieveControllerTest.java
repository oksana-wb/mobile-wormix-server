package com.pragmatix.achieve;

import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.achieve.messages.client.IncreaseAchievements;
import com.pragmatix.achieve.messages.server.IncreaseAchievementsResult;
import com.pragmatix.achieve.services.MaintainedAchievementService;
import com.pragmatix.app.messages.client.Ping;
import com.pragmatix.app.messages.server.Pong;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;

import static junit.framework.Assert.assertEquals;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.07.11 14:48
 */
public class AchieveControllerTest extends AchieveTest {

    @Resource(name = "wormixVkontakteAchievementService")
    private MaintainedAchievementService service;

//    @Test
//    public void testChooseBonusItem() throws InterruptedException {
//        // всё в нули
//        clearAchievements();
//
//        // выбираем
//        ChooseBonusItem cmd = new ChooseBonusItem();
//        cmd.itemId = 62;
//        achieveConnection.send(cmd);
//        Thread.sleep(300);
//
//        ChooseBonusItemResult result = achieveConnection.receive(ChooseBonusItemResult.class, 100);
//        // сасамба! нет очков
//        assertEquals(ShopResultEnum.NOT_ENOUGH_MONEY.getType(), result.result);
//
//        // набираем очивок для первой 1000 очков
//        SetAchievements setAchievements = new SetAchievements();
//        setAchievements.profileId = "" + testerProfileId;
//        setAchievements.achievementsIndex = getAchieveIndexes();
//        setAchievements.achievementsValues = new int[setAchievements.achievementsIndex.length];
//
//        int points = 0;
//        int i = 0;
//        for(byte index : setAchievements.achievementsIndex) {
//            points += service.getAchieveAwards(index).get(2).getPoints();
//            setAchievements.achievementsValues[i] = (short) service.getAchieveAwards(index).get(2).getProgress();
//            i++;
//            if(points > 1000) {
//                break;
//            }
//        }
//
//        achieveConnection.send(setAchievements);
//        Thread.sleep(300);
//
//        // патаемся выбрать оружие со 2-го уровня
//        cmd = new ChooseBonusItem();
//        cmd.itemId = 1027;
//        achieveConnection.send(cmd);
//        Thread.sleep(300);
//
//        result = achieveConnection.receive(ChooseBonusItemResult.class, 100);
//        // ошибка, не тот уровень
//        assertEquals(ShopResultEnum.MIN_REQUIREMENTS_ERROR.getType(), result.result);
//
//
//        // выбираем с первого уровня имея ачивок на 100 очков
//        cmd = new ChooseBonusItem();
//        cmd.itemId = 62;
//        achieveConnection.send(cmd);
//        Thread.sleep(300);
//
//        result = achieveConnection.receive(ChooseBonusItemResult.class, 100);
//        // всё должно получиться
//        assertEquals(ShopResultEnum.SUCCESS.getType(), result.result);
//
//    }
//
//    private void clearAchievements() throws InterruptedException {
//
//        SetAchievements setCmd = new SetAchievements();
//        setCmd.profileId = "" + testerProfileId;
//        setCmd.achievementsIndex = getAchieveIndexes();
//        setCmd.achievementsValues = new int[setCmd.achievementsIndex.length];
//
//        achieveConnection.send(setCmd);
//    }

    private byte[] getAchieveIndexes(String profileId) {
        WormixAchievements achievements = new WormixAchievements(profileId);
        byte[] result = new byte[achievements.getAchievements().length + achievements.getStatistics().length];
        int i = 0;
        for(WormixAchievements.AchievementName achievement : WormixAchievements.AchievementName.values()) {
            result[i] = (byte) achievement.getIndex();
            i++;
        }
        return result;
    }

//    @Test
//    public void testIncAchievementsNoAward() throws InterruptedException {
//
////        clearAchievements();
//
//
//        IncreaseAchievements incCmd = new IncreaseAchievements();
//        incCmd.sessionKey = sessionId;
//        incCmd.achievementsIndex = new int[]{0, 1, 2, 101};
//        incCmd.achievementsRise = new int[]{10, 20, 30, 1000};
//        timeSequence++;
//        incCmd.timeSequence = timeSequence;
//
//        achieveConnection.send(incCmd);
//        Thread.sleep(300);
//
//        IncreaseAchievementsResult incResult = achieveConnection.receive(IncreaseAchievementsResult.class, 100);
//
//        assertEquals(10, incResult.achievementsValues[0]);
//        assertEquals(20, incResult.achievementsValues[1]);
//        assertEquals(30, incResult.achievementsValues[2]);
//
//        assertEquals(1000, incResult.achievementsValues[3]);
//
//    }

//    @Test
//    public void testIncBigAchievementsNoAward() throws InterruptedException {
//
//        clearAchievements();
//
//        // fuzzes_spent -> 25000
//        SetAchievements setCmd = new SetAchievements();
//        setCmd.profileId = "" + testerProfileId;
//        setCmd.achievementsIndex = new byte[]{17};
//        setCmd.achievementsValues = new int[]{60000};
//        achieveConnection.send(setCmd);
//
//        //fuzzes_spent + 25000
//        IncreaseAchievements incCmd = new IncreaseAchievements();
//        incCmd.sessionKey = sessionId;
//        incCmd.achievementsIndex = new byte[]{17};
//        incCmd.achievementsRise = new short[]{10000};
//        loginTime++;
//        incCmd.timeSequence = loginTime;
//
//        achieveConnection.send(incCmd);
//        Thread.sleep(300);
//
//        IncreaseAchievementsResult incResult = achieveConnection.receive(IncreaseAchievementsResult.class, 100);
//
//        assertEquals(-1, incResult.achievementsValues[0]);
//
//    }

//    @Test
//    public void testIncAchievementsGetAward() throws InterruptedException {
//
////        clearAchievements();
//
//        IncreaseAchievements incCmd = new IncreaseAchievements();
//        incCmd.sessionKey = sessionId;
//        incCmd.achievementsIndex = new int[]{0, 1, 2, 101};
//        incCmd.achievementsRise = new int[]{10, 20, 50, 1000};
//        timeSequence++;
//        incCmd.timeSequence = timeSequence;
//
//        achieveConnection.send(incCmd);
//        Thread.sleep(300);
//
//        Object lastMessage;
//        boolean b;
//        do {
//            achieveConnection.send(new Ping());
//            Thread.sleep(1000);
//            lastMessage = achieveConnection.getMessageHandler().lastMessageExcept(Pong.class);
//            b = lastMessage != null && lastMessage instanceof IncreaseAchievementsResult;
//        } while (!b);
//
//        IncreaseAchievementsResult incResult = (IncreaseAchievementsResult) lastMessage;
//
//        assertEquals(10, incResult.achievementsValues[0]);
//        assertEquals(20, incResult.achievementsValues[1]);
//        assertEquals(50, incResult.achievementsValues[2]);
//        assertEquals(1000, incResult.achievementsValues[3]);
//
//        assertEquals(2, incResult.thresholdAchievementsIndex.get(0).intValue());
//        assertEquals(0, incResult.thresholdAchievementsOldValues.get(0).intValue());
//
//    }
//
//    @Test
//    public void testIncAchievementsGetAwardFail() throws InterruptedException {
//
////        clearAchievements();
//
//
//        IncreaseAchievements incCmd = new IncreaseAchievements();
//        incCmd.sessionKey = sessionId;
//        incCmd.achievementsIndex = new int[]{0, 1, 2, 101};
//        incCmd.achievementsRise = new int[]{10, 20, 50, 1000};
//        timeSequence++;
//        incCmd.timeSequence = timeSequence;
//
//        achieveConnection.send(incCmd);
//        Thread.sleep(300);
//
//        Object lastMessage;
//        boolean b;
//        do {
//            achieveConnection.send(new Ping());
//            Thread.sleep(1000);
//            lastMessage = achieveConnection.getMessageHandler().lastMessageExcept(Pong.class);
//            b = lastMessage != null && lastMessage instanceof IncreaseAchievementsResult;
//        } while (!b);
//
//        IncreaseAchievementsResult incResult = (IncreaseAchievementsResult) lastMessage;
//
//        assertEquals(10, incResult.achievementsValues[0]);
//        assertEquals(20, incResult.achievementsValues[1]);
//        assertEquals(49, incResult.achievementsValues[2]);
//        assertEquals(1000, incResult.achievementsValues[3]);
//
//        assertEquals(0, incResult.thresholdAchievementsIndex.size());
//
//    }

//    @Test
//    public void testGiveAward() throws InterruptedException {
//        SetAchievements cmd = new SetAchievements();
//        cmd.profileId = "" + testerProfileId;
//        cmd.achievementsIndex = new byte[]{0, 1};
//        cmd.achievementsValues = new int[]{499, 99};
//        achieveConnection.send(cmd);
//
//        loginTime++;
//        IncreaseAchievements incCmd = new IncreaseAchievements();
//        incCmd.sessionKey = sessionId;
//        incCmd.achievementsIndex = new byte[]{0, 1};
//        incCmd.achievementsRise = new short[]{4501, 1};
//        incCmd.timeSequence = loginTime;
//
//        achieveConnection.send(incCmd);
//        Thread.sleep(300);
//        IncreaseAchievementsResult incResult = achieveConnection.receive(IncreaseAchievementsResult.class, 100);
//
//        // выдача приза за 1-ое достижение
//        assertEquals(5000, incResult.achievementsValues[0]);
//        assertEquals(0, incResult.thresholdAchievementsIndex[0]);
//        assertEquals(499, incResult.thresholdAchievementsOldValues[0]);
//
//        // выдача приза за 2-ое достижение
//        assertEquals(100, incResult.achievementsValues[1]);
//        assertEquals(1, incResult.thresholdAchievementsIndex[1]);
//        assertEquals(99, incResult.thresholdAchievementsOldValues[1]);
//
//        Thread.sleep(2000);
//    }

//    @Test
//    public void wipeTest() throws InterruptedException {
//        clearAchievements();
//
//        SetAchievements cmd = new SetAchievements();
//        cmd.profileId = "" + testerProfileId;
//        cmd.achievementsIndex = new byte[]{0};
//        cmd.achievementsValues = new int[]{100};
//
//        achieveConnection.send(cmd);
//
//        achieveConnection.send(new GetAchievements("" + testerProfileId));
//
//        GetAchievementsResult lm = achieveConnection.receive(GetAchievementsResult.class, 100);
//        assertEquals(100, lm.achievementsValues[0]);
//
//        WipeAchievements wipeAchievements = new WipeAchievements();
//        wipeAchievements.profileId = "" + testerProfileId;
//
//        achieveConnection.send(wipeAchievements);
//
//        connect();
//
//        GetAchievements getAchievements = new GetAchievements();
//        getAchievements.profileId = "" + testerProfileId;
//
//        achieveConnection.send(getAchievements);
//        lm = achieveConnection.receive(GetAchievementsResult.class, 100);
//        assertEquals(0, lm.achievmensIndex.length);
//
//    }

}
