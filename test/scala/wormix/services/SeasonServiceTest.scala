package wormix.services

import java.time.{LocalDate, LocalDateTime}
import javax.annotation.Resource

import com.pragmatix.app.common.RatingType
import com.pragmatix.app.services.rating.{RankService, SeasonService}
import com.pragmatix.app.services.{Store, TestService}
import com.pragmatix.app.settings.GenericAward
import com.pragmatix.arena.coliseum.{ColiseumRewardItem, ColiseumService}
import com.pragmatix.intercom.messages.EndPvpBattleRequest
import com.pragmatix.pvp.BattleWager
import org.junit.Test
import wormix.BaseTest

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 13.05.2016 17:10
  */
class SeasonServiceTest extends BaseTest {

  @Resource
  var seasonService: SeasonService = _

  @Resource
  var testService: TestService = _

  @Resource
  var coliseumService: ColiseumService = _

  @Resource
  var store: Store = _

  @Test
  def storeCurrentSeasonStartDateTimeTest(): Unit = {
    daoService.doInTransactionWithoutResult(new Runnable {
      override def run(): Unit = store.save(seasonService.currentSeasonStartDateTimeStoreKey, LocalDateTime.now.toString)
    })
    println(LocalDateTime.parse(store.load(seasonService.currentSeasonStartDateTimeStoreKey, classOf[String])))
  }

  @Test
  def closeSeasonAwardTest(): Unit = {
    val profile1 = getProfile(testerProfileId)
    val profile2 = getProfile(testerProfileId - 1)
    val profile3 = getProfile(testerProfileId - 2)
    val profiles = Seq(profile1, profile2, profile3)

    seasonService.setFirstSeasonStartDateTime(LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay())
    seasonService.setCurrentSeasonStartDateTime(LocalDate.now().withDayOfMonth(1).atStartOfDay())

    profiles.foreach(p => loginMain(p.getId))
  }

  @Test
  def closeSeasonTest(): Unit = {
    var profile1 = getProfile(testerProfileId)
    var profile2 = getProfile(testerProfileId - 1)
    var profile3 = getProfile(testerProfileId - 2)
    var profiles = Seq(profile1, profile2, profile3)
    doInTransactionWithoutResult(jdbcTemplate.update("truncate wormswar.ranks"))
    profiles.foreach(p => p.setRankPoints(0))

    profile1.setBestRank(0)


    val msg = new EndPvpBattleRequest()
    msg.ratingPoints = 100
    msg.wager = BattleWager.WAGER_15_DUEL

    msg.rankPoints = 1000
    ratingService.onEndPvpBattle(profile1, msg)
    msg.rankPoints = 900
    ratingService.onEndPvpBattle(profile2, msg)
    msg.rankPoints = 800
    ratingService.onEndPvpBattle(profile3, msg)

    import collection.JavaConverters._
    val top = ratingService.getTop(RatingType.Seasonal, msg.wager, profile1).profileStructures.asScala
    println(top)
    top.map(p => (p.id, p.ratingPoints)) shouldBe Seq((profile1.getId, 1000), (profile2.getId, 900), (profile3.getId, 800))
    profile2.setBestRank(15)
    profile3.setBestRank(19)

    profiles.foreach(p => loginMain(p.getId))

    seasonService.setFirstSeasonStartDateTime(LocalDate.now().minusMonths(2).withDayOfMonth(1).atStartOfDay())
    seasonService.setCurrentSeasonStartDateTime(LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay())
    doInTransactionWithoutResult(jdbcTemplate.update("truncate wormswar.season_total"))

    seasonService.closeSeason()

    profiles.foreach(p => loginMain(p.getId))
    profile1 = getProfile(testerProfileId)
    profile2 = getProfile(testerProfileId - 1)
    profile3 = getProfile(testerProfileId - 2)
    profiles = Seq(profile1, profile2, profile3)

    profiles.foreach(p => p.getBestRank shouldBe RankService.INIT_RANK_VALUE)
    profile1.getRankPoints shouldBe seasonService.getRubyRankStartPoints
    profile2.getRankPoints shouldBe 900 / 5
    profile3.getRankPoints shouldBe 800 / 5

    println(ratingService.getTop(RatingType.Seasonal, msg.wager, profile1).profileStructures)
  }

  @Test
  def withdrawSeasonWeaponTest(): Unit = {
    val profile = getProfile(testerProfileId)
    testService.giveCurrentSeasonWeapons(profile)
    profile.setLastLoginDateTime(seasonService.getFirstSeasonStartDateTime.minusDays(1))

    import collection.JavaConverters._
    println(profile.getBackpack.asScala.filter(item => weaponCreator.getWeapon(item.getWeaponId).isSeasonal))
    val result = seasonService.withdrawSeasonWeapon(profile)
    println(profile.getBackpack.asScala.filter(item => weaponCreator.getWeapon(item.getWeaponId).isSeasonal))

    println(result)
  }

  @Test
  def simpleTest(): Unit = {
    println(seasonService.getCurrentSeasonWeaponItems)
    println(seasonService.getCurrentSeasonWeapons)
    println(seasonService.getCurrentSeasonWeaponsArr.mkString(","))

    val builder = GenericAward.builder()
    val coliseumRewardItem = new ColiseumRewardItem()
    coliseumRewardItem.weaponMin = 2
    coliseumRewardItem.weaponMax = 7
    (1 to 5).foreach(_ => coliseumService.mapItemToGenericAward(builder, coliseumRewardItem))
    println(builder.build())
  }

}
