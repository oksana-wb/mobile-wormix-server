package com.pragmatix.pvp.controllers;

import com.pragmatix.testcase.AbstractSpringTest;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.11.11 9:49
 */
public class WagerStartBattleControllerTest extends AbstractSpringTest {

//    @Resource
//    private WagerBattleController battleController;
//
//    public static final boolean LOG = false;
//
//    @Before
//    public void setUp() throws Exception {
//
//    }
//
//    @Test
//    public void testOnWagerBattleRequests() throws Exception {
//        int userCount = 1000;
//        List<UserProfile> list = new ArrayList<UserProfile>();
//        for(int i = 0; i < userCount; i++) {
//            list.add(createUserProfile());
//        }
//
//        final WagerBattleRequest msg = new WagerBattleRequest();
//        msg.wager = 10;
//
//        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
//
//        List<Future> futureList = new ArrayList<Future>();
//        for(final UserProfile profile : list) {
//            Future<?> future = executorService.submit(new Runnable() {
//                @Override
//                public void run() {
//                    while(true) {
//
//                        Thread.currentThread().setName("Thread#"+profile.getId());
//
//                        assertEquals(BattleState.NOT_IN_BATTLE, profile.getBattleState());
//                        battleController.onWagerBattleRequest(msg, profile);
//                        int k=0;
//                        while(profile.getBattleState() != BattleState.WAGER_PvP && k < 5) {
//                            try {
//                                Thread.sleep(1);
//                            } catch(InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            k++;
//                        }
//                        if(profile.getBattleId() >0 && profile.getBattleId() % 100000 == 0) {
//                            System.out.println(Thread.currentThread().getName() + ":" + profile.getBattleId()/1000+"K");
//                        }
//
//                        battleController.onWagerCancelBattle(new WagerCancelBattle(), profile);
//                    }
//                }
//            });
//            futureList.add(future);
//        }
//
//        for(Future fut : futureList) {
//            fut.get();
//        }
//    }
//
//    @Test
//    public void testOnWagerBattleRequest() throws Exception {
//
//        int enemyCount = 2;
//        List<UserProfile> list = new ArrayList<UserProfile>();
//        list.add(createUserProfile());
//        for(int i = 0; i < enemyCount; i++) {
//            list.add(createUserProfile());
//        }
//
//        final WagerBattleRequest msg = new WagerBattleRequest();
//        msg.wager = 10;
//
//        ExecutorService executorService = Executors.newFixedThreadPool(enemyCount * 2);
//
//        int i = 0;
//        for(int j = 0; j < (LOG ? 50 : Integer.MAX_VALUE); j++) {
//            List<Future> futureList = new ArrayList<Future>();
//            for(final UserProfile profile : list) {
//                Future<?> future = executorService.submit(new Runnable() {
//                    @Override
//                    public void run() {
//                        battleController.onWagerBattleRequest(msg, profile);
//                    }
//                });
//                futureList.add(future);
//            }
//
//            for(Future fut : futureList) {
//                fut.get();
//            }
//
//
//            i++;
//
//            if(i % 10000 == 0) {
//                System.out.println(i);
//            }
//
////            System.out.println(i);
//            if(LOG) {
//                for(UserProfile profile : list) {
//                    System.out.printf("id:%s, battleType=%s, battleId=%s\n", profile.getId(), profile.getBattleState(), profile.getBattleId());
//                }
//            }
//            int c = 0;
//            for(UserProfile profile : list) {
//                c += profile.getBattleState() == BattleState.WAGER_PvP ? 1 : 0;
//            }
//
////            assertTrue(c == 2);
////            assertTrue(k == 1);
//            if(c == enemyCount + 1) {
//                System.out.println("Fuck:" + i);
//                i = 0;
//            }
//
//            for(UserProfile profile : list) {
//                battleController.onWagerCancelBattle(new WagerCancelBattle(), profile);
//            }
//
//            if(LOG) {
//                System.out.println("==========================");
//            }
//        }
//    }
//
//    private UserProfile createUserProfile2() {
//        UserProfile profile = new UserProfile();
//        profile.setId((long) AppUtils.generateRandom(100000));
//        profile.setMoney(Integer.MAX_VALUE);
//        profile.setLevel(1);
//        return profile;
//    }
//
//    @Test
//    public void testOnCancelBattle() throws Exception {
//    }
}
