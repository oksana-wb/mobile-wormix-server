package wormix.arena.mercenaries

import java.util.function.Consumer

import com.pragmatix.app.common.{AwardKindEnum, PvpBattleResult}
import com.pragmatix.app.model.{Stuff, UserProfile}
import com.pragmatix.arena.mercenaries.{MercenariesDao, MercenariesEntity, MercenariesErrorEnum, MercenariesService}
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import wormix.BaseTest

/**
  *
  * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
  *         Created: 18.02.2016 8:43
  */
class MercenariesServiceTest extends BaseTest {

  @Autowired val service: MercenariesService = null
  @Autowired var dao: MercenariesDao = null

  @Test
  def battleSeriesTest(): Unit = {
    dao.delete(testerProfileId.toInt, false)
    val profile = getProfile(testerProfileId)

    val entity = service.mercenariesEntity(profile)
    println(entity)
    entity.open shouldBe false

    def attempts = service.attemptsRemainToday(profile)
    attempts shouldBe service.attemptsByDay

    successByTicket(profile)

    service.setTeamMembers(profile, Array[Byte](1, 2, 3))
    println(entity.team.mkString("[", ",", "]"))
    service.isSeriesInProgress(profile, entity) shouldBe true

    (1 to 3) foreach { i =>
      service.isSeriesInProgress(profile, entity) shouldBe true
      service.consumeBattleResult(profile, PvpBattleResult.WINNER, Array[Byte]())
      entity.win shouldBe i
    }
    service.isSeriesInProgress(profile, entity) shouldBe false

    val oldExp = profile.getExperience
    val reward = service.getReward(profile)
    println(reward)
    (profile.getExperience - oldExp) should be > 0
    entity.open shouldBe false
    attempts shouldBe service.attemptsByDay - 1

    service.setTeamMembers(profile, Array[Byte](3, 4, 5))
    entity.team shouldBe Array[Byte](3, 4, 5)

    dailyRegistry.setMercenariesBattleSeries(profile.getId, service.attemptsByDay)
    successByTicket(profile)
    service.attemptsRemainToday(profile) shouldBe 0

    dailyRegistry.getDailyTask.runServiceTask()
    service.attemptsRemainToday(profile) shouldBe service.attemptsByDay
    service.isSeriesInProgress(profile, entity) shouldBe true

    service.buyTicket(profile).getRight shouldBe MercenariesErrorEnum.ALREADY_OPEN
    entity.open = false
    service.buyTicket(profile).getRight shouldBe MercenariesErrorEnum.NO_ENOUGH_BATTLES
  }

  def successByTicket(profile: UserProfile): Unit = {
    val entity: MercenariesEntity = profile.getMercenariesEntity
    profile.setBattlesCount(service.ticketBattlesPrice)
    service.buyTicket(profile)
    profile.getBattlesCount shouldBe 0
    entity.open shouldBe true
    service.isSeriesInProgress(profile, entity) shouldBe false
  }

  @Test
  def awardTest(): Unit = {
    dao.delete(testerProfileId.toInt, false)
    val profile = getProfile(testerProfileId)
    profile.setExperience(0)
    profile.setLevel(25)

    //    cleanTempStuffs(profile)
    //
    //    val booster = stuffService.getStuff(3001)
    //    stuffService.addBoost(profile, booster)

    val entity = service.mercenariesEntity(profile)
    successByTicket(profile)
    service.setTeamMembers(profile, Array[Byte](1, 2, 3))
    (1 to 3) foreach { i =>
      service.consumeBattleResult(profile, PvpBattleResult.WINNER, Array[Byte]())
    }
    //    val money = 180
    import collection.JavaConverters._
    //    val awardDef = Vector[GenericAwardProducer](GenericAward.builder().addMoney(money).useBooster().build()).asJava
    //    service.completeSeriesAwardByWin = Map(3.asInstanceOf[Integer] -> awardDef).asJava
    val reward = service.getReward(profile)
    //    reward.asScala.find(_.awardKind == AwardKindEnum.MONEY).get.count shouldBe money * booster.getBoostParam
    //    reward.asScala.find(_.awardKind == AwardKindEnum.EXPERIENCE).get.count shouldBe money * booster.getBoostParam / 4
    val money = reward.asScala.find(_.awardKind == AwardKindEnum.MONEY).get.count
    println(money)
    println(profile.getExperience)
    reward.asScala.find(_.awardKind == AwardKindEnum.EXPERIENCE).get.count shouldBe money / 4
  }

  def cleanTempStuffs(profile: UserProfile): Unit = {
    var tempStuffSet = Set[Short]()
    stuffService.visitTemporalStuff(profile.getTemporalStuff, new Consumer[Stuff]() {
      override def accept(stuff: Stuff): Unit = tempStuffSet += stuff.getStuffId
    })
    tempStuffSet.foreach(stuffId => stuffService.removeStuff(profile, stuffId))
  }
}
