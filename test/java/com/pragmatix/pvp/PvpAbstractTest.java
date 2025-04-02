package com.pragmatix.pvp;

import com.pragmatix.app.services.BanService;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.matchmaking.BlackListService;
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf;
import com.pragmatix.testcase.AbstractSpringTest;
import io.netty.channel.Channel;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 09.09.13 15:11
 */
public class PvpAbstractTest extends AbstractSpringTest {

    protected long user1Id = 58027749L;
    protected long user2Id = 58027748L;
    protected long user3Id = 58027747L;
    protected long user4Id = 58027746L;

    @Resource
    protected LobbyConf lobbyConf;

    @Resource
    protected BlackListService blackListService;

    @Resource
    protected BanService banService;

    @Resource
    protected PvpService pvpService;

    protected List<PvpParticipant> battleParticipants;

    protected static Map<PvpParticipant, Channel> mainChannelsMap = new HashMap<>();

    public void turnOffLobbyRestrict() throws InterruptedException {
        lobbyConf.setUseLevelDiffFactor(false);
        lobbyConf.setEnemyLevelRange(30);
        lobbyConf.setHpDiffFactor(100);
        lobbyConf.setMaxHpDiffFactor(100);
        lobbyConf.setBestMatchQuality(0);
        lobbyConf.setSandboxBattlesDelimiter(0);
        lobbyConf.setRankThreshold(100);

        lobbyConf.setCheckOpponents(false);

        getProfile(user1Id).setLevel(30);
        getProfile(user2Id).setLevel(30);
        getProfile(user3Id).setLevel(30);
        getProfile(user4Id).setLevel(30);

        banService.getBanList().clear();

        blackListService.getBlackListsForUsers().clear();
    }

}
