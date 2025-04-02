package wormix.pvp

import com.pragmatix.app.common.RatingType
import com.pragmatix.pvp.BattleWager
import com.pragmatix.pvp.dsl.WagerDuelBattle
import org.junit.Test
import wormix._

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 16.02.2016 9:31
  */
class EndPvpBattleAwardTest extends BasePvpTest {

  @Test
  def fireDuelBattles() {
    turnOffLobbyRestrict()

    for(
      wager <- Array(BattleWager.WAGER_15_DUEL, BattleWager.WAGER_300_DUEL);
      teamSize <- 1 to 4
    ) {
      doBattleByWagerAndTeamSize(wager, teamSize)
    }

  }

  @Test
  def successBattle(): Unit = {
    turnOffLobbyRestrict()

    new WagerDuelBattle(binarySerializer)
      .loginMain(user1Id, user2Id)
      .startBattle()
      .finishBattle()
  }

  @Test
  def wager20Test(): Unit = {
    turnOffLobbyRestrict()

    for (teamSize <- 1 to 4){
      val profile_1 = getProfile(user1Id).initTeamWithSize(teamSize)
      val profile_2 = getProfile(user2Id).initTeamWithSize(teamSize)

      val wagerDuelBattle = new WagerDuelBattle(binarySerializer)
        .setWager(BattleWager.WAGER_20_DUEL)
        .loginMain(user1Id, user2Id)
        .startBattle()

      wagerDuelBattle.finishBattle()
      wagerDuelBattle.disconnectFromPvp()
      wagerDuelBattle.disconnectFromMain()

      Thread.sleep(300)
    }

  }

  @Test
  def drawBattleInRubyLeague() {
    val teamSize = 4
    val profile_1 = getProfile(user1Id).initTeamWithSize(teamSize)
    val profile_2 = getProfile(user2Id).initTeamWithSize(teamSize)
    turnOffLobbyRestrict()

    profile_1.setBestRank(0)
    profile_1.setRankPoints(10000)

    profile_2.setBestRank(15)
    profile_2.setRankPoints(1000)

    new WagerDuelBattle(binarySerializer, mainChannelsMap)
      .loginMain(profile_1.getId, profile_2.getId)
      .startBattle()
      .drawBattle()

    profile_1.getRankPoints should be < 10000
    profile_2.getRankPoints shouldBe 1000
  }

  @Test
  def looseBattleWithDroppedUnits() {
    val profile = getProfile(testerProfileId)
    turnOffLobbyRestrict()

    doBattleByWagerAndTeamSize(BattleWager.WAGER_15_DUEL, 4, 3)

    println(ratingService.getTop(RatingType.Seasonal, BattleWager.NO_WAGER, profile))
  }

  def doBattleByWagerAndTeamSize(wager: BattleWager, teamSize: Int, droppedUnits: Int = 0): Unit = {
    val profile_1 = getProfile(user1Id).initTeamWithSize(teamSize)
    val profile_2 = getProfile(user2Id).initTeamWithSize(teamSize)
    val wagerDef = battleService.getBattleWagerDef(wager, teamSize)

    val wagerDuelBattle = new WagerDuelBattle(binarySerializer, mainChannelsMap)
      .setWager(wagerDef.wager)
      .loginMain(profile_1.getId, profile_2.getId)

    profile_1.setMoney(wagerDef.value)
    profile_2.setMoney(wagerDef.value)

    wagerDuelBattle.startBattle()

    val (winner, looser) = wagerDuelBattle.finishBattle(droppedUnits) match {case Array(w, l) => (getProfile(w.getUserId), getProfile(l.getUserId))}

    winner.getMoney shouldBe wagerDef.value + wagerDef.award
    looser.getMoney shouldBe 0 + battleService.getAwardForDroppedUnits.get(droppedUnits)

    wagerDuelBattle.getBattleParticipants.foreach(_.disconnectFromMain())

    Thread.sleep(1000)
  }

}
