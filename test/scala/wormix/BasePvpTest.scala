package wormix

import com.pragmatix.app.services.{BanService, BattleService}
import com.pragmatix.pvp.PvpParticipant
import com.pragmatix.pvp.services.matchmaking.BlackListService
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf
import io.netty.channel.Channel
import org.springframework.beans.factory.annotation.Autowired

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 16.02.2016 9:33
  */
class BasePvpTest extends BaseTest {

  protected val user1Id = 58027749L
  protected val user2Id = 58027748L
  protected val user3Id = 58027747L
  protected val user4Id = 58027746L

  @Autowired protected val lobbyConf: LobbyConf = null
  @Autowired protected val blackListService: BlackListService = null
  @Autowired protected val banService: BanService = null
  @Autowired protected val battleService: BattleService = null

  protected var battleParticipants: java.util.List[PvpParticipant] = null
  protected val mainChannelsMap = new java.util.HashMap[PvpParticipant, Channel]

  def turnOffLobbyRestrict() {
    lobbyConf.setUseLevelDiffFactor(false)
    lobbyConf.setEnemyLevelRange(30)
    lobbyConf.setHpDiffFactor(100)
    lobbyConf.setMaxHpDiffFactor(100)
    lobbyConf.setBestMatchQuality(0)
    lobbyConf.setSandboxBattlesDelimiter(0)
    lobbyConf.setCheckLastOpponent(false)
    lobbyConf.setMercenariesBattleLobbyWinPercent(101)
    getProfile(user1Id).setLevel(25)
    getProfile(user2Id).setLevel(25)
    getProfileOpt(user3Id).foreach(_.setLevel(25))
    getProfileOpt(user4Id).foreach(_.setLevel(25))
    banService.getBanList.clear()
    blackListService.getBlackListsForUsers.clear()
  }

}
