package wormix.services

import javax.annotation.Resource

import com.pragmatix.app.common.PvpBattleResult
import com.pragmatix.app.services.rating.RankService
import com.pragmatix.pvp.model.{BattleBuffer, BattleParticipant, PvpUser}
import com.pragmatix.pvp.services.RatingFormula
import com.pragmatix.pvp.services.battletracking.{BattleStateTrackerI, PvpBattleActionEnum, PvpBattleStateEnum}
import com.pragmatix.pvp.{BattleWager, PvpBattleType}
import com.pragmatix.sessions.IAppServer
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 19.05.2016 15:40
  */
@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/test-rank-beans.xml"))
class RankServiceUnitTest {

  @Resource
  var rankService: RankService = _

  @Resource
  var ratingFormula: RatingFormula = _

  /*
14:07:24.961 DEBUG [nioEventLoopGroup-7-5] [NO EXECUTION] [OutboundMessageHandler.java:34] [id: 0x3fdab000, L:/178.218.212.43:61102 - R:/46.18.204.43:54457] message out >> BattleCreated{battleType=WAGER_PvP_DUEL(4), battleId=226, wager=W
AGER_15_DUEL, mapId=76, missionIds=null, questId=0, participantStructs=[PvpProfileStructure{id=1:36353064, playerNum=0, playerTeam=0, profileStringId=36353064, rankPoints=5235, bestRank=0, backpack(160)}, PvpProfileStructure{id=1:1473584
57, playerNum=1, playerTeam=1, profileStringId=147358457, rankPoints=0, bestRank=13, backpack(72)}], seed=601, reagentsForBattle=[0, 6, -1], preCalculatedPoints=[{ratingPoints=20, rankPoints=20}, {ratingPoints=0, rankPoints=-119}]}

14:07:24.961 DEBUG [nioEventLoopGroup-7-6] [NO EXECUTION] [OutboundMessageHandler.java:34] [id: 0x1697c695, L:/178.218.212.43:61102 - R:/46.18.204.43:54458] message out >> BattleCreated{battleType=WAGER_PvP_DUEL(4), battleId=226, wager=W
AGER_15_DUEL, mapId=76, missionIds=null, questId=0, participantStructs=[PvpProfileStructure{id=1:36353064, playerNum=0, playerTeam=0, profileStringId=36353064, rankPoints=5235, bestRank=0, backpack(160)}, PvpProfileStructure{id=1:1473584
57, playerNum=1, playerTeam=1, profileStringId=147358457, rankPoints=0, bestRank=13, backpack(72)}], seed=601, reagentsForBattle=[0, 6, -1], preCalculatedPoints=[{ratingPoints=-716, rankPoints=-102}, {ratingPoints=0, rankPoints=0}]}
   */


  @Test
  def utilTest(): Unit = {
    import scala.collection.JavaConversions._
    println(rankService.ranks.map(r => (r.rank, r.needPoints)).reverse.mkString("\n"))
    println(rankService.ranks.map(r => (r.rank, r.needPoints)).reverse.tail.map{case (rank, needPoints) => s"WHEN rank_points < $needPoints THEN ${rank + 1}"}.mkString("\n"))
  }

  @Test
  def getRankTest(): Unit = {
    val points = 4736
    val rank = rankService.getPlayerRankValue(points)
    println(s"$points -> $rank")
  }

  @Test
  def calcPointsTest(): Unit = {
    val teamSize = 1

    val level_1 = 17
    val rankPoints_1 = 19
    val bestRank_1 = 4

    val level_2 = 20
    val rankPoints_2 = 0
    val bestRank_2 = 20

    val battleBuffer = new BattleBuffer(1L, PvpBattleType.WAGER_PvP_DUEL, 1, battleStateTracker)
    battleBuffer.setWager(BattleWager.WAGER_15_DUEL)


    val battleParticipant_1 = new BattleParticipant(36353064L, 1, BattleParticipant.State.readyForBattle, 0, 0, mainServer)
    battleParticipant_1.setLevel(level_1)
    battleParticipant_1.profileRankPoints = rankPoints_1
    battleParticipant_1.bestRank = bestRank_1
    battleParticipant_1.teamSize = teamSize

    val battleParticipant_2 = new BattleParticipant(147358457L, 1, BattleParticipant.State.readyForBattle, 1, 1, mainServer)
    battleParticipant_2.setLevel(level_2)
    battleParticipant_2.profileRankPoints = rankPoints_2
    battleParticipant_2.bestRank = bestRank_2
    battleParticipant_2.teamSize = teamSize

    battleBuffer.addParticipant(battleParticipant_1)
    battleBuffer.addParticipant(battleParticipant_2)

    println(s"teamSize=$teamSize")
    println(s"level_1=$level_1 rankPoints_1=$rankPoints_1 bestRank_1=$bestRank_1")
    println(s"level_2=$level_2 rankPoints_2=$rankPoints_2 bestRank_2=$bestRank_2")
    println()
    println("первый")
    printResult(battleBuffer, battleParticipant_1)
    println()
    println("второй")
    printResult(battleBuffer, battleParticipant_2)
  }

  def printResult(battleBuffer: BattleBuffer, battleParticipant: BattleParticipant): Unit = {
    val win_ratingPoints = ratingFormula.getRatingPoints(battleBuffer, battleParticipant, PvpBattleResult.WINNER)
    val win_rankPoints = rankService.getRankPoints(battleBuffer, battleParticipant, PvpBattleResult.WINNER)

    val defeat_ratingPoints = ratingFormula.getRatingPoints(battleBuffer, battleParticipant, PvpBattleResult.NOT_WINNER)
    val defeat_rankPoints = rankService.getRankPoints(battleBuffer, battleParticipant, PvpBattleResult.NOT_WINNER)

    println(s"win:    $win_ratingPoints / $win_rankPoints")
    println(s"defeat: $defeat_ratingPoints / $defeat_rankPoints")
  }

  val mainServer: IAppServer = new IAppServer {
    override def getDestAddress: String = "mainServer"

    override def getId: AnyRef = new java.lang.Long(1L)
  }

  val battleStateTracker: BattleStateTrackerI = new BattleStateTrackerI {
    override def handleAction(action: PvpBattleActionEnum, battleBuffer: BattleBuffer): Unit = {}

    override def getInitState: PvpBattleStateEnum = PvpBattleStateEnum.ReadyToDispatch

    override def handleEvent(user: PvpUser, event: scala.Any, battleBuffer: BattleBuffer): Unit = {}
  }
}
