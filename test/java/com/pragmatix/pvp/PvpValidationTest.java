package com.pragmatix.pvp;

import com.pragmatix.app.common.CheatTypeEnum;
import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.services.CheatersCheckerService;
import com.pragmatix.pvp.dsl.WagerDuelBattle;
import com.pragmatix.pvp.messages.battle.client.PvpActionEx;
import com.pragmatix.pvp.messages.battle.client.PvpActionEx.ActionCmd;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.server.PvpEndBattle;
import com.pragmatix.pvp.messages.battle.server.PvpStartTurn;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.PvpBattleLog;
import com.pragmatix.pvp.services.battletracking.PvpBattleTrackerService;
import io.vavr.Tuple2;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.*;

import static com.pragmatix.pvp.services.battletracking.handlers.ValidCommandHandler.packClientActions;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 13.07.2016 11:07
 *         <p>
 * Тесты на процесс парсинга клиентских {@link PvpActionEx}'ов, сбора их в {@link PvpBattleLog} и валидации через {@link CheatersCheckerService}
 */
public class PvpValidationTest extends PvpAbstractTest {

    @Resource
    private PvpBattleTrackerService pvpBattleTrackerService;

    @Value("#{unlimitedUseWeapons}")
    private Set<Integer> unlimitedUseWeapons = new HashSet<>();
    /**
     * Оружия, для которых один выстрел задается двумя или более одинаковыми экшнами
     */
    @Value("#{multiClickWeapons}")
    private Map<Integer, Integer> multiClickWeapons = new HashMap<>();

    private int goodWeaponId;
    private int badWeaponId;
    private WagerDuelBattle battle;
    private BattleBuffer battleBuffer;
    private long frame;

    @Before
    public void setUp() throws Exception {
        turnOffLobbyRestrict();
        goodWeaponId = unlimitedUseWeapons.iterator().next();
        badWeaponId = goodWeaponId + 1;
        battle = new WagerDuelBattle(binarySerializer).setWager(BattleWager.WAGER_15_DUEL);
        battle.startBattle(user1Id, user2Id);
        battleBuffer = pvpBattleTrackerService.getBattle(battle.getSender().getBattleId());
        battleParticipants = Arrays.asList(battle.getBattleParticipants());
        frame = 1;
    }

    @Test
    public void testParsing() throws Exception {
        battle.getSender().sendActionEx(packClientActions(frame++,
                action(ActionCmd.selectWeapon, badWeaponId, ActionCmd.point.getCode()),
                action(ActionCmd.selectWeapon, goodWeaponId, ActionCmd.release.getCode())
        ));
        PvpActionEx cmd = battle.getReceiver().reciveFromPvp(PvpActionEx.class, 100);
        Assert.assertEquals(2, cmd.ids[1]); // там лежит actionsCount за первый кадр

        battle.getSender().sendActionEx(join(
            packClientActions(frame++,
                action(ActionCmd.charge, 1)
            ),
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, badWeaponId, ActionCmd.point.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20)
        )));
        cmd = battle.getReceiver().reciveFromPvp(PvpActionEx.class, 100);
        Assert.assertEquals(1, cmd.ids[1]); // там лежит actionsCount за первый кадр
    }

    @Test
    public void testFailTooMuchShots() throws Exception {
        battle.getSender().sendActionEx(join(
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, badWeaponId, ActionCmd.point.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20)
            ),
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, goodWeaponId, ActionCmd.release.getCode())
        )));
        // команда корректная => пропускаем
        PvpActionEx cmd = battle.getReceiver().reciveFromPvp(PvpActionEx.class, 100);
        Assert.assertEquals(1, cmd.ids[1]); // там лежит actionsCount за первый кадр
        battle.getSender().sendActionEx(join(
            packClientActions(frame++,
                action(ActionCmd.charge, 1),
                action(ActionCmd.release)
            ),
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, badWeaponId, ActionCmd.point.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20) // CHEAT! 7'th shot
        )));
        // команда читерская => не пропускаем
        cmd = battle.getReceiver().reciveFromPvpNullable(PvpActionEx.class, 100);
        Assert.assertNull("receiver not received PvpActionEx", cmd);

        Assert.assertTrue("battleLog.isPresent()", battleBuffer.getBattleLog().isPresent());
        PvpBattleLog.Turn turn = battleBuffer.getBattleLog().get().getCurrentTurn();
        Assert.assertFalse("!turn.valid", turn.valid);
        Assert.assertEquals(CheatTypeEnum.PVP_WEAPON_USAGE_HIGH, turn.getReason());

        // соперник победил
        PvpEndBattle endBattle = battle.getReceiver().reciveFromPvp(PvpEndBattle.class, 1000);
        Assert.assertEquals(PvpBattleResult.WINNER, endBattle.battleResult);
        battle.disconnectFromPvp();
        battle.disconnectFromMain();
    }

    @Test
    public void testNoFailRope() throws Exception {
        battle.getSender().sendActionEx(join(
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, badWeaponId, ActionCmd.point.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20)
            ),
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, goodWeaponId, ActionCmd.charge.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.charge, 1),
                action(ActionCmd.charge, 1),
                action(ActionCmd.charge, 1),
                action(ActionCmd.charge, 1),
                action(ActionCmd.charge, 1),
                action(ActionCmd.charge, 1),
                action(ActionCmd.charge, 1),
                action(ActionCmd.charge, 1) // веревкой можно пользоваться сколько угодно
        )));
        Assert.assertTrue("battleLog.isPresent()", battleBuffer.getBattleLog().isPresent());
        PvpBattleLog.Turn turn = battleBuffer.getBattleLog().get().getCurrentTurn();
        Assert.assertTrue("turn.valid", turn.valid);
        Assert.assertEquals(CheatTypeEnum.OK, turn.getReason());
    }

    @Test
    public void testNotCountIncompleteShots() throws Exception {
        battle.getSender().sendActionEx(join(
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, badWeaponId, ActionCmd.release.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.charge, 1),
                action(ActionCmd.charge, 1), // начал стрелять... отменил
                action(ActionCmd.charge, 1), // начал стрелять... отменил
                action(ActionCmd.charge, 1), // начал стрелять... отменил
                action(ActionCmd.charge, 1), // начал стрелять... отменил
                action(ActionCmd.release),   // 1й выстрел
                action(ActionCmd.charge, 1), // начал стрелять... отменил
                action(ActionCmd.charge, 1), // начал стрелять... отменил
                action(ActionCmd.charge, 1), // начал стрелять... отменил
                action(ActionCmd.release)    // 2й выстрел
        )));
        Assert.assertTrue("battleLog.isPresent()", battleBuffer.getBattleLog().isPresent());
        PvpBattleLog.Turn turn = battleBuffer.getBattleLog().get().getCurrentTurn();
        // должно засчитаться только два выстрела
        Assert.assertEquals(2, turn.shotsByWeapon.get(badWeaponId).get());
        // не должен засчитаться чит
        Assert.assertTrue("turn.valid", turn.valid);
        Assert.assertEquals(CheatTypeEnum.OK, turn.getReason());
    }

    @Test
    public void testFailTooMuchShotsPerTurnOnly() throws Exception {
        battle.getSender().sendActionEx(join(
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, badWeaponId, ActionCmd.point.getCode()),
                action(ActionCmd.selectWeapon, goodWeaponId,ActionCmd.release.getCode()),
                action(ActionCmd.selectWeapon, badWeaponId, ActionCmd.point.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20)
            ),
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, goodWeaponId, ActionCmd.release.getCode())
        )));
        // команда корректная => пропускаем
        PvpActionEx cmd = battle.getReceiver().reciveFromPvp(PvpActionEx.class, 100);
        Assert.assertEquals(3, cmd.ids[1]); // там лежит actionsCount за первый кадр

        // transfer turn: sender -> receiver

        System.out.println("--- sender: PvpActionEx[endTurn]: ----");
        battle.getSender().sendActionEx(
            packClientActions(frame++,
                action(ActionCmd.endTurn)
        ));
        battle.getReceiver().reciveFromPvp(PvpActionEx.class, 100);
        System.out.println("--- sender: send end turn: ----");
        battle.getSender().sendEndTurn();
        Thread.sleep(1000);
        System.out.println("--- receiver: confirm turn transfer: ----");
        confirmTurnTransfer(battle.getSender());
        Thread.sleep(1000);
        System.out.println("--- receiver: on start turn: ----");
        startTurn(battle.getReceiver());

        // transfer turn: receiver -> sender

        battle.getReceiver().sendActionEx();
        battle.getSender().reciveFromPvp(PvpActionEx.class, 100);
        System.out.println("--- receiver: PvpActionEx[endTurn]: ----");
        battle.getReceiver().sendActionEx(
            packClientActions(frame++,
                action(ActionCmd.endTurn)
        ));
        battle.getSender().reciveFromPvp(PvpActionEx.class, 100);
        System.out.println("--- receiver: send end turn: ----");
        battle.getReceiver().sendEndTurn();
        Thread.sleep(1000);
        System.out.println("--- sender: confirm turn transfer: ----");
        confirmTurnTransfer(battle.getReceiver());
        Thread.sleep(1000);
        System.out.println("--- sender: on start turn: ----");
        startTurn(battle.getSender());

        battle.getSender().sendActionEx(join(
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, badWeaponId, ActionCmd.point.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20) // NOT CHEAT: 8'th shot in battle, but only 4'th in this turn!
            ),
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, goodWeaponId, ActionCmd.release.getCode())
        )));
        // команда корректная => пропускаем
        cmd = battle.getReceiver().reciveFromPvp(PvpActionEx.class, 100);
        Assert.assertEquals(1, cmd.ids[1]); // там лежит actionsCount за первый кадр
        battle.getSender().sendActionEx(join(
            packClientActions(frame++,
                action(ActionCmd.charge, 1),
                action(ActionCmd.release)
            ),
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, badWeaponId, ActionCmd.point.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20),
                action(ActionCmd.point, 10, 20) // CHEAT! 7'th shot
        )));
        // команда читерская => не пропускаем
        cmd = battle.getReceiver().reciveFromPvpNullable(PvpActionEx.class, 100);
        Assert.assertNull("receiver not received PvpActionEx", cmd);

        Assert.assertTrue("battleLog.isPresent()", battleBuffer.getBattleLog().isPresent());
        PvpBattleLog.Turn turn = battleBuffer.getBattleLog().get().getCurrentTurn();
        Assert.assertFalse("!turn.valid", turn.valid);
        Assert.assertEquals(CheatTypeEnum.PVP_WEAPON_USAGE_HIGH, turn.getReason());

        // соперник победил
        PvpEndBattle endBattle = battle.getReceiver().reciveFromPvp(PvpEndBattle.class, 1000);
        Assert.assertEquals(PvpBattleResult.WINNER, endBattle.battleResult);
        battle.disconnectFromPvp();
        battle.disconnectFromMain();
        System.out.println("OK");
    }

    @Test
    public void bug_testDisconnectNPE() throws Exception {
        battle.getSender().sendActionEx();
        Thread.sleep(100);
        battle.getReceiver().disconnectFromPvp();
        battle.getReceiver().disconnectFromMain();
        Thread.sleep(5000);

        PvpEndBattle endBattle = battle.getSender().reciveFromPvp(PvpEndBattle.class, 1000);
        Assert.assertEquals(PvpBattleResult.WINNER, endBattle.battleResult);
        System.out.println("OK");
        battle.disconnectFromPvp();
        battle.disconnectFromMain();
    }

    @Test
    public void bug_testShotWithBackpack() throws Exception {
        battle.getSender().sendActionEx(join(
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, badWeaponId, ActionCmd.point.getCode()),
                action(ActionCmd.point, 10, 20) // не считается
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20), // считается, #1
                action(ActionCmd.point, 10, 20)  // считается, #2
        )));
        battle.getSender().sendActionEx(join(
            packClientActions(frame++,
                action(ActionCmd.backpackOpen),
                action(ActionCmd.point, 10, 20) // не считается
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20) // считается, #3
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20), // не считается
                action(ActionCmd.backpackClose)
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20), // не считается
                action(ActionCmd.selectWeapon, goodWeaponId, ActionCmd.release.getCode()) // но оружие сменяется
            ),
            packClientActions(frame++,
                action(ActionCmd.charge, 1),
                action(ActionCmd.release), // считается, #1
                action(ActionCmd.charge, 1),
                action(ActionCmd.charge, 1),
                action(ActionCmd.release) // считается, #2
        )));

        PvpBattleLog.Turn turn = battleBuffer.getBattleLog().get().getCurrentTurn();
        Assert.assertEquals(3, turn.shotsByWeapon.get(badWeaponId).get());
        Assert.assertEquals(2, turn.shotsByWeapon.get(goodWeaponId).get());
    }

    @Test
    public void testCancelPoint() throws Exception {
        battle.getSender().sendActionEx(join(
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, badWeaponId, ActionCmd.point.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20) // считается
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20) // отменяется последующим cancelShot:
            ),
            packClientActions(frame++,
                action(ActionCmd.cancelShot)
            ),
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, goodWeaponId, ActionCmd.release.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.charge, 1),
                action(ActionCmd.release),   // считается
                action(ActionCmd.charge, 1),
                action(ActionCmd.release)   // отменяется последующим cancelShot:
            ),
            packClientActions(frame++,
                action(ActionCmd.cancelShot),
                action(ActionCmd.charge, 1),
                action(ActionCmd.release)  // считается
            )
        ));
        PvpBattleLog.Turn turn = battleBuffer.getBattleLog().get().getCurrentTurn();
        Assert.assertEquals(1, turn.shotsByWeapon.get(badWeaponId).get());
        Assert.assertEquals(2, turn.shotsByWeapon.get(goodWeaponId).get());
    }

    @Test
    public void testJumpCancelsShotsAfter() throws Exception {
        battle.getSender().sendActionEx(join(
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, badWeaponId, ActionCmd.point.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20), // считается
                action(ActionCmd.point, 10, 20), // считается
                action(ActionCmd.jump1),
                action(ActionCmd.point, 10, 20), // НЕ считается
                action(ActionCmd.jump1)
            )
        ));
        PvpBattleLog.Turn turn = battleBuffer.getBattleLog().get().getCurrentTurn();
        Assert.assertEquals(2, turn.shotsByWeapon.get(badWeaponId).get());
    }

    @Test
    public void testMultiClickWeapon() throws Exception {
        int specialWeapon = multiClickWeapons.keySet().iterator().next();
        battle.getSender().sendActionEx(join(
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, specialWeapon, ActionCmd.point.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.point, 10, 20), // ещё не выстрел
                action(ActionCmd.point, 10, 20), // 1й выстрел
                action(ActionCmd.point, 10, 20), // ещё не выстрел
                action(ActionCmd.point, 10, 20), // 2й выстрел
                action(ActionCmd.point, 10, 20)  // ещё не выстрел
            ),
            packClientActions(frame++,
                action(ActionCmd.selectWeapon, goodWeaponId, ActionCmd.release.getCode())
            ),
            packClientActions(frame++,
                action(ActionCmd.release)
            )
        ));
        PvpBattleLog.Turn turn = battleBuffer.getBattleLog().get().getCurrentTurn();
        Assert.assertEquals(2, turn.shotsByWeapon.get(specialWeapon).get());
        Assert.assertEquals(1, turn.shotsByWeapon.get(goodWeaponId).get());
    }

    private void startTurn(PvpParticipant nextTurn) {
        battleParticipants.stream().filter(user -> !user.isEndBattle()).forEach(user -> {
            PvpStartTurn startTurn = user.reciveFromPvp(PvpStartTurn.class, 300);
            Assert.assertEquals(startTurn.turningPlayerNum, nextTurn.getPlayerNum());
            user.onStartTurn(startTurn);
        });
    }

    private void confirmTurnTransfer(PvpParticipant inTurn) {
        battleParticipants.stream().filter(user -> !user.isEndBattle() && !user.equals(inTurn)).forEach(user -> {
            PvpEndTurn pvpEndTurn = user.reciveFromPvp(PvpEndTurn.class, 300);
            user.confirmTurnTransfer(pvpEndTurn);
        });
    }

    @After
    public void tearDown() throws Exception {
        if (battle != null) {
            if (battle.getSender().reciveFromPvpNullable(PvpEndBattle.class, 100) != null ||
                battle.getReceiver().reciveFromPvpNullable(PvpEndBattle.class, 100) != null) {
                battle.disconnectFromPvp();
                battle.disconnectFromMain();
            } else {
                battle.finishBattle();
            }
            Thread.sleep(3000);
        }
    }

    private static Tuple2<ActionCmd, long[]> action(ActionCmd actionCmd, long... params) {
        return new Tuple2<>(actionCmd, params);
    }

    private static long[] join(long[]... arrays) {
        return Arrays.stream(arrays).flatMapToLong(Arrays::stream).toArray();
    }
}

