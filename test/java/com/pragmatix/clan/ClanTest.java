package com.pragmatix.clan;

import com.pragmatix.app.services.rating.RatingServiceImpl;
import com.pragmatix.clanserver.domain.Price;
import com.pragmatix.clanserver.domain.Product;
import com.pragmatix.clanserver.messages.request.LoginCreateRequest;
import com.pragmatix.clanserver.messages.request.LoginRequest;
import com.pragmatix.clanserver.messages.request.TopClansRequest;
import com.pragmatix.clanserver.messages.response.EnterAccount;
import com.pragmatix.clanserver.messages.response.TopClansResponse;
import com.pragmatix.clanserver.services.ClanServiceImpl;
import com.pragmatix.clanserver.services.PriceServiceImpl;
import com.pragmatix.pvp.PvpParticipant;
import com.pragmatix.pvp.dsl.WagerDuelBattle;
import com.pragmatix.testcase.AbstractSpringTest;
import com.pragmatix.testcase.ClientConnection;
import com.pragmatix.testcase.SocketClientConnection;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;

import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.04.13 10:04
 */
public class ClanTest extends AbstractSpringTest {

    @Value("${connection.clan.port}")
    private int clanPort;

    @Resource
    private JdbcTemplate jdbcTemplate;

//    @Resource
//    private RatingServiceImpl ratingService;

    @Resource
    private ClanInteropServiceImpl clanInteropService;

    @Resource
    private PriceServiceImpl priceService;

    @Resource
    private ClanServiceImpl clanService;

    @Before
    public void before() {
        clanInteropService.setClanCreatorMinLevel(1);
        clanInteropService.setClanCreatorMinRating(0);
        priceService.setCreateClanPrice(new Price(Price.CURRENCY_FUSY, 1, Product.CREATE_CLAN, null));
    }

    @Test
    public void createClanTest() throws Exception {
        daoService.doInTransactionWithoutResult(() -> jdbcTemplate.update("delete from clan.clan"));
        
        loginMain();
        createClan(mainConnection, testerProfileId);
        disconnectMain();
        Thread.sleep(1000);
    }

    @Test
    public void seasonTopTest() throws Exception {

        long userId1 = testerProfileId;
        long userId2 = testerProfileId - 1;

        WagerDuelBattle duelBattle = new WagerDuelBattle(binarySerializer).loginMain(userId1, userId2);

        SocketClientConnection clanConnection1 = loginClan(userId1, duelBattle);
        SocketClientConnection clanConnection2 = loginClan(userId2, duelBattle);

        duelBattle.startBattle();
        duelBattle.finishBattle();
        Thread.sleep(1000);

//        ratingService.updateSeasonProgress();

        TopClansResponse topClansResponse;

        clanConnection1.send(new TopClansRequest(true));
        topClansResponse = clanConnection1.receive(TopClansResponse.class, 300);
        System.out.println(topClansResponse);

        clanConnection2.send(new TopClansRequest(true));
        topClansResponse = clanConnection2.receive(TopClansResponse.class, 300);
        System.out.println(topClansResponse);

        clanConnection1.disconnect();
        clanConnection2.disconnect();

//        ratingService.persistToDisk();
        Thread.sleep(1000);
    }

    private SocketClientConnection loginClan(long userId1, WagerDuelBattle duelBattle) throws Exception {
        SocketClientConnection clanConnection1 = null;
        PvpParticipant battleParticipant1 = duelBattle.getBattleParticipant(userId1);
        if(battleParticipant1.isClanMember()) {
            clanConnection1 = loginClan(battleParticipant1.getMainSessionKey(), userId1);
        } else {
            //clanConnection1 = createClan(battleParticipant1.getMainSessionKey(), userId1);
        }
        return clanConnection1;
    }

    private void createClan(ClientConnection connection, long testerProfileId) throws Exception {
        LoginCreateRequest createRequest = new LoginCreateRequest();
        createRequest.name = "" + testerProfileId;
        createRequest.clanName = "Клан " + testerProfileId;
        createRequest.clanEmblem = new byte[5];

        connection.send(createRequest);
        EnterAccount enterAccount = connection.receive(EnterAccount.class, 300);

        assertTrue(enterAccount.isOk());
    }

    private SocketClientConnection loginClan(String sessionId, long testerProfileId) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.name = "" + testerProfileId;

        SocketClientConnection clanConnection = new SocketClientConnection(binarySerializer);
        clanConnection.connect(host, clanPort);
        clanConnection.send(loginRequest);
        EnterAccount enterAccount = clanConnection.receive(EnterAccount.class, 1000);

        assertTrue(enterAccount.isOk());
        return clanConnection;
    }

//        InviteToClanRequest inviteRequest = new InviteToClanRequest();
//        inviteRequest.clanId = leader.getClanId();
//        inviteRequest.socialId = leader.socialId;
//
//        InviteToClanResponse inviteResponse;
//
//        for(int i = 0; i < Rank.LEADER.inviteLimit; i++) {
//            inviteRequest.profileId = rnd.nextInt(Integer.MAX_VALUE);
//            inviteResponse = send(leaderConnect, inviteRequest, InviteToClanResponse.class);
//            if(print) sout(inviteResponse);
//            assert inviteResponse.isOk();
//            assert leader.invites.length == i + 1;
//        }
//
//        Invite[] invites = leader.invites.clone();
//
//        for(Invite invite : invites) {
//            ClanMember member = createUser();
//            member.profileId = invite.profileId;
//
//            LoginJoinRequest loginRequest = new LoginJoinRequest();
//            loginRequest.clanId = leader.getClanId();
//            loginRequest.socialId = member.socialId;
//            loginRequest.profileId = member.profileId;
//            loginRequest.name = member.name;
//            loginRequest.hostSocialId = leader.socialId;
//            loginRequest.hostProfileId = leader.profileId;
//
//            MainConnect clansmanConnect = createServerConnect();
//            CommonResponse<LoginBase> loginResponse = send(clansmanConnect, loginRequest, CommonResponse.class);
//            clansmanConnect.close();
//
//            if(print) sout(loginResponse);
//            assert loginResponse.isOk();
//        }
//        leaderConnect.close();
//        return clan;
}
