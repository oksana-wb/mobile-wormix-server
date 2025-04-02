package com.pragmatix.pvp;

import com.pragmatix.testcase.AbstractSpringTest;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.08.11 10:35
 */
public class BattleTest extends AbstractSpringTest {

//    protected TestcaseNettyMessageHandler messageHandler;
//    protected TestcaseNettyMessageHandler messageHandlerEnemy;
//    protected long profileId;
//    protected long enemyProfileId;
//    protected boolean firstTurn;
//    protected int battleId;
//    protected Channel channel;
//    protected Channel channelEnemy;
//
//    @Resource
//    private PvpBattleTrackerService turnTrackerService;
//
//    @Before
//    public void login() throws InterruptedException {
//        messageHandler = new TestcaseNettyMessageHandler();
//        messageHandlerEnemy = new TestcaseNettyMessageHandler();
//        Random rnd = new Random();
//        profileId = rnd.nextInt(100000);
//        enemyProfileId = rnd.nextInt(100000);
//        firstTurn = rnd.nextBoolean();
//        battleId = rnd.nextInt(100);
//
////        channel = loginPvp(messageHandler, profileId, enemyProfileId, firstTurn, battleId);
////        channelEnemy = loginPvp(messageHandlerEnemy, enemyProfileId, profileId, !firstTurn, battleId);
//
//    }
//
//
//    @Test
//    public void testFailureTurnTransfer() throws InterruptedException {
//        channel = loginPvp(messageHandler, profileId, enemyProfileId, true, battleId);
//        channelEnemy = loginPvp(messageHandlerEnemy, enemyProfileId, profileId, false, battleId);
//
//        Thread.sleep(1500);
//
//        assertTrue(messageHandler.lastMessage() instanceof PvpStartBattle);
//        assertTrue(messageHandlerEnemy.lastMessage() instanceof PvpStartBattle);
//
//        int playerNum = ((PvpStartBattle) messageHandler.lastMessage()).playerNum;
//        int playerNumEnemy = ((PvpStartBattle) messageHandlerEnemy.lastMessage()).playerNum;
//
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                System.out.println("+5 sec.");
//            }
//        }, 5000, 5000);
//
//        int turnNum = 1;
//        int commandNum = 1;
//
//        // логин прошел успешно
//        BattleBuffer battleBuffer = turnTrackerService.getBattle(battleId);
//        assertTrue(battleBuffer != null);
//        assertEquals(StateMachine.BattleStateEnum.ReadyToDispatch, battleBuffer.getBattleState());
//        // отсылаем первую команду
//        send(new PvpActionEx(battleId, turnNum, commandNum++, playerNum), channel);
//        assertEquals(StateMachine.BattleStateEnum.ReadyToDispatch, battleBuffer.getBattleState());
//        assertEquals(2, messageHandlerEnemy.countExceptPong());
//        // ждем ActivityTimeout
//        Thread.sleep(StateMachine.BattleStateEnum.ReadyToDispatch.getActivityTimeoutInSeconds() * 1000 + 3000);
//        // бой продолжается, отослали запрос на след. команду ждем продолжения хода
//        battleBuffer = turnTrackerService.getBattle(battleId);
//        assertTrue(battleBuffer != null);
//        assertEquals(StateMachine.BattleStateEnum.WaitForReplayCommand, battleBuffer.getBattleState());
//        assertTrue(messageHandler.lastMessage() instanceof PvpRetryCommandRequestServer);
//        // ждем StateTimeout
//        Thread.sleep(StateMachine.BattleStateEnum.WaitForReplayCommand.getStateTimeoutInSeconds() * 1000 + 3000);
//        // пытаемся передать ход противнику
//        assertEquals(StateMachine.BattleStateEnum.WaitForTurnTransfer, battleBuffer.getBattleState());
//        assertTrue(messageHandlerEnemy.lastMessage() instanceof PvpEndTurn);
//        // ждем StateTimeout
//        Thread.sleep(StateMachine.BattleStateEnum.WaitForTurnTransfer.getStateTimeoutInSeconds() * 1000 + 3000);
//        // бой всё еще присутствует, но уже в состоянии EndBattle
//        assertEquals(StateMachine.BattleStateEnum.EndBattle, battleBuffer.getBattleState());
//        assertTrue(messageHandler.lastMessage() instanceof PvpEndBattle);
//        assertTrue(messageHandlerEnemy.lastMessage() instanceof PvpEndBattle);
//        assertArrayEquals(new byte[]{2,2}, ((PvpEndBattle) messageHandlerEnemy.lastMessage()).results);
//
//        // ждем StateTimeout
//        Thread.sleep(StateMachine.BattleStateEnum.EndBattle.getStateTimeoutInSeconds() * 1000 + 3000);
//        // бой должен удалится
//        battleBuffer = turnTrackerService.getBattle(battleId);
//        assertTrue(battleBuffer == null);
//    }
//
//
//    @Test
//    public void testFailureReconnect() throws InterruptedException {
//        channel = loginPvp(messageHandler, profileId, enemyProfileId, true, battleId);
//        channelEnemy = loginPvp(messageHandlerEnemy, enemyProfileId, profileId, false, battleId);
//
//        Thread.sleep(1500);
//
//        assertTrue(messageHandler.lastMessage() instanceof PvpStartBattle);
//        assertTrue(messageHandlerEnemy.lastMessage() instanceof PvpStartBattle);
//
//        int playerNum = ((PvpStartBattle) messageHandler.lastMessage()).playerNum;
//        int playerNumEnemy = ((PvpStartBattle) messageHandlerEnemy.lastMessage()).playerNum;
//
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                if(channel.isOpen()) {
//                    System.out.println(profileId + ": Ping");
//                    send(new Ping(), channel);
//                }
//                if(!channel.isOpen()) {
//                    cancel();
//                }
//            }
//        }, 5000, 5000);
//
//        send(new PvpActionEx(battleId, 1, 1, playerNum), channel);
//
//        Thread.sleep(1000);
//
//        channelEnemy.close();
//
//        send(new PvpActionEx(battleId, 1, 2, playerNum), channel);
//
//        Thread.sleep(13000);
//
//        channelEnemy = reconnectPvp(messageHandlerEnemy, enemyProfileId, battleId, 1, 1, playerNumEnemy);
//
//        Thread.sleep(1500);
//
//        assertTrue(messageHandlerEnemy.lastMessage() instanceof PvpLoginError);
//    }
//
//    @Test
//    public void testSuccessReconnect() throws InterruptedException {
//        channel = loginPvp(messageHandler, profileId, enemyProfileId, true, battleId);
//        channelEnemy = loginPvp(messageHandlerEnemy, enemyProfileId, profileId, false, battleId);
//
//        Thread.sleep(1500);
//
//        assertTrue(messageHandler.lastMessage() instanceof PvpStartBattle);
//        assertTrue(messageHandlerEnemy.lastMessage() instanceof PvpStartBattle);
//
//        int playerNum = ((PvpStartBattle) messageHandler.lastMessage()).playerNum;
//        int playerNumEnemy = ((PvpStartBattle) messageHandlerEnemy.lastMessage()).playerNum;
//
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                if(channel.isOpen()) {
//                    System.out.println(profileId + ": Ping");
//                    send(new Ping(), channel);
//                }
//                if(!channel.isOpen()) {
//                    cancel();
//                }
//            }
//        }, 5000, 5000);
//
//        send(new PvpActionEx(battleId, 1, 1, playerNum), channel);
//
//        Thread.sleep(1000);
//
//        channelEnemy.close();
//
//        send(new PvpActionEx(battleId, 1, 2, playerNum), channel);
//
//        Thread.sleep(2000);
//
//        channelEnemy = reconnectPvp(messageHandlerEnemy, enemyProfileId, battleId, 1, 1, playerNumEnemy);
//
//        Thread.sleep(1000);
//
//        assertEquals(4, messageHandlerEnemy.getIncomingMessages().size());
//    }
//
//    @Test
//    public void testNonExistenBattleCommsnd() throws InterruptedException {
//        channel = loginPvp(messageHandler, profileId, enemyProfileId, true, battleId);
//        channelEnemy = loginPvp(messageHandlerEnemy, enemyProfileId, profileId, false, battleId);
//
//        Thread.sleep(1500);
//
//        assertTrue(messageHandler.lastMessage() instanceof PvpStartBattle);
//        assertTrue(messageHandlerEnemy.lastMessage() instanceof PvpStartBattle);
//
//        int playerNum = ((PvpStartBattle) messageHandler.lastMessage()).playerNum;
//        int playerNumEnemy = ((PvpStartBattle) messageHandlerEnemy.lastMessage()).playerNum;
//
//
//        send(new PvpActionEx(battleId + 1, 1, 1, playerNum), channel);
//
//        Thread.sleep(1000);
//
//        assertTrue(!channel.isActive());
//    }
//
//    @Test
//    public void testPvpActionTimeout() throws InterruptedException {
//        channel = loginPvp(messageHandler, profileId, enemyProfileId, true, battleId);
//        channelEnemy = loginPvp(messageHandlerEnemy, enemyProfileId, profileId, false, battleId);
//
//        int turnNum = 1;
//        int commandNum = 0;
//
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                if(channel.isOpen()) {
//                    System.out.println(profileId + ": Ping");
//                    send(new Ping(), channel);
//                }
//                if(channelEnemy.isOpen()) {
//                    System.out.println(enemyProfileId + ": Ping");
//                    send(new Ping(), channelEnemy);
//                }
//                if(!channel.isOpen() && !channelEnemy.isOpen()) {
//                    cancel();
//                }
//            }
//        }, 5000, 5000);
//
//        Thread.sleep(2000);
//
//        int playerNum = ((PvpStartBattle) messageHandler.lastMessage()).playerNum;
//        int playerNumEnemy = ((PvpStartBattle) messageHandlerEnemy.lastMessage()).playerNum;
//
//        // логин прошел успешно
//        BattleBuffer battleBuffer = turnTrackerService.getBattle(battleId);
//        assertTrue(battleBuffer != null);
//        assertEquals(StateMachine.BattleStateEnum.ReadyToDispatch, battleBuffer.getBattleState());
//        // отсылаем первую команду
//        send(new PvpActionEx(battleId, turnNum, ++commandNum, playerNum), channel);
//        Thread.sleep(500);
//        assertEquals(StateMachine.BattleStateEnum.ReadyToDispatch, battleBuffer.getBattleState());
//        assertEquals(2, messageHandlerEnemy.countExceptPong());
//        // ждем ActivityTimeout
//        Thread.sleep(StateMachine.BattleStateEnum.ReadyToDispatch.getActivityTimeoutInSeconds() * 1000 + 3000);
//        // бой продолжается, отослали запрос на след. команду ждем продолжения хода
//        battleBuffer = turnTrackerService.getBattle(battleId);
//        assertTrue(battleBuffer != null);
//        assertEquals(StateMachine.BattleStateEnum.WaitForReplayCommand, battleBuffer.getBattleState());
//        assertEquals(2, messageHandler.countExceptPong());
//        // ждем StateTimeout
//        Thread.sleep(StateMachine.BattleStateEnum.WaitForReplayCommand.getStateTimeoutInSeconds() * 1000 + 3000);
//        // бой всё еще присутствует, но уже в состоянии EndBattle
//        battleBuffer = turnTrackerService.getBattle(battleId);
//        assertTrue(battleBuffer != null);
//        assertEquals(StateMachine.BattleStateEnum.EndBattle, battleBuffer.getBattleState());
//        // ждем StateTimeout
//        Thread.sleep(StateMachine.BattleStateEnum.EndBattle.getStateTimeoutInSeconds() * 1000 + 3000);
//        // бой должен удалится
//        battleBuffer = turnTrackerService.getBattle(battleId);
//        assertTrue(battleBuffer == null);
//
//    }
//
//    @Test
//    public void testLoginTimeout() throws InterruptedException {
//        channel = loginPvp(messageHandler, profileId, enemyProfileId, firstTurn, battleId);
//
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                if(channel.isOpen()) {
//                    System.out.println("Ping");
//                    send(new Ping(), channel);
//                } else {
//                    cancel();
//                }
//            }
//        }, 5000, 5000);
//
//        Thread.sleep(1000);
//
//        BattleBuffer battleBuffer = turnTrackerService.getBattle(battleId);
//        assertTrue(battleBuffer != null);
//        assertEquals(StateMachine.BattleStateEnum.Init, battleBuffer.getBattleState());
//
//        Thread.sleep(StateMachine.BattleStateEnum.Init.getStateTimeoutInSeconds() / 2 * 1000);
//
//        channel.close();
//
//        Thread.sleep(StateMachine.BattleStateEnum.Init.getStateTimeoutInSeconds() / 2 * 1000 + 3000);
//
//        battleBuffer = turnTrackerService.getBattle(battleId);
//        assertTrue(battleBuffer != null);
//        assertEquals(StateMachine.BattleStateEnum.EndBattle, battleBuffer.getBattleState());
//
//        Thread.sleep(StateMachine.BattleStateEnum.EndBattle.getStateTimeoutInSeconds() * 1000 + 3000);
//
//        battleBuffer = turnTrackerService.getBattle(battleId);
//        assertTrue(battleBuffer == null);
//
//    }

}
