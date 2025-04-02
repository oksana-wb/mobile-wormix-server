package com.pragmatix.pvp;

import com.pragmatix.app.common.BanType;
import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.messages.client.Login;
import com.pragmatix.app.messages.server.EnterAccount;
import com.pragmatix.app.messages.server.LoginError;
import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.app.common.LoginErrorEnum;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.pvp.filters.PvpProtocolCheckerFilter;
import com.pragmatix.pvp.messages.battle.client.*;
import com.pragmatix.pvp.messages.battle.server.PvpEndBattle;
import com.pragmatix.pvp.messages.battle.server.PvpStartTurn;
import com.pragmatix.pvp.messages.handshake.client.CancelBattle;
import com.pragmatix.pvp.messages.handshake.client.CreateBattleRequest;
import com.pragmatix.pvp.messages.handshake.client.JoinToBattle;
import com.pragmatix.pvp.messages.handshake.client.ReconnectToBattle;
import com.pragmatix.pvp.messages.handshake.server.CallToBattle;
import com.pragmatix.pvp.messages.handshake.server.StartPvpBattle;
import com.pragmatix.serialization.AppBinarySerializer;
import com.pragmatix.testcase.SocketClientConnection;
import com.pragmatix.testcase.handlers.TestcaseMessageHandler;
import com.pragmatix.testcase.handlers.TestcaseNettyMessageHandler;
import io.netty.channel.Channel;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.LoggerFactory;
import com.pragmatix.testcase.SimpleTest;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.12.12 18:46
 */
public class PvpParticipant {

    protected long userId;
    protected TestcaseMessageHandler mainMessageHandler;
    protected Channel mainChannel;
    protected TestcaseMessageHandler pvpMessageHandler;
    protected Channel pvpChannel;
    protected AppBinarySerializer binarySerializer;
    protected String host = "127.0.0.1";
    protected int portMain = 6001;
    protected int portPvp = 6002;
    private SocialServiceEnum socialNetId = SocialServiceEnum.vkontakte;
    private long battleId;
    private String sessionKey;
    private byte playerNum;

    private short turnNum = 1;
    private short commandNum = 1;
    private boolean endBattle;
    private EnterAccount enterAccount;

    public PvpParticipant(long userId, AppBinarySerializer binarySerializer) {
        this.userId = userId;
        this.binarySerializer = binarySerializer;
    }

    public PvpParticipant(long userId, SocialServiceEnum socialNetId, AppBinarySerializer binarySerializer) {
        this.userId = userId;
        this.socialNetId = socialNetId;
        this.binarySerializer = binarySerializer;
    }

    public PvpParticipant(int playerNum) {
        this.playerNum = (byte) playerNum;
    }

    public void sendCancelBattle() {
        CancelBattle cancelBattle = new CancelBattle();

        sendToPvp(cancelBattle);
    }

    public void sendCreateBattleRequest(BattleWager battleWager) {
        CreateBattleRequest createBattleRequest = сreateBattleRequest(battleWager);
        sendToPvp(createBattleRequest);
    }

    public CreateBattleRequest сreateBattleRequest(BattleWager battleWager) {
        return сreateBattleRequest(battleWager.battleType, battleWager);
    }

    public CreateBattleRequest сreateBattleRequest(PvpBattleType battleType, BattleWager battleWager) {
        CreateBattleRequest createBattleRequest = new CreateBattleRequest();
        createBattleRequest.profileId = userId;
        createBattleRequest.socialNetId = (byte) socialNetId.getType();
        createBattleRequest.profileStringId = "" + userId;
        createBattleRequest.profileName = "user_" + userId;
        createBattleRequest.authKey = SimpleTest.MASTER_AUTH_KEY();
        createBattleRequest.mainServerId = (short) 1;

        createBattleRequest.wager = battleWager;
        createBattleRequest.participants = new long[]{userId};
        createBattleRequest.teamIds = new byte[]{};
        return createBattleRequest;
    }

    public ReconnectToBattle createReconnectToBattleRequest() {
        ReconnectToBattle reconnect = new ReconnectToBattle();
        reconnect.profileId = userId;
        reconnect.socialNetId = (byte) socialNetId.getType();
        reconnect.profileStringId = "" + userId;
        reconnect.profileName = "user_" + userId;
        reconnect.authKey = SimpleTest.MASTER_AUTH_KEY();
        reconnect.mainServerId = (short) 1;

        reconnect.battleId = battleId;
        reconnect.turnNum = turnNum;
        reconnect.playerNum = playerNum;
        reconnect.lastCommandNum = commandNum;
        return reconnect;
    }

    public void sendCreatePveFriendBattleRequest(short missionIds, long friendId) {
        sendCreatePveFriendBattleRequest(new short[]{missionIds}, 0, friendId);
    }

    public void sendCreatePveFriendBattleRequest(short[] missionIds, long mapId, long friendId) {
        CreateBattleRequest createBattleRequest = new CreateBattleRequest();
        createBattleRequest.profileId = userId;
        createBattleRequest.socialNetId = (byte) socialNetId.getType();
        createBattleRequest.profileStringId = "" + userId;
        createBattleRequest.profileName = "user_" + userId;
        createBattleRequest.authKey = SimpleTest.MASTER_AUTH_KEY();
        createBattleRequest.mainServerId = (short) 1;

        createBattleRequest.wager = BattleWager.PvE_FRIEND;
        createBattleRequest.missionIds = missionIds;
        createBattleRequest.mapId = mapId;
        createBattleRequest.participants = new long[]{userId, friendId};
        createBattleRequest.teamIds = new byte[]{};

        sendToPvp(createBattleRequest);
    }

    public void sendCreatePvePartnerBattleRequest(short... missionIds) {
        CreateBattleRequest createBattleRequest = new CreateBattleRequest();
        createBattleRequest.profileId = userId;
        createBattleRequest.socialNetId = (byte) socialNetId.getType();
        createBattleRequest.profileStringId = "" + userId;
        createBattleRequest.profileName = "user_" + userId;
        createBattleRequest.authKey = SimpleTest.MASTER_AUTH_KEY();
        createBattleRequest.mainServerId = (short) 1;

        createBattleRequest.wager = BattleWager.PvE_PARTNER;
        createBattleRequest.missionIds = missionIds;
        createBattleRequest.participants = new long[]{userId};
        createBattleRequest.teamIds = new byte[]{};

        sendToPvp(createBattleRequest);
    }

    public void sendCreateTeamBattleRequest(BattleWager battleWager, long friendId) {
        CreateBattleRequest createBattleRequest = new CreateBattleRequest();
        createBattleRequest.profileId = userId;
        createBattleRequest.socialNetId = (byte) socialNetId.getType();;
        createBattleRequest.profileStringId = "" + userId;
        createBattleRequest.profileName = "user_" + userId;
        createBattleRequest.authKey = SimpleTest.MASTER_AUTH_KEY();
        createBattleRequest.mainServerId = (short) 1;

        createBattleRequest.wager = battleWager;
        createBattleRequest.participants = new long[]{userId, friendId};
        createBattleRequest.teamIds = new byte[]{};

        sendToPvp(createBattleRequest);
    }

    public void sendCreateFriendBattleRequest(long[] participants, byte[] teams) {
        CreateBattleRequest createBattleRequest = new CreateBattleRequest();
        createBattleRequest.profileId = userId;
        createBattleRequest.socialNetId = (byte) socialNetId.getType();;
        createBattleRequest.profileStringId = "" + userId;
        createBattleRequest.profileName = "user_" + userId;
        createBattleRequest.authKey = SimpleTest.MASTER_AUTH_KEY();
        createBattleRequest.mainServerId = (short) 1;

        createBattleRequest.wager = BattleWager.NO_WAGER;
        createBattleRequest.participants = participants;
        createBattleRequest.teamIds = teams;

        sendToPvp(createBattleRequest);
    }

    public void sendJoinToBattle(CallToBattle callToBattle) {
        JoinToBattle joinToBattle = new JoinToBattle();
        joinToBattle.profileId = userId;
        joinToBattle.socialNetId = (byte) socialNetId.getType();;
        joinToBattle.profileStringId = "" + userId;
        joinToBattle.profileName = "user_" + userId;
        joinToBattle.authKey = SimpleTest.MASTER_AUTH_KEY();
        joinToBattle.mainServerId = (short) 1;

        joinToBattle.battleId = callToBattle.getBattleId();

        sendToPvp(joinToBattle);
    }

    public void loginMain() throws Exception {
        Tuple2<TestcaseMessageHandler, Channel> handler_channel = connect(host, portMain);
        mainMessageHandler = handler_channel._1;
        mainChannel = handler_channel._2;

        Login message = new Login();
        message.socialNet = SocialServiceEnum.vkontakte;
        message.id = userId;
        message.ids = Collections.emptyList();
        message.authKey = SimpleTest.MASTER_AUTH_KEY();
        message.params = new String[0];
        message.version = AppParams.VERSION();

        sendToMain(message);
        enterAccount = reciveFromMain(EnterAccount.class, 15000000);
    }

    public void loginMainFailureWithReason(LoginErrorEnum expectedLoginError) throws Exception {
        Tuple2<TestcaseMessageHandler, Channel> handler_channel = connect(host, portMain);
        mainMessageHandler = handler_channel._1;
        mainChannel = handler_channel._2;

        Login message = new Login();
        message.socialNet = SocialServiceEnum.vkontakte;
        message.id = userId;
        message.ids = Collections.emptyList();
        message.authKey = SimpleTest.MASTER_AUTH_KEY();
        message.params = new String[0];

        sendToMain(message);
        LoginError loginError = reciveFromMain(LoginError.class, 1500);
        assertEquals(expectedLoginError, loginError.error);
    }

    public void setMainChannel(Channel mainChannel) {
        this.mainChannel = mainChannel;
        mainMessageHandler = (TestcaseNettyMessageHandler) mainChannel.pipeline().get("handler");
    }

    public Channel getMainChannel() {
        return mainChannel;
    }

    public void onStartBattle(StartPvpBattle startPvpBattle) {
        this.battleId = startPvpBattle.battleId;
        this.sessionKey = startPvpBattle.sessionKey;
        this.playerNum = startPvpBattle.playerNum;
    }

    public void onStartTurn(PvpStartTurn startTurn) {
        this.turnNum = startTurn.turnNum;
        this.commandNum = startTurn.commandNum;

        commandNum++;
    }

    public void sendEndTurn(PvpParticipant... droppedPlayers) {
        sendEndTurn(0, "", new byte[0], droppedPlayers);
    }

    public void sendCheatEndTurn(PvpParticipant... droppedPlayers) {
        BanType banType = BanType.BAN_FOR_USE_CHEAT_ENGINE;
        sendEndTurn(banType.type, banType.caption, new byte[0], droppedPlayers);
    }

    public void sendEndTurn(int banType, String banNote, byte[] droppedUnits, PvpParticipant... droppedPlayers) {
        PvpEndTurn pvpEndTurn = new PvpEndTurn();
        pvpEndTurn.battleId = battleId;
        pvpEndTurn.turnNum = turnNum;
        pvpEndTurn.commandNum = commandNum;
        pvpEndTurn.collectedReagents = new byte[0];
        pvpEndTurn.items = new BackpackItemStructure[0];
        pvpEndTurn.droppedUnits = droppedUnits;
        pvpEndTurn.droppedPlayers = new byte[droppedPlayers.length];
        for(int i = 0; i < droppedPlayers.length; i++) {
            pvpEndTurn.droppedPlayers[i] = droppedPlayers[i].getPlayerNum();
        }
        pvpEndTurn.banType = (short) banType;
        pvpEndTurn.banNote = banNote;

        sendToPvp(pvpEndTurn);

        commandNum++;
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendEndTurnResponse(PvpParticipant... droppedPlayers) {
        PvpEndTurnResponse pvpEndTurnResponse = new PvpEndTurnResponse();
        pvpEndTurnResponse.battleId = battleId;
        pvpEndTurnResponse.turnNum = turnNum;
        pvpEndTurnResponse.commandNum = commandNum;
        pvpEndTurnResponse.collectedReagents = new byte[0];
        pvpEndTurnResponse.items = new BackpackItemStructure[0];
        pvpEndTurnResponse.droppedPlayers = new byte[droppedPlayers.length];
        for(int i = 0; i < droppedPlayers.length; i++) {
            pvpEndTurnResponse.droppedPlayers[i] = droppedPlayers[i].getPlayerNum();
        }

        sendToPvp(pvpEndTurnResponse);

        commandNum++;
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendActionEx() {
        sendActionEx(new long[0]);
    }

    public void sendActionEx(long[] ids) {
        PvpActionEx pvpActionEx = new PvpActionEx();
        pvpActionEx.battleId = battleId;
        pvpActionEx.playerNum = playerNum;
        pvpActionEx.turnNum = turnNum;
        pvpActionEx.commandNum = commandNum;
        pvpActionEx.ids = ids;
        pvpActionEx.firstFrame = 0;
        pvpActionEx.lastFrame = PvpProtocolCheckerFilter.MAX_STEP_SIZE;

        sendToPvp(pvpActionEx);

        commandNum++;
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void confirmTurnTransfer(PvpEndTurn pvpEndTurn) {
        sendToPvp(new PvpEndTurnResponse(pvpEndTurn));
    }

    public void consumeStartTurn() {
        PvpStartTurn startTurn = reciveFromPvp(PvpStartTurn.class, 300);
        turnNum = startTurn.turnNum;
        commandNum = startTurn.commandNum;
    }

    public void surrender() {
        PvpDropPlayer pvpDropPlayer = new PvpDropPlayer();
        pvpDropPlayer.battleId = battleId;
        pvpDropPlayer.playerNum = playerNum;
        pvpDropPlayer.reason = DropReasonEnum.SURRENDER;
        sendToPvp(pvpDropPlayer);

        endBattle = true;
    }

    public void finishBattleWithResult(PvpBattleResult expected, int delay, boolean confirmed) {
        PvpEndBattle pvpEndBattle = reciveFromPvp(PvpEndBattle.class, delay);
        PvpBattleResult battleResult = pvpEndBattle.battleResult;
        assertEquals(expected, battleResult);
        assertEquals(confirmed, pvpEndBattle.confirmed);
        endBattle = true;
    }

    public void connectToPvp() throws Exception {
        Tuple2<TestcaseMessageHandler, Channel> handler_channel = connect(host, portPvp);
        pvpMessageHandler = handler_channel._1;
        pvpChannel = handler_channel._2;
    }

    public void sendToMain(Object message) {
        send(message, mainChannel);
    }

    public void sendToPvp(Object message) {
        send(message, pvpChannel);
    }

    public <T> T reciveFromPvp(Class<T> cmdClass, long delay) {
        T cmd = waitAndGetCommand(pvpMessageHandler, cmdClass, delay);
        assertNotNull(cmd);
        return cmd;
    }

    public <T> T reciveFromPvpNullable(Class<T> cmdClass, long delay) {
        return waitAndGetCommand(pvpMessageHandler, cmdClass, delay);
    }

    public <T> T reciveFromMain(Class<T> cmdClass, long delay) {
        T cmd = waitAndGetCommand(mainMessageHandler, cmdClass, delay);
        assertNotNull(cmd);
        return cmd;
    }

    public void disconnectFromMain() {
        if(mainChannel.isActive()) {
            mainChannel.disconnect();
        }
        enterAccount = null;
    }

    public void disconnectFromPvp() {
        if(pvpChannel.isActive()) {
            pvpChannel.disconnect();
        }
        endBattle = true;
    }

    private Tuple2<TestcaseMessageHandler, Channel> connect(String host, int port) throws Exception {
        SocketClientConnection socketClientConnection = new SocketClientConnection(binarySerializer).connect(host, port);
        return Tuple.of(socketClientConnection.getMessageHandler(), socketClientConnection.getChannel());
    }

    private void send(Object message, Channel channel) {
        org.slf4j.Logger log = LoggerFactory.getLogger(TestcaseNettyMessageHandler.class.getName());
        log.info("OUT >> " + message);
        channel.writeAndFlush(message);
    }

    private <T> T waitAndGetCommand(TestcaseMessageHandler messageHandler, Class<T> cmdClass, long delay) {
        long start = System.currentTimeMillis();
        while (start + delay > System.currentTimeMillis()) {
            List<Object> incomingMessages = messageHandler.getIncomingMessages();
            for(int i = incomingMessages.size() - 1; i >= 0; i--) {
                Object msg = incomingMessages.get(i);
                if(msg != null && cmdClass.isInstance(msg)) {
                    return (T) incomingMessages.remove(i);
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public long getUserId() {
        return userId;
    }

    public long getBattleId() {
        return battleId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public String getMainSessionKey() {
        return enterAccount.sessionKey;
    }

    public byte getPlayerNum() {
        return playerNum;
    }

    public boolean isEndBattle() {
        return endBattle;
    }

    public int getMoney() {
        return enterAccount.userProfileStructure.money;
    }

    public int getRating() {
        return enterAccount.userProfileStructure.rating;
    }

    public boolean isClanMember() {
        return enterAccount.userProfileStructure.clanMember.getClanId() > 0;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        PvpParticipant that = (PvpParticipant) o;

        if(userId != that.userId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (userId ^ (userId >>> 32));
    }

}
